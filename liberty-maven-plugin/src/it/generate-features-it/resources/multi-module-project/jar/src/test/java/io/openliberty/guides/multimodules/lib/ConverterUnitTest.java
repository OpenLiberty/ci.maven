package io.openliberty.guides.multimodules.lib;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConverterUnitTest {

    @Test
    public void testHeightFeet() {
        int feet = Converter.getFeet(61);
        assertEquals(2, feet);
    }

}