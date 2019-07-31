package com.marcop.foodsystem.store;

import com.marcop.foodsystem.model.ItemState;
import com.marcop.foodsystem.model.Order;
import com.marcop.foodsystem.model.OrderProcessingStrategy;
import com.marcop.foodsystem.model.OrderState;

import java.sql.Timestamp;
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

    // Clear orders based on current time and items' cook times.
    void clearFinishedOrders(Timestamp submitTime);

    // Get number of orders in store.
    int getCurrentNumOrders();

    // Get number of items in store orders
    int getCurrentNumItems();

    // Get maximum number of items in store orders
    int getMaxAllowedItems();

    // Get reverse sorted Item frequency count
    Map<String, Integer> getItemFrequencyCount();

    // Get reverse sorted Order price (cents) table
    Map<Order, Integer> getOrdersByPrice();

    // Get order state counts by time
    Map<Timestamp, Map<OrderState, Integer>> getOrderStateCountsByTime();

    // Get item state counts by time
    Map<Timestamp, Map<ItemState, Integer>> getItemStateCountsByTime();

    // Get sorted item revenue (cents) table
    Map<String, Integer> getRevenueByItem();
}
