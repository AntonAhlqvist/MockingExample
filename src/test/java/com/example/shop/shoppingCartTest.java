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
     */
    @Test
    void shouldAddItemToCart() {
        ShoppingCart cart = new ShoppingCart();

        cart.addItem("Mango", 10.0, 1);

        assertEquals(1, cart.getItemCount());
    }
}
