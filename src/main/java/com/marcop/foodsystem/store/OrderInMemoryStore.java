package com.marcop.foodsystem.store;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.marcop.foodsystem.model.*;

import java.sql.Timestamp;
import java.util.*;

/**
 * In-memory implementation of an OrderStore.
 */
public class OrderInMemoryStore implements OrderStore {

    private static final int DEFAULT_MAX_ALLOWED_ITEMS = Integer.MAX_VALUE;

    private final int maxAllowedItems;
    private int currentNumOrders;
    private int currentNumItems;
    private TreeMultimap<Timestamp, Order> ordersByTime;
    private Map<Timestamp, Map<OrderState, Integer>> orderStateCountsByTime;
    private Timestamp firstModifiedAt;
    private Timestamp lastModifiedAt;

    public OrderInMemoryStore(int maxAllowedItems) {
        this.maxAllowedItems = maxAllowedItems;
        currentNumOrders = 0;
        currentNumItems = 0;
        this.ordersByTime = TreeMultimap.create();
        this.orderStateCountsByTime = new TreeMap<>();
    }

    // Use default max items allowed.
    public OrderInMemoryStore() {
        this(DEFAULT_MAX_ALLOWED_ITEMS);
    }

    @Override
    public boolean submitOrder(Order order, Timestamp submitTime) {
        int numItems = order.getOrderItems().size();
        if (numItems > maxAllowedItems - currentNumItems || submitTime.before(order.getOrderedAt())) {
            return false;
        }
        order.setProcessingStartedAt(submitTime);
        order.updateState(OrderState.PROCESSING);
        int sizeBeforePut = ordersByTime.size();
        ordersByTime.put(order.getOrderedAt(), order);
        if (ordersByTime.size() > sizeBeforePut) {
            currentNumOrders++;
            currentNumItems += numItems;
        }
        updateLastModifiedAt(submitTime);
        incrementStateCounters(orderStateCountsByTime, order.getOrderedAt(), OrderState.CREATED);
        incrementStateCounters(orderStateCountsByTime, submitTime, OrderState.PROCESSING);
        return true;
    }

    @Override
    public boolean addOrder(Order order) {
        int numItems = order.getOrderItems().size();
        if (numItems > maxAllowedItems - currentNumItems) {
            return false;
        }
        int sizeBeforePut = ordersByTime.size();
        ordersByTime.put(order.getOrderedAt(), order);
        if (ordersByTime.size() > sizeBeforePut) {
            currentNumOrders++;
            currentNumItems += numItems;
        }
        // Since addOrder can be used generically for different order states, different timestamp updates will result.
        switch (order.getState()) {
            case CREATED:
                updateFirstModifiedAt(order.getOrderedAt());
                incrementStateCounters(orderStateCountsByTime, order.getOrderedAt(), OrderState.CREATED);
                break;
            case PROCESSING:
                updateLastModifiedAt(order.getProcessingStartedAt());
                incrementStateCounters(orderStateCountsByTime, order.getOrderedAt(), OrderState.CREATED);
                incrementStateCounters(orderStateCountsByTime, order.getProcessingStartedAt(), OrderState.PROCESSING);
                break;
            case COMPLETE:
                updateLastModifiedAt(order.getCompletedAt());
                incrementStateCounters(orderStateCountsByTime, order.getOrderedAt(), OrderState.CREATED);
                incrementStateCounters(orderStateCountsByTime, order.getProcessingStartedAt(), OrderState.PROCESSING);
                incrementStateCounters(orderStateCountsByTime, order.getCompletedAt(), OrderState.COMPLETE);
                break;
            case REJECTED:
                Timestamp orderedAt = order.getOrderedAt();
                updateFirstModifiedAt(orderedAt);
                updateLastModifiedAt(orderedAt);
                incrementStateCounters(orderStateCountsByTime, order.getOrderedAt(), OrderState.CREATED);
                incrementStateCounters(orderStateCountsByTime, order.getOrderedAt(), OrderState.REJECTED);
                break;
            default:
                break;
        }
        return true;
    }

    /** Update firstModifiedAt if the timestamp is before this.firstModifiedAt */
    private void updateFirstModifiedAt(Timestamp timestamp) {
        if (firstModifiedAt == null || timestamp.before(firstModifiedAt)) {
            // Update first modified time stamp.
            firstModifiedAt = timestamp;
        }
    }

    /** Update lastModifiedAt if the timestamp is after this.lastModifiedAt */
    private void updateLastModifiedAt(Timestamp timestamp) {
        if (lastModifiedAt == null || timestamp.after(lastModifiedAt)) {
            // Update last modified time stamp.
            lastModifiedAt = timestamp;
        }
    }

    @Override
    public Order getAndDequeueOrder(OrderProcessingStrategy strategy) {
        // Currently there is only one order processing strategy.
        if (ordersByTime.isEmpty()) {
            return null;
        }
        Timestamp timestampKey = ordersByTime.keySet().first();
        Order order = ordersByTime.get(timestampKey).first();
        ordersByTime.remove(timestampKey, order);
        currentNumOrders--;
        currentNumItems-=order.getOrderItemsSize();
        return order;
    }

