package com.marcop.foodsystem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.marcop.foodsystem.model.MenuItem;

public class MenuItemDto {
    private String name;
    @JsonProperty("cook_time")
    private int cookTimeSeconds;

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    int getCookTimeSeconds() {
        return cookTimeSeconds;
    }

    void setCookTimeSeconds(int cookTimeSeconds) {
        this.cookTimeSeconds = cookTimeSeconds;
    }

    /** Convert to MenuItem */
    public MenuItem toMenuItem() {
        return new MenuItem(name, cookTimeSeconds);
    }
}
