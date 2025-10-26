package com.example;

public final class Assert {

    public static void notNull(String field, Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Missing mandatory value in " + field);
        }
    }
}
