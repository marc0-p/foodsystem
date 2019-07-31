package com.marcop.foodsystem.model;

public enum OrderState {
    // Order has been created
    CREATED,
    // Order has been submitted for processing (cooking).
    SUBMITTED,
    // Order has been completed
    COMPLETE
}
