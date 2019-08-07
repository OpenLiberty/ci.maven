package com.demo.test;

import static org.junit.Assert.assertEquals;

import com.demo.HelloWorld;

import org.junit.Test;

public class HelloTest {
	
    @Test
    public void testHello() {
        assertEquals("helloWorld", new HelloWorld().helloWorld());
    }
}
