package com.example.shop;

import java.util.ArrayList;
import java.util.List;

public class ShoppingCart {

    private List<String> items = new ArrayList<>();

    /**
     * Efter refaktorering:
     * <p>
     * Varorna lagras i en lista och varje enhet av varan läggs till individuellt
     * med hjälp av en loop som hanterar kvantiteten.
     */
    public void addItem(String name, double price, int quantity) {
        for (int i = 0; i < quantity; i++) {
            items.add(name);
        }
    }

    /**
     * Returnerar det totala antalet varor i kundvagnen genom att kontrollera listans
     * storlek.
     */
    public int getItemCount() {
        return items.size();
    }
}
