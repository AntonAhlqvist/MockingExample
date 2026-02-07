package com.example.shop;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ShoppingCart {

    private static class Item {
        String name;
        BigDecimal price;

        /**
         * Representerar en vara i kundvagnen.
         * <p>
         * Sparar namn och pris på varan. Pris lagras som BigDecimal
         * för korrekt hantering av decimaler.
         */
        Item(String name, double price) {
            this.name = name;
            this.price = BigDecimal.valueOf(price);
        }
    }

    private List<Item> items = new ArrayList<>();

    /**
     * Efter refaktorering:
     * <p>
     * Varorna lagras i en lista och varje enhet av varan läggs till individuellt
     * med hjälp av en loop som hanterar kvantiteten.
     */
    public void addItem(String name, double price, int quantity) {
        for (int i = 0; i < quantity; i++) {
            items.add(new Item(name, price));
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
     * Efter refaktorering:
     * <p>
     * Tar bort ett angivet antal förekomster av en vara från kundvagnen.
     * <p>
     * Metoden går igenom listan med varor och tar bort den första
     * matchande varan upprepade gånger tills det angivna antalet varor har
     * tagits bort. Index justeras vid varje borttagning för att undvika
     * hopp över element i listan.
     */
    public void removeItem(String name, int quantity) {
        int removed = 0;
        for (int i = 0; i < items.size() && removed < quantity; i++) {
            if (items.get(i).name.equals(name)) {
                items.remove(i);
                i--;
                removed++;
            }
        }
    }

    /**
     * Efter refaktorering:
     * <p>
     * Beräknar totalpriset för alla varor i kundvagnen.
     * <p>
     * Loopar genom listan med varor och summerar priset för varje vara.
     * BigDecimal används för att hantera decimaler korrekt.
     * <p>
     * Metoden fungerar även om kundvagnen är tom och returnerar då
     * BigDecimal.ZERO, vilket skrivs ut som 0, utan att orsaka fel
     * eller undantag.
     */
    public BigDecimal getTotalPrice() {
        BigDecimal total = BigDecimal.ZERO;

        if (items != null) {
            for (Item vara : items) {
                if (vara != null && vara.price != null) {
                    total = total.add(vara.price);
                }
            }
        }

        return total;
    }
}
