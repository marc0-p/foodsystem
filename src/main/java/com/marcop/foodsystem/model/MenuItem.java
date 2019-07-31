package com.marcop.foodsystem.model;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * An item which can be ordered and/or prepared by a Kitchen.
 * TODO (future work): add ingredient info, and interface to check if item is able to be prepared or not.
 * TODO (future work): getCookTime could use additional information, to dynamically compute cook time.
 *      (e.g. something needs to be prepared from scratch vs from already prepared sub-components.)
 **/
public class MenuItem {

    // Name of menu item (e.g. "Tiramisu").
    private final String name;

    // How long (in seconds) it takes to prepare the item.
    private final int cookTimeSeconds;

    public MenuItem (String name, int cookTimeSeconds) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "Name must be a non-empty String.");
        Preconditions.checkArgument(cookTimeSeconds > 0, "Cook time must be greater than 0.");
        this.name = name;
        this.cookTimeSeconds = cookTimeSeconds;
    }

    public String getName() {
        return name;
    }

    public int getCookTimeSeconds() {
        return cookTimeSeconds;
    }
}
