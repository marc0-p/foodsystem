package com.marcop.foodsystem.model;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A list of items which can be prepared, and belong to a certain subset. Also attributes which describe the subset.
 * E.g. BubbleTea Menu, or Dinner Menu for brand X, Lunch Menu for brand Y
 * TODO (future work): add Menu ordering constraints.
 **/
public class Menu {

    // List of items
    private Set<MenuItem> menuItems;

    // Name for this menu (e.g. "Dinner Menu"). Menu name for a given kitchen is unique.
    private final String name;

    public Menu(String name) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "Name must be a non-empty String.");
        this.name = name;
        this.menuItems = new HashSet<>();
    }

    /** Add a MenuItem to the Menu. No duplicates are added. */
    public void addMenuItem(MenuItem item) {
        menuItems.add(item);
    }

    /** Add a List of MenuItems to the Menu. No duplicates are added. */
    public void addMenuItems(List<MenuItem> items) {
        menuItems.addAll(items);
    }

    /** Set MenuItems */
    public void setMenuItems(Set<MenuItem> items) {
        menuItems = items;
    }

    /** Get name. */
    public String getName() {
        return name;
    }

    /** Get size of menuItems, or 0 if null or empty. */
    public int getMenuItemsSize() {
        if (menuItems == null || menuItems.isEmpty()) {
            return 0;
        }
        return menuItems.size();
    }

    /** Get menuItems. */
    public Set<MenuItem> getMenuItems() {
        return menuItems;
    }
}
