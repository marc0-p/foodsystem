package com.marcop.foodsystem.store;

import com.google.common.collect.TreeMultimap;
import com.marcop.foodsystem.model.ItemState;
import com.marcop.foodsystem.model.Order;
import com.marcop.foodsystem.model.OrderProcessingStrategy;
import com.marcop.foodsystem.model.OrderState;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/**
 * Data access layer for orders and stats.
 */
public interface OrderStore {
    // Submit a new order to the store. Returns if order was successfully added to the store.
    // Updates state, and timestamps.
    boolean submitOrder(Order order, Timestamp submitTime);

    // Add a new order to the store. Returns if order was successfully added to the store.
    boolean addOrder(Order order);

    // Remove an order from the order store and return it.
    Order getAndDequeueOrder(OrderProcessingStrategy strategy);

    // Clear orders based on current time and items' cook times, returns list of completed orders (now removed).
    List<Order> clearFinishedOrders(Timestamp submitTime);

    // Get number of orders in store.
    int getCurrentNumOrders();

    // Get number of items in store orders
    int getCurrentNumItems();

    // Get maximum number of items in store orders
    int getMaxAllowedItems();

    // Get Item frequency count
    Map<String, Integer> getItemFrequencyCount();

    // Get sorted Order price (cents) table
    TreeMultimap<Integer, Order> getOrdersByPrice();

    // Get sorted order state counts by time
    Map<Timestamp, Map<OrderState, Integer>> getOrderStateCountsByTime();

    // Get revenue (cents) by item table
    Map<String, Integer> getRevenueByItem();

    // Get revenue (cents) by service table
    Map<String, Integer> getRevenueByService();

    // Get total revenue (cents).
    int getTotalRevenue();
}
