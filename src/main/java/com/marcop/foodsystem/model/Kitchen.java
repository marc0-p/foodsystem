package com.marcop.foodsystem.model;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import java.util.Set;

/**
 * An entity representing a physical kitchen configuration.
 * KitchenBuilder should be used to construct a valid Kitchen
 **/
public class Kitchen {
    // Name to identify this kitchen.
    private final String name;

    // Set of menus which this kitchen can prepare.
    // Kitchens can support multiple Menus (e.g. different brands, time of day, etc).
    private final Set<Menu> menus;

    // Maximum number of items which can be prepared concurrently.
    // If this is 0, it implies that parallelism is unconstrained.
    private final int maxConcurentItems;

    public Kitchen(String name, Set<Menu> menus, int maxConcurentItems) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "Kitchen name must be set and not empty");
        Preconditions.checkArgument(menus != null && !menus.isEmpty(), "Kitchen needs at least one menu");
        Preconditions.checkArgument(maxConcurentItems >= 0, "maxConcurentItems cannot be negative");
        this.name = name;
        this.menus = menus;
        this.maxConcurentItems = maxConcurentItems;
    }

    public String getName() {
        return name;
    }

    public Set<Menu> getMenus() {
        return menus;
    }

    public int getMenusSize() {
        if (menus == null || menus.isEmpty()) {
            return 0;
        }
        return menus.size();
    }

    public int getMaxConcurentItems() {
        return maxConcurentItems;
    }
}
