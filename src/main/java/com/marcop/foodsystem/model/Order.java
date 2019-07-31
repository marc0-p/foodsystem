package com.marcop.foodsystem.model;

import java.sql.Timestamp;
import java.util.List;

/**
 * Represents a food order, which has a list of items, and other attributes.
 */
public class Order implements Comparable<Order> {
    // When the order was submitted to the system.
    private final Timestamp orderedAt;
    // First and last name of ordering individual.
    private final String name;
    // Food Ordering App Name (should be enum, if all apps are known).
    private final String service;
    // list of items in the order.
    private final List<OrderItem> orderItems;
    // When the order was began processing.
    private Timestamp processingStartedAt;
    // When the order was completed.
    private Timestamp completedAt;
    // State of the order (e.g. completed)
    private OrderState state;
    // Total cook time of the order (current system will only process an order if all the items can be started).
    private int totalCookTimeSeconds;

    public Order(Timestamp orderedAt, String name, String service, List<OrderItem> orderItems) {
        this.orderedAt = orderedAt;
        this.name = name;
        this.service = service;
        this.orderItems = orderItems;
        this.processingStartedAt = null;
        this.completedAt = null;
        this.state = OrderState.CREATED;
    }

    /** Get orderedAt as a Timestamp. */
    public Timestamp getOrderedAt() {
        return orderedAt;
    }

    /** Get name. */
    public String getName() {
        return name;
    }

    /** Get service. */
    public String getService() {
        return service;
    }

    /** Get items. */
    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    /** Get processingStartedAt as a Timestamp. */
    public Timestamp getProcessingStartedAt() {
        return processingStartedAt;
    }

    /** Get completedAt as a Timestamp. */
    public Timestamp getCompletedAt() {
        return completedAt;
    }

    /** Get order state. */
    public OrderState getState() {
        return state;
    }

    /** Get total cook time. */
    public int getTotalCookTimeSeconds() {
        return totalCookTimeSeconds;
    }

    /** Set processingStartedAt. */
    public void setProcessingStartedAt(Timestamp ts) {
        processingStartedAt = ts;
    }

    /** Set processingStartedAt. */
    public void setCompletedAt(Timestamp ts) {
        completedAt = ts;
    }

    /** Update state of the order. */
    public void updateState(OrderState newState) {
        state = newState;
    }

    /** Set total cook time. */
    public int setTotalCookTimeSeconds(int totalCookTimeSeconds) {
        this.totalCookTimeSeconds = totalCookTimeSeconds;
    }

    @Override
    public int compareTo(Order anotherOrder) {
        int orderedAtCompare = anotherOrder.getOrderedAt().compareTo(orderedAt);
        if (orderedAtCompare != 0) {
            return orderedAtCompare;
        }
        int nameCompare = anotherOrder.getName().compareTo(name);
        if (nameCompare != 0) {
            return nameCompare;
        }
        return anotherOrder.getService().compareTo(service);
    }
}
