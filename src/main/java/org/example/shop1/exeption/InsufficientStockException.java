package org.example.shop1.exeption;

import org.example.shop1.model.entity.Order;

public class InsufficientStockException extends RuntimeException {
    private final Order updatedOrder;

    public InsufficientStockException(Order updatedOrder) {
        super("به علت عدم موجودی کافی کالا در انبار، فاکتور خرید شما اصلاح شد.");
        this.updatedOrder = updatedOrder;
    }

    public Order getUpdatedOrder() {
        return updatedOrder;
    }
}