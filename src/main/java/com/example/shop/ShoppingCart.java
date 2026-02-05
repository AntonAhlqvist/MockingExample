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

    /**
     * Tar bort ett angivet antal förekomster av en vara från kundvagnen.
     * <p>
     * Loopar genom listan och tar bort varan när den hittas tills
     * det angivna antalet har tagits bort eller inga fler förekomster finns.
     */
    public void removeItem(String name, int quantity) {
        int count = 0;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).equals(name)) {
                items.remove(i);
                i--;
                count++;
                if (count == quantity) {
                    break;
                }
            }
        }
    }
}
