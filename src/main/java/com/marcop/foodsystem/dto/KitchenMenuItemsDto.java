package com.marcop.foodsystem.dto;

import com.marcop.foodsystem.model.Menu;

import java.util.Map;
import java.util.Set;

/** Wrapper around kitchen name to menus mapping */
public class KitchenMenuItemsDto {
    private Map<String, Set<Menu>> menusByKitchenName;

    public Map<String, Set<Menu>> getMenusByKitchenName() {
        return menusByKitchenName;
    }

    public void setMenusByKitchenName(Map<String, Set<Menu>> menusByKitchenName) {
        this.menusByKitchenName = menusByKitchenName;
    }
}
