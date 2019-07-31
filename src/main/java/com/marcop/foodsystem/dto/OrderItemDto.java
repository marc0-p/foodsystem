package com.marcop.foodsystem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OrderItemDto {
    private String name;
    private int quantity;
    @JsonProperty("price_per_unit")
    private int pricePerUnitCents;

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    int getQuantity() {
        return quantity;
    }

    void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    int getPricePerUnitCents() {
        return quantity;
    }

    void setPricePerUnitCents(int pricePerUnitCents) {
        this.pricePerUnitCents = pricePerUnitCents;
    }
}
