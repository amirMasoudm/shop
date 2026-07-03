package org.example.shop1.model.dto;

import java.math.BigDecimal;
import java.util.List;

public class OrderRequestDto {

    private String address;
    private List<CartItemDto> items;
    private String shippingMethod;
    private BigDecimal shippingCost;

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public List<CartItemDto> getItems() { return items; }
    public void setItems(List<CartItemDto> items) { this.items = items; }

    public String getShippingMethod() { return shippingMethod; }
    public void setShippingMethod(String shippingMethod) { this.shippingMethod = shippingMethod; }

    public BigDecimal getShippingCost() { return shippingCost; }
    public void setShippingCost(BigDecimal shippingCost) { this.shippingCost = shippingCost; }

    public static class CartItemDto {
        private String productId;
        private int quantity;

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
}