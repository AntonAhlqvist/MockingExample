package com.example.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShoppingCartTest {

    /**
     * Steg 1 – Red:
     * <p>
     * Ett enkelt test som kontrollerar att det går att lägga till en vara
     * i kundvagnen.
     * <p>
     * Vid detta steg förväntas testet misslyckas eftersom klassen
     * "ShoppingCart" ännu inte är implementerad.
     * <p>
     * --------------------------------------------------------------------
     * <p>
     * Steg 2 - Green:
     * <p>
     * Sedan förra commit har klassen "ShoppingCart" implementerats,
     * så nu lyckas samma test.
     * <p>
     * --------------------------------------------------------------------
     * <p>
     * Steg 3 – Refactor:
     * <p>
     * Klassen "ShoppingCart" har refaktorerats för bättre struktur,
     * till exempel genom att lagra varor i en lista och hantera kvantitet
     * med en loop. Testet fortsätter att gå igenom utan ändringar.
     */
    @Test
    void shouldAddItemToCart() {
        ShoppingCart cart = new ShoppingCart();

        cart.addItem("Mango", 10.0, 1);

        assertEquals(1, cart.getItemCount());
    }

    /**
     * Steg 1 - Red
     * <p>
     * Testar att det går att ta bort varor från kundvagnen.
     * <p>
     * Testet förväntas att misslyckas eftersom metoden "removeItem"
     * inte är implementerad.
     * <p>
     * --------------------------------------------------------------------
     * <p>
     * Steg 2 - Green:
     * <p>
     * Sedan förra commit har metoden "removeItem" implementerats,
     * så nu lyckas samma test.
     * <p>
     */
    @Test
    void shouldRemoveItemFromCart() {
        ShoppingCart cart = new ShoppingCart();

        cart.addItem("Mango", 10.0, 3);
        cart.addItem("Hockeybiljetter", 5.0, 2);

        cart.removeItem("Mango", 2);

        assertEquals(3, cart.getItemCount());
    }
}
