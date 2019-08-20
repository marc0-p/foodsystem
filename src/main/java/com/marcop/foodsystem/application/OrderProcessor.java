package com.marcop.foodsystem.application;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.TreeMultimap;
import com.marcop.foodsystem.builders.KitchenBuilder;
import com.marcop.foodsystem.charts.ChartUtils;
import com.marcop.foodsystem.dto.KitchenMenuItemsDto;
import com.marcop.foodsystem.dto.KitchenMenusDeserializer;
import com.marcop.foodsystem.dto.OrderDeserializer;
import com.marcop.foodsystem.indexing.KitchenMenuItemIndexes;
import com.marcop.foodsystem.model.Kitchen;
import com.marcop.foodsystem.model.Menu;
import com.marcop.foodsystem.model.Order;
import com.marcop.foodsystem.model.OrderItem;
import com.marcop.foodsystem.model.OrderProcessingStrategy;
import com.marcop.foodsystem.model.OrderState;
import com.marcop.foodsystem.store.OrderInMemoryStore;
import com.marcop.foodsystem.store.OrderStore;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static com.marcop.foodsystem.charts.ChartUtils.STATS_PAGE_FILE_NAME;

/**
 * OrderProcessor - Given a list of orders, simulates preparing those orders, and computes various metrics.
 * Note: The metrics produced can be consumed by downstream visualization and analysis tools.
 * Executes the following:
 * 1. Constructs a kitchen based on passed arguments (See cmd line helper).
 * 2. Creates an index (to allow quick look up of cook times)
 * 3. Creates a "pending" order queue. This is a queue of orders which have been received, not yet prepared.
 * 4. Creates an "in process" order queue.  This represents the ordered being prepared.
 * 5. Instantiates a list for "completed" orders.
 * 6. Instantiates various statistics to be computed.
 * 7. Simulates preparing all orders submitted to the application.
 * 8. Computes statistics.
 * 9. Creates a website to visualize the statistics.
 *
 * Usage: OrderProcessor -kn kitchen_name -kmc (optional) kitchen_max_concurrent_orders
 *                       -ip order_input_path (json) -op stats_output_path
 */
public class OrderProcessor
{
    private static final String OPTION_KITCHEN_NAME = "kitchen_name";
    private static final String OPTION_KITCHEN_MAX_CONCURRENT_ITEMS = "kitchen_max_concurrent_items";
    private static final String OPTION_ORDER_INPUT_PATH= "order_input_path";
    private static final String OPTION_OUTPUT_PATH = "output_path";
    private static final OrderProcessingStrategy DEFAULT_STRATEGY = OrderProcessingStrategy.FIRST_COME_FIRST_SERVE;
    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Options OPTIONS = new Options()
            .addOption(
                    "kn", OPTION_KITCHEN_NAME, true, "Name of the kitchen to enable for processing orders.")
            .addOption(
                    "kmc", OPTION_KITCHEN_MAX_CONCURRENT_ITEMS, true,
                    "Maximum number of items that the kitchen can process in parallel.")
            .addOption("ip", OPTION_ORDER_INPUT_PATH, true, "Path to file containing orders to be processed.")
            .addOption("op", OPTION_OUTPUT_PATH, true, "Path for new output directory containing all outputs.");

    public static void main( String[] args ) throws ParseException, IOException
    {
        CommandLine cmdLine = new GnuParser().parse(OPTIONS, args);
        Preconditions.checkArgument(
                cmdLine.hasOption(OPTION_KITCHEN_NAME) &&
                        cmdLine.hasOption(OPTION_ORDER_INPUT_PATH) &&
                        cmdLine.hasOption(OPTION_OUTPUT_PATH), "Missing required argument. See help.");
        String kitchenName = cmdLine.getOptionValue(OPTION_KITCHEN_NAME);
        Path inputPath = new Path(cmdLine.getOptionValue(OPTION_ORDER_INPUT_PATH));
        Path outputPath = new Path(cmdLine.getOptionValue(OPTION_OUTPUT_PATH));
        int maxConcurrentItems = cmdLine.hasOption(OPTION_KITCHEN_MAX_CONCURRENT_ITEMS)
                ? Integer.parseInt(cmdLine.getOptionValue(OPTION_KITCHEN_MAX_CONCURRENT_ITEMS)) : 0;

        // Extract orders from JSON
        SimpleModule module =
                new SimpleModule("OrderDeserializer", new Version(1, 0, 0, null, null, null));
        module.addDeserializer(Order.class, new OrderDeserializer());
        OBJECT_MAPPER.registerModule(module);
        byte[] jsonOrderData = Files.readAllBytes(Paths.get(inputPath.toString()));
        List<Order> orders = OBJECT_MAPPER.readValue(
                jsonOrderData,
                OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, Order.class)
        );

