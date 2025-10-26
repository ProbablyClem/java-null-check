package com.example.demo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BananaTest {

    @Test
    void constructor_shouldRejectNullColor() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new Banana(null, "round")
        );
        assertTrue(ex.getMessage().contains("Banana.color"));
    }

    @Test
    void constructor_shouldRejectNullShape() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new Banana("yellow", null)
        );
        assertTrue(ex.getMessage().contains("Banana.shape"));
    }

    @Test
    void constructor_shouldAcceptNonNullValues() {
        assertDoesNotThrow(() -> new Banana("yellow", "round"));
    }
}