    @Override
    public List<Order> clearFinishedOrders(Timestamp queryTime) {
        TreeMultimap<Timestamp, Order> completedOrders = TreeMultimap.create();
        for (Map.Entry<Timestamp, Order> orderEntry : ordersByTime.entries()) {
            Order order = orderEntry.getValue();
            Timestamp doneTime = null;
            if (order.getProcessingStartedAt() != null) {
                doneTime = new Timestamp(
                        order.getProcessingStartedAt().getTime() + (order.getTotalCookTimeSeconds() * 1000L));

                if (!doneTime.after(queryTime)) {
                    // If order is complete, add finish time to the order.
                    Timestamp orderSubmitTime = orderEntry.getKey();
                    NavigableSet<Order> orders = ordersByTime.get(orderSubmitTime);
                    for (Order orderToUpdate : orders) {
                        if (orderToUpdate.equals(order)) {
                            // MultiMap can have multiple orders at the same timestamp.  Only update the correct order.
                            order.setCompletedAt(doneTime);
                            order.updateState(OrderState.COMPLETE);
                            completedOrders.put(orderSubmitTime, order);
                            // update store status counters
                            currentNumOrders--;
                            currentNumItems -= order.getOrderItemsSize();
                            updateLastModifiedAt(doneTime);
                        }
                    }
                }
            }
        }
        for (Map.Entry<Timestamp, Order> orderEntry : completedOrders.entries()) {
            // remove complete orders from this store.
            ordersByTime.remove(orderEntry.getKey(), orderEntry.getValue());
        }
        return new ArrayList<>(completedOrders.values());
    }

    @Override
    public int getCurrentNumOrders() {
        return currentNumOrders;
    }

    @Override
    public int getCurrentNumItems() {
        return currentNumItems;
    }

    @Override
    public int getMaxAllowedItems() {
        return maxAllowedItems;
    }

    @Override
    public Map<String, Integer> getItemFrequencyCount(){
        Map<String, Integer> itemCounts = new HashMap<>();
        for (Order order : ordersByTime.values()) {
            for (OrderItem item : order.getOrderItems()) {
                String itemName = item.getName();
                if (!itemCounts.containsKey(itemName)) {
                    itemCounts.put(itemName, 1);
                } else {
                    itemCounts.put(itemName, itemCounts.get(itemName) + 1);
                }
            }
        }
        return itemCounts;
    }

    @Override
    public TreeMultimap<Integer, Order> getOrdersByPrice(){
        TreeMultimap<Integer, Order> ordersByPrice = TreeMultimap.create();
        for (Order order : ordersByTime.values()) {
            ordersByPrice.put(order.getTotalPriceCents(), order);
        }
        return ordersByPrice;
    }

    @Override
    public Map<Timestamp, Map<OrderState, Integer>> getOrderStateCountsByTime(){
        return orderStateCountsByTime;
    }

    /** Increment state counts at a given timestamp in a Map <Timestamp, Map<OrderState, Integer>> */
    private void incrementStateCounters(Map<Timestamp, Map<OrderState, Integer>> orderStateCountsByTime,
                                        Timestamp timestamp, OrderState orderState) {
        if (orderStateCountsByTime.containsKey(timestamp)) {
            Map<OrderState, Integer> stateCounts = orderStateCountsByTime.get(timestamp);
            if (stateCounts.containsKey(orderState)) {
                stateCounts.put(orderState, stateCounts.get(orderState) + 1);
            } else {
                stateCounts.put(orderState, 1);
            }
        } else {
            Map<OrderState, Integer> stateCounts = new HashMap<>();
            stateCounts.put(orderState, 1);
            orderStateCountsByTime.put(timestamp, stateCounts);
        }
    }

    @Override
    public Map<String, Integer> getRevenueByItem() {
        Map<String, Integer> revenueByItem = new HashMap<>();
        for (Order order : ordersByTime.values()) {
            for (OrderItem item : order.getOrderItems()) {
                String itemName = item.getName();
                if (!revenueByItem.containsKey(itemName)) {
                    revenueByItem.put(itemName, item.getPriceCents());
                } else {
                    revenueByItem.put(itemName, revenueByItem.get(itemName) + item.getPriceCents());
                }
            }
        }
        return revenueByItem;
    }

    @Override
    public Map<String, Integer> getRevenueByService() {
        Map<String, Integer> revenueByService = new HashMap<>();
        for (Order order : ordersByTime.values()) {
            String serviceName = order.getService();
            if (!revenueByService.containsKey(serviceName)) {
                revenueByService.put(serviceName, order.getTotalPriceCents());
            } else {
                revenueByService.put(serviceName, revenueByService.get(serviceName) + order.getTotalPriceCents());
            }
        }
        return revenueByService;
    }

    @Override
    public int getTotalRevenue() {
        int totalRevenue = 0;
        for (Order order : ordersByTime.values()) {
            totalRevenue =  totalRevenue + order.getTotalPriceCents();
        }
        return totalRevenue;
    }
}
