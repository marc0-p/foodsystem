package com.marcop.foodsystem.model;

public enum OrderState {
    // New Order has been created
    CREATED,
    // Order has been rejected
    REJECTED,
    // Order is being processed (cooked).
    PROCESSING,
    // Order has been completed
    COMPLETE
}
