package net.wasdev.wlp.maven.test.app;

import static org.junit.Assert.*;

import java.io.File;
import org.junit.Test;

/**
 * 
 * configDirectory test case
 * 
 */

public class ConfigDirectoryTest {

    @Test
    public void testConfigDirectoryFileExist() throws Exception {
        File f1 = new File("liberty/usr/servers/test", "server1.xml");
        assertTrue(f1.getCanonicalFile() + " doesn't exist", f1.exists());
        
        File f2 = new File("liberty/usr/servers/test/testDir", "bootstrap1.properties");
        assertTrue(f2.getCanonicalFile() + " doesn't exist", f2.exists());
    }
}
