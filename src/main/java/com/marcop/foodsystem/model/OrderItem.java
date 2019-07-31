package com.marcop.foodsystem.model;

/**
 * An item associated with an order, and appropriate attributes.
 */
public class OrderItem {
    private final String name;
    private final int priceCents;
    private ItemState state;
    private int cookTimeSeconds;

    public OrderItem(String name, int priceCents) {
        this.name = name;
        this.priceCents = priceCents;
        this.state = ItemState.PENDING;
    }

    public ItemState getState() {
        return state;
    }

    public void updateState(ItemState newState) {
        this.state = newState;
    }

    public String getName() {
        return name;
    }

    public int getPriceCents() {
        return priceCents;
    }

    public int getCookTimeSeconds() {
        return cookTimeSeconds;
    }

    public void setCookTimeSeconds(int cookTimeSeconds) {
        this.cookTimeSeconds = cookTimeSeconds;
    }
}
