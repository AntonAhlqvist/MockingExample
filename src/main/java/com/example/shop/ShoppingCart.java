package com.example.shop;

public class ShoppingCart {

    private int itemCount = 0;

    /**
     * Lägger till en vara i kundvagnen.
     */
    public void addItem(String name, double price, int quantity) {
        itemCount += quantity;
    }

    public int getItemCount() {
        return itemCount;
    }
}
