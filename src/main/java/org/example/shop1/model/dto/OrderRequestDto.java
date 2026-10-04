package org.example.shop1.model.dto;

import org.example.shop1.model.enums.OrderItemType;

import java.util.List;

public class OrderRequestDto {

    private String address;
    private List<CartItemDto> items;
    // فقط نامِ روش (بدونِ هزینه) — هزینه دیگر از کلاینت گرفته نمی‌شود، پس‌کرایه است
    private String shippingMethod;

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public List<CartItemDto> getItems() { return items; }
    public void setItems(List<CartItemDto> items) { this.items = items; }

    public String getShippingMethod() { return shippingMethod; }
    public void setShippingMethod(String shippingMethod) { this.shippingMethod = shippingMethod; }

    public static class CartItemDto {
        private String productId;
        private int quantity;
        // نبودش یعنی PRODUCT (سازگاریِ عقب با سبدِ فعلی که این فیلد را نمی‌فرستد)
        private OrderItemType itemType;

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public OrderItemType getItemType() { return itemType != null ? itemType : OrderItemType.PRODUCT; }
        public void setItemType(OrderItemType itemType) { this.itemType = itemType; }
    }
}