package com.example.demo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimpleClassLombokTest {

    @Test
    void constructor_shouldRejectNullColor() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new SimpleClassLombok(null, "round")
        );
        assertTrue(ex.getMessage().contains("SimpleClassLombok.color"));
    }

    @Test
    void constructor_shouldRejectNullShape() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new SimpleClassLombok("yellow", null)
        );
        assertTrue(ex.getMessage().contains("SimpleClassLombok.shape"));
    }

    @Test
    void constructor_shouldAcceptNonNullValues() {
        assertDoesNotThrow(() -> new SimpleClassLombok("yellow", "round"));
    }
}
