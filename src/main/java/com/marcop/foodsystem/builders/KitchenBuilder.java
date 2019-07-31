package com.marcop.foodsystem.builders;

import com.marcop.foodsystem.model.Kitchen;
import com.marcop.foodsystem.model.Menu;

import java.util.*;

/**
 * Constructs a Kitchen object.
 */
public class KitchenBuilder {

    private static int DEFAULT_MAX_CONCURENT_ITEMS = 0;

    private Kitchen kitchen;
    private String name;
    private Map<String, Menu> menusByName;
    private int maxConcurrentItems;

    public KitchenBuilder() {
        kitchen = null;
        name = null;
        menusByName = new HashMap<>();
        maxConcurrentItems = DEFAULT_MAX_CONCURENT_ITEMS;
    }

    /** Set name of the Kitchen */
    public void setName(String name) {
        this.name = name;
    }

    /** Add a Menu to the Kitchen. This will enforce only one menu per name. */
    public void addMenu(Menu menu) {
        menusByName.put(menu.getName(), menu);
    }

    /** Set maxConcurentItems for the Kitchen */
    public void setMaxConcurrentItems(int maxConcurentItems) {
        this.maxConcurrentItems = maxConcurentItems;
    }

    public Kitchen build() {
        return new Kitchen(name, new HashSet<>(menusByName.values()), maxConcurrentItems);
    }

}
