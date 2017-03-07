package net.wasdev.wlp.maven.test.app;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

import net.wasdev.wlp.maven.test.support.HttpUtils;

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
