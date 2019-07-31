package com.marcop.foodsystem.applicaiton;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.marcop.foodsystem.builders.KitchenBuilder;
import com.marcop.foodsystem.indexing.KitchenMenuItemIndexes;
import com.marcop.foodsystem.model.Kitchen;
import com.marcop.foodsystem.model.Menu;
import com.marcop.foodsystem.model.Order;
import com.marcop.foodsystem.model.OrderProcessingStrategy;
import com.marcop.foodsystem.model.ProcessStats;
import com.marcop.foodsystem.store.OrderInMemoryStore;
import com.marcop.foodsystem.store.OrderStore;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.hadoop.fs.Path;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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

    private static final Options OPTIONS = new Options()
            .addOption(
                    "kn", OPTION_KITCHEN_NAME, true, "Name of the kitchen to enable for processing orders.")
            .addOption(
                    "kmc", OPTION_KITCHEN_MAX_CONCURRENT_ITEMS, true,
                    "Maximum number of items that the kitchen can process in parallel.")
            .addOption("ip", OPTION_ORDER_INPUT_PATH, true, "Path to file containing orders to be processed.")
            .addOption("op", OPTION_OUTPUT_PATH, true, "Path for new output directory containing all outputs.");

    public static void main( String[] args ) throws ParseException
    {
        CommandLine cmdLine = new GnuParser().parse(OPTIONS, args);
        Preconditions.checkArgument(
                cmdLine.hasOption(OPTION_KITCHEN_NAME) &&
                        cmdLine.hasOption(OPTION_KITCHEN_NAME) &&
                        cmdLine.hasOption(OPTION_ORDER_INPUT_PATH) &&
                        cmdLine.hasOption(OPTION_OUTPUT_PATH), "Missing required argument. See help.");
        String kitchenName = cmdLine.getOptionValue(OPTION_KITCHEN_NAME);
        Path inputPath = new Path(cmdLine.getOptionValue(OPTION_ORDER_INPUT_PATH));
        Path outputPath = new Path(cmdLine.getOptionValue(OPTION_OUTPUT_PATH));
        int maxConcurrentItems = cmdLine.hasOption(OPTION_KITCHEN_MAX_CONCURRENT_ITEMS)
                ? Integer.parseInt(cmdLine.getOptionValue(OPTION_KITCHEN_MAX_CONCURRENT_ITEMS)) : 0;

        // Use utils to extract orders from JSON
        List<Order> orders = new ArrayList<>();

        OrderStore completedOrders = runProcessing(kitchenName, maxConcurrentItems, orders, DEFAULT_STRATEGY);

        // Compute stats from completed orders.

    }

    @VisibleForTesting
    public static OrderStore runProcessing(String kitchenName, int maxConcurrentItems,
                                     List<Order> orders, OrderProcessingStrategy strategy) {
        ProcessStats processStats = new ProcessStats();
        LOGGER.info(String.format("Configuring Kitchen %s.", kitchenName));
        KitchenBuilder kitchenBuilder = new KitchenBuilder();
        kitchenBuilder.setName(kitchenName);
        kitchenBuilder.setMaxConcurrentItems(maxConcurrentItems);
        List<Menu> menus = new ArrayList<>();
        // Get kitchen's menus from resources
        for (Menu menu : menus) {
            kitchenBuilder.addMenu(menu);
        }
        Kitchen kitchen = kitchenBuilder.build();
        LOGGER.info(
                String.format(
                        "%s Kitchen has been launched with the following menus: %s,and max concurrent items = %s.",
                        kitchenName, menus.toString(), maxConcurrentItems == 0 ? "INF" : maxConcurrentItems)
        );
        LOGGER.info("Building Kitchen Indexes.");
        // Build index to lookup cook times.
        KitchenMenuItemIndexes menuItemIndexes = new KitchenMenuItemIndexes(kitchen);
        LOGGER.info("Setting up order stores.");
        OrderInMemoryStore pendingOrders = new OrderInMemoryStore();
        OrderInMemoryStore processingOrders = maxConcurrentItems > 0
                ? new OrderInMemoryStore(maxConcurrentItems)
                : new OrderInMemoryStore();
        OrderInMemoryStore completedOrders = new OrderInMemoryStore();

        LOGGER.info("Adding new orders to pending queue");
        for (Order order : orders) {
            boolean orderAdded = pendingOrders.addOrder(order);
            if (!orderAdded) {
                // This should not happen.
                throw new RuntimeException("Order could not be added to the pending queue.");
            }
        }
        LOGGER.info("Processing orders...");
        submitAndProcess(pendingOrders, processingOrders, completedOrders, strategy);
        LOGGER.info("All order processing complete.");
        return completedOrders;
    }

    private static void submitAndProcess(OrderStore pendingOrders, OrderStore processingOrders,
                                         OrderStore completedOrders, OrderProcessingStrategy strategy) {
        int totalOrders = pendingOrders.getCurrentNumOrders();
        Order orderToSubmit = pendingOrders.getAndDequeueOrder(strategy);
        // Start with first order's timestamp.
        Timestamp currentTime = orderToSubmit.getOrderedAt();
        while (completedOrders.getCurrentNumOrders() < totalOrders) {
            boolean isOrderSubmitted = true;
            while (isOrderSubmitted) {
                if (orderToSubmit.getOrderItems().size() > processingOrders.getMaxAllowedItems()) {
                    // Kitchens cannot process only part of an order at a time.
                    // As such it must be large enough to process all of the orders items at the same time.
                    throw new RuntimeException("Kitchen is too small to process this order. Item count = "
                            + orderToSubmit.getOrderItems().size());
                }
                // Try to submit order for processing.
                isOrderSubmitted = processingOrders.submitOrder(orderToSubmit, currentTime);
                if (!isOrderSubmitted) {
                    // Once a pending order cannot be moved to the process store, then start processing that store.
                    processBatch(processingOrders, completedOrders, currentTime);
                }
                for (Order completedOrder : completedOrderBatch) {
                    completedOrders.submitOrder(completedOrder);
                }
            }
        }
    }

    private static void processBatch(OrderStore processingOrders, OrderStore completedOrders, Timestamp startedAt) {
        boolean endOfBatch = false;
        while (processingOrders.getCurrentNumOrders() > 0 || !endOfBatch) {
            List<Order> completedOrderBatch = processingOrders.clearFinishedOrders(currentTime);
            elapsedTimeMinutes++;
            for (Order completedOrder : completedOrderBatch) {
                completedOrders.submitOrder(completedOrder);
            }
        }
    }
}
