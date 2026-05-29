package com.CartService.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Cart implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sessionId;
    private List<CartItem> items = new ArrayList<>();
    private double totalAmount;
    private int itemCount;

    // Constructors
    public Cart() {
    }

    public Cart(String sessionId) {
        this.sessionId = sessionId;
        this.items = new ArrayList<>();
        recalculate();
    }

    public Cart(String sessionId, List<CartItem> items) {
        this.sessionId = sessionId;
        this.items = items != null ? items : new ArrayList<>();
        recalculate();
    }

    /**
     * Recalculates the itemCount and totalAmount dynamically based on the current items list.
     */
    public void recalculate() {
        if (this.items == null) {
            this.itemCount = 0;
            this.totalAmount = 0.0;
            return;
        }
        // itemCount is the number of unique products in the cart (as per Core Requirement 4)
        this.itemCount = this.items.size();
        this.totalAmount = this.items.stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
    }

    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        recalculate();
    }

    public double getTotalAmount() {
        recalculate();
        return totalAmount;
    }

    // Maintained for serialization compatibility
    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public int getItemCount() {
        recalculate();
        return itemCount;
    }

    // Maintained for serialization compatibility
    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    @Override
    public String toString() {
        return "Cart{" +
                "sessionId='" + sessionId + '\'' +
                ", items=" + items +
                ", totalAmount=" + totalAmount +
                ", itemCount=" + itemCount +
                '}';
    }
}
