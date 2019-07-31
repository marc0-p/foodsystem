package com.marcop.foodsystem.store;

import com.google.common.collect.TreeMultimap;
import com.marcop.foodsystem.model.*;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

/**
 * In-memory implementation of an OrderStore.
 */
public class OrderInMemoryStore implements OrderStore{

    private static final int DEFAULT_MAX_ALLOWED_ITEMS = Integer.MAX_VALUE;

    private final int maxAllowedItems;
    private int currentNumOrders;
    private int currentNumItems;
    private TreeMultimap<Timestamp, Order> ordersByTime;

    public OrderInMemoryStore(int maxAllowedItems) {
        this.maxAllowedItems = maxAllowedItems;
        currentNumOrders = 0;
        currentNumItems = 0;
        this.ordersByTime = TreeMultimap.create();
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
        order.updateState(OrderState.SUBMITTED);
        ordersByTime.put(order.getOrderedAt(), order);
        currentNumOrders++;
        currentNumItems += numItems;
        return true;
    }

    @Override
    public boolean addOrder(Order order) {
        int numItems = order.getOrderItems().size();
        if (numItems > maxAllowedItems - currentNumItems) {
            return false;
        }
        ordersByTime.put(order.getOrderedAt(), order);
        currentNumOrders++;
        currentNumItems += numItems;
        return true;
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
        return order;
    }

    @Override
    void clearFinishedOrders(Timestamp submitTime) {
        for (Order order : ordersByTime) {
            for (OrderItem item : order.getOrderItems()) {
                if (item.getCookTimeSeconds()
            }
        }
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
        return itemCounts;
    }

    @Override
    public Map<Order, Integer> getOrdersByPrice(){
        Map<Order, Integer> ordersByPrice = new HashMap<>();
        return ordersByPrice;
    }

    @Override
    public Map<Timestamp, Map<OrderState, Integer>> getOrderStateCountsByTime(){
        Map<Timestamp, Map<OrderState, Integer>> orderStateCountsByTime = new HashMap<>();
        return orderStateCountsByTime;
    }

    @Override
    public Map<Timestamp, Map<ItemState, Integer>> getItemStateCountsByTime(){
        Map<Timestamp, Map<ItemState, Integer>> itemStateCountsByTime = new HashMap<>();
        return itemStateCountsByTime;
    }

    @Override
    public Map<String, Integer> getRevenueByItem(){
        Map<String, Integer> revenueByItem = new HashMap<>();
        return revenueByItem;
    }
}
