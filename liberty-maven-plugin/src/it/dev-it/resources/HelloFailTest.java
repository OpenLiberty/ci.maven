package com.demo.test;

import static org.junit.Assert.assertEquals;

import com.demo.HelloWorld;

import org.junit.Test;

public class HelloFailTest {
	
    @Test
    public void testHello() {
        assertEquals("invalid", new HelloWorld().helloWorld());
    }
}
