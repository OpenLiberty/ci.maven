package it.com.demo.test;

import static org.junit.Assert.assertEquals;

import com.demo.HelloWorld;

import org.junit.Test;

public class HelloFailIT {
	
    @Test
    public void testHello() {
        assertEquals("invalid", new HelloWorld().helloWorld());
    }
}
