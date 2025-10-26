package com.example.demo;

import com.example.annotations.NullCheck;
import lombok.RequiredArgsConstructor;
import org.springframework.util.Assert;

@NullCheck
@RequiredArgsConstructor
public class Banana {
    private final String color;
    private final String shape;
}
