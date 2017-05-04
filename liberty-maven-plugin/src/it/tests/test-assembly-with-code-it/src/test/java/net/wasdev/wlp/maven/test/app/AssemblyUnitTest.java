package net.wasdev.wlp.maven.test.app;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

import net.wasdev.wlp.maven.test.support.HttpUtils;

/**
 * Unit test for liberty-assembly
 * 
 */

public class AssemblyUnitTest {

    @Test
    public void testAutoCompile() throws Exception {
        assertTrue("Testing unit test classes are compiled.", true);
    }
}
