package com.tidyfy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MainTest {

    @Test
    void greetingIncludesName() {
        assertEquals("Olá, Tidyfy!", Main.greeting("Tidyfy"));
    }
}
