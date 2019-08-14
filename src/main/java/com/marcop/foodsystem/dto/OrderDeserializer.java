package com.marcop.foodsystem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.marcop.foodsystem.model.Order;
import com.marcop.foodsystem.model.OrderItem;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Utilities for orders
 */
public class OrderDeserializer extends StdDeserializer<Order> {

    private final ObjectMapper MAPPER = new ObjectMapper();

    public OrderDeserializer() {
        this(null);
    }

    public OrderDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Order deserialize(JsonParser parser, DeserializationContext deserializer) throws IOException {
        ObjectCodec codec = parser.getCodec();
        JsonNode node = codec.readTree(parser);

        JsonNode orderedAtNode = node.get("ordered_at");
        Timestamp orderedAt =  Timestamp.valueOf(orderedAtNode.asText().replace('T', ' '));

        JsonNode nameNode = node.get("name");
        String name =  nameNode.asText();

        JsonNode serviceNode = node.get("service");
        String service =  serviceNode.asText();

        Iterator<JsonNode> itemsNode = node.get("items").elements();
        List<OrderItem> items = new ArrayList<>();
        while (itemsNode.hasNext()) {
            JsonNode itemNode = itemsNode.next();
            OrderItemDto orderItemDto = MAPPER.treeToValue(itemNode, OrderItemDto.class);
            if (orderItemDto != null) {
                items.addAll(orderItemDto.toOrderItems());
            }
        }
        return new Order(orderedAt, name, service, items);
    }
}
