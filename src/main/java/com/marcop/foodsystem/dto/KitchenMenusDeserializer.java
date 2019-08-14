package com.marcop.foodsystem.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.common.base.Strings;
import com.marcop.foodsystem.model.Menu;


import java.io.IOException;
import java.util.*;

/**
 * Utilities for kitchen config
 */
public class KitchenMenusDeserializer extends StdDeserializer<KitchenMenuItemsDto> {

    private final ObjectMapper MAPPER = new ObjectMapper();

    public KitchenMenusDeserializer() {
        this(null);
    }

    public KitchenMenusDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public KitchenMenuItemsDto deserialize(JsonParser parser, DeserializationContext deserializer)
            throws IOException {
        ObjectCodec codec = parser.getCodec();
        JsonNode node = codec.readTree(parser);
        Iterator<JsonNode> kitchens = node.elements();

        Map<String, Set<Menu>> menusByKitchenName = new HashMap<>();
        while (kitchens.hasNext()) {
            JsonNode kitchenNode = kitchens.next();
            String kitchenName = kitchenNode.get("name").asText();
            Iterator<JsonNode>  menusNode = kitchenNode.get("menus").elements();
            Set<Menu> menus = new HashSet<>();

            while (menusNode.hasNext()) {
                JsonNode menuNode = menusNode.next();
                String menuName = menuNode.get("name").asText();
                Iterator<JsonNode>  itemsNode = menuNode.get("menu_items").elements();
                if (Strings.isNullOrEmpty(menuName) || itemsNode == null) {
                    continue;
                }
                Menu menu = new Menu(menuName);
                while (itemsNode.hasNext()) {
                    JsonNode itemNode = itemsNode.next();
                    MenuItemDto menuItemDto = MAPPER.treeToValue(itemNode, MenuItemDto.class);
                    if (menuItemDto != null) {
                        menu.addMenuItem(menuItemDto.toMenuItem());
                    }
                }
                menus.add(menu);
            }
            if (!Strings.isNullOrEmpty(kitchenName) || !menus.isEmpty()) {
                menusByKitchenName.put(kitchenName, menus);
            }
        }
        KitchenMenuItemsDto kitchenMenuItemsDto = new KitchenMenuItemsDto();
        kitchenMenuItemsDto.setMenusByKitchenName(menusByKitchenName);
        return kitchenMenuItemsDto;
    }
}
