package com.example.demo;

import com.example.annotations.NullCheck;

@NullCheck
public class SimpleClass {
    private final String color;
    private final String shape;

    public SimpleClass(String color, String shape) {
        this.color = color;
        this.shape = shape;
    }
}
