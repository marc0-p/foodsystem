package com.marcop.foodsystem.indexing;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.marcop.foodsystem.model.Kitchen;
import com.marcop.foodsystem.model.Menu;
import com.marcop.foodsystem.model.MenuItem;

import java.util.HashMap;
import java.util.Map;

/**
 * Different indexes for the menu items of a kitchen.
 */
public class KitchenMenuItemIndexes {

    private final Map<String, Integer> cookTimeByMenuItemName;

    public KitchenMenuItemIndexes(Kitchen kitchen) {
        cookTimeByMenuItemName = new HashMap<>();
        // Build cookTimeByMenuItemName index.
        for (Menu menu : kitchen.getMenus()) {
            if (menu.getMenuItemsSize() == 0) {
                // Skip any null of empty Menus.
                continue;
            }
            for (MenuItem menuItem : menu.getMenuItems()) {
                Preconditions.checkArgument(
                        menuItem.getCookTimeSeconds() > 0, "Cannot build index due to invalid cook time");
                Preconditions.checkArgument(
                        !Strings.isNullOrEmpty(menu.getName()), "Cannot build index due to invalid item name.");
                cookTimeByMenuItemName.put(menuItem.getName(), menuItem.getCookTimeSeconds());
            }
        }
    }

    public int getCookTime(String menuItemName) {
        return cookTimeByMenuItemName.get(menuItemName);
    }
}
