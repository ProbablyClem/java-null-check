package com.example.demo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimpleClassTest {

    @Test
    void constructor_shouldRejectNullColor() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new SimpleClass(null, "round")
        );
        assertTrue(ex.getMessage().contains("SimpleClass.color"));
    }

    @Test
    void constructor_shouldRejectNullShape() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new SimpleClass("yellow", null)
        );
        assertTrue(ex.getMessage().contains("SimpleClass.shape"));
    }

    @Test
    void constructor_shouldAcceptNonNullValues() {
        assertDoesNotThrow(() -> new SimpleClass("yellow", "round"));
    }
}
