package com.example.demo;

import com.example.annotations.NullCheck;
import lombok.RequiredArgsConstructor;

@NullCheck
@RequiredArgsConstructor
public final class SimpleClassLombok {
    private final String color;
    private final String shape;
}
