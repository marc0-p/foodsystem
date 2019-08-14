package com.marcop.foodsystem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.marcop.foodsystem.model.OrderItem;

import java.util.ArrayList;
import java.util.List;

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

    /** Convert to OrderItems (flat list, without quantity) */
    public List<OrderItem> toOrderItems() {
        List<OrderItem> orderItems = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            orderItems.add(new OrderItem(name, pricePerUnitCents));
        }
        return orderItems;
    }
}
