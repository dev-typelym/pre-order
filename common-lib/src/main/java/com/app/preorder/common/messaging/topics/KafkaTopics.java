package com.app.preorder.common.messaging.topics;

public final class  KafkaTopics {
    public static final String INVENTORY_STOCK_EVENTS_V1 = "inventory.stock-events.v1";
    public static final String INVENTORY_STOCK_RESTORE_REQUEST_V1 = "inventory.stock-restore.request.v1";
    public static final String MEMBER_CART_CREATE_REQUEST_V1 = "member.cart-create-request.v1";
    private KafkaTopics() {}
}