        OrderInMemoryStore completedOrders = new OrderInMemoryStore();
        OrderInMemoryStore rejectedOrders = new OrderInMemoryStore();
        runProcessing(kitchenName, maxConcurrentItems, orders, DEFAULT_STRATEGY, completedOrders, rejectedOrders);

        // Compute stats from completed orders.
        // Get sorted Order price (cents) table
        TreeMultimap<Integer, Order> ordersByPrice = completedOrders.getOrdersByPrice();

        // Get sorted Order pending time table
        TreeMultimap<Integer, Order> ordersByPendingTime = completedOrders.getOrdersByPendingDuration();

        // Get sorted order state counts by time
        Map<Timestamp, Map<OrderState, Integer>> orderStateCountsByTime = completedOrders.getOrderStateCountsByTime();

        // Get revenue (cents) by item table
        Map<String, Integer> revenueByItem = completedOrders.getRevenueByItem();

        // Get revenue (cents) by service table
        Map<String, Integer> revenueByService = completedOrders.getRevenueByService();

        // Get total revenue (cents).
        int totalRevenue = completedOrders.getTotalRevenue();

        // Get rejected order count
        int rejectedOrderCount = rejectedOrders.getCurrentNumOrders();

        // Create Stats Page
        LOGGER.info("Creating Stats Page.");
        ChartUtils.createStatsPage(
                kitchenName,
                maxConcurrentItems,
                ordersByPrice,
                ordersByPendingTime,
                orderStateCountsByTime,
                revenueByItem,
                revenueByService,
                totalRevenue,
                rejectedOrderCount,
                outputPath);
        LOGGER.info(String.format("Stats page location: %s.", new Path(outputPath, STATS_PAGE_FILE_NAME).toString()));
        LOGGER.info("Application is complete.");
    }

    @VisibleForTesting
    public static void runProcessing(String kitchenName, int maxConcurrentItems,
                                     List<Order> orders, OrderProcessingStrategy strategy,
                                     OrderStore completedOrders, OrderStore rejectedOrders) throws IOException {
        LOGGER.info(String.format("Configuring Kitchen %s.", kitchenName));

        // Get kitchen's menus from JSON resources
        SimpleModule module =
                new SimpleModule("KitchenDeserializer", new Version(1, 0, 0, null, null, null));
        module.addDeserializer(KitchenMenuItemsDto.class, new KitchenMenusDeserializer());
        OBJECT_MAPPER.registerModule(module);
        byte[] jsonKitchenConfig = Files.readAllBytes(Paths.get("resources/kitchens.json"));
        KitchenMenuItemsDto kitchenMenuItemsDto =
                OBJECT_MAPPER.readValue(jsonKitchenConfig, KitchenMenuItemsDto.class);
        Map<String, Set<Menu>> menusByKitchenName = kitchenMenuItemsDto.getMenusByKitchenName();

        Preconditions.checkArgument(menusByKitchenName != null && menusByKitchenName.containsKey(kitchenName),
                "The kitchen configuration specified, " + kitchenName + ", cannot be found");

        Set<Menu> menus = kitchenMenuItemsDto.getMenusByKitchenName().get(kitchenName);
        Preconditions.checkArgument(!menus.isEmpty(), "The kitchen, " + kitchenName + ", has no menus configured.");

        KitchenBuilder kitchenBuilder = new KitchenBuilder();
        kitchenBuilder.setName(kitchenName);
        kitchenBuilder.setMaxConcurrentItems(maxConcurrentItems);
        // Get kitchen's menus from resources
        List<String> menuNames = new ArrayList<>();
        for (Menu menu : menus) {
            kitchenBuilder.addMenu(menu);
            menuNames.add(menu.getName());
        }
        Kitchen kitchen = kitchenBuilder.build();
        LOGGER.info(
                String.format(
                        "Kitchen %s has been launched with the following menus: %s. And max concurrent items = %s.",
                        kitchenName, Joiner.on(',').join(menuNames),
                        maxConcurrentItems == 0 ? "INF" : maxConcurrentItems)
        );
        LOGGER.info("Building Kitchen Indexes.");
        // Build index to lookup cook times.
        KitchenMenuItemIndexes menuItemIndexes = new KitchenMenuItemIndexes(kitchen);
        LOGGER.info("Setting up order stores.");
        OrderInMemoryStore pendingOrders = new OrderInMemoryStore();
        OrderInMemoryStore processingOrders = maxConcurrentItems > 0
                ? new OrderInMemoryStore(maxConcurrentItems)
                : new OrderInMemoryStore();

        LOGGER.info("Adding new orders to pending queue");
        for (Order order : orders) {
            if (order.getOrderedAt() == null) {
                LOGGER.warning("Order rejected, since it is missing timestamp.");
                order.updateState(OrderState.REJECTED);
                rejectedOrders.addOrder(order);
                continue;
            }
            if (order.getOrderItemsSize() == 0) {
                LOGGER.warning("Order rejected, since it has no items.");
                order.updateState(OrderState.REJECTED);
                rejectedOrders.addOrder(order);
                continue;
            }
            enrichOrderWithCookTimes(order, menuItemIndexes);
            order.updateState(OrderState.CREATED);
            boolean orderAdded = pendingOrders.addOrder(order);
            if (!orderAdded) {
                // This should not happen.
                throw new RuntimeException("Order could not be added to the pending queue.");
            }
        }
        LOGGER.info("Processing orders...");
        submitAndProcess(pendingOrders, processingOrders, completedOrders, strategy);
        LOGGER.info("All order processing complete.");
    }

    /**
     * Add item cook times, and total cook time to an order.
     *
     */
    private static void enrichOrderWithCookTimes(Order order, KitchenMenuItemIndexes kitchenMenuItemIndexes) {
        int maxCookTime = 0;
        for (OrderItem item : order.getOrderItems()) {
            int itemCookTime = kitchenMenuItemIndexes.getCookTime(item.getName());
            item.setCookTimeSeconds(itemCookTime);
            if (itemCookTime > maxCookTime) {
                maxCookTime = itemCookTime;
            }
        }
        order.setTotalCookTimeSeconds(maxCookTime);
    }

    /**
     * Submit orders from pending queue to processing queue, until the processing queue can no longer accept.
     * Then process orders until there is room to accept more. Keep repeating until all orders are completed.
     */
    private static void submitAndProcess(OrderStore pendingOrders, OrderStore processingOrders,
                                         OrderStore completedOrders, OrderProcessingStrategy strategy) {
        Order orderToSubmit = pendingOrders.getAndDequeueOrder(strategy);
        // Start with first order's timestamp.
        Timestamp currentTime = orderToSubmit.getOrderedAt();
        boolean getOrder = false;
        while (pendingOrders.getCurrentNumOrders() > 0) {
            // Submit all pending orders.
            boolean isOrderSubmitted = true;
            while (isOrderSubmitted) {
                if (getOrder) {
                    orderToSubmit = pendingOrders.getAndDequeueOrder(strategy);
                    if (orderToSubmit == null) {
                        break;
                    }
                    currentTime = orderToSubmit.getOrderedAt();
                } else {
                    // orderToSubmit in memory still needs to be submitted. Skip fetching a new order.
                    getOrder = true;
                }
                if (orderToSubmit != null
                        && orderToSubmit.getOrderItemsSize() > processingOrders.getMaxAllowedItems()) {
                    // Kitchens cannot process only part of an order at a time.
                    // As such it must be large enough to process all of the orders items at the same time.
                    throw new RuntimeException("Kitchen is too small to process this order. Item count = "
                            + orderToSubmit.getOrderItems().size());
                }
                // Try to submit order for processing.
                isOrderSubmitted = processingOrders.submitOrder(orderToSubmit, currentTime);
            }
            currentTime = processBatch(processingOrders, completedOrders, currentTime);
            getOrder = false;
        }
        // Now that all orders have been submitted, finish processing remaining orders
        while (processingOrders.getCurrentNumOrders() > 0) {
            currentTime = processBatch(processingOrders, completedOrders, currentTime);
        }
    }

    /**
     * For a given timestamp, clear all orders which would be complete by that time.
     */
    private static Timestamp processBatch(OrderStore processingOrders,
                                          OrderStore completedOrders,
                                          Timestamp startedAt) {
        Timestamp queryTime = startedAt;
        int minutesElapsed = 0;

        while (processingOrders.getCurrentNumOrders() > 0) {
            queryTime = new Timestamp(queryTime.getTime() + (minutesElapsed * 60 * 1000L));
            List<Order> completedOrderBatch = processingOrders.clearFinishedOrders(queryTime);
            if (!completedOrderBatch.isEmpty()) {
                for (Order completedOrder : completedOrderBatch) {
                    completedOrders.addOrder(completedOrder);
                }
                break;
            } else {
                // If no batch for query time, increment time.
                minutesElapsed++;
            }
        }
        return queryTime;
    }
}
