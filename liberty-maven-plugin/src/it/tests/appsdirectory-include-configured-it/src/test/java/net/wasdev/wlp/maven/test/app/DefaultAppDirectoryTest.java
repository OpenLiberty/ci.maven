package net.wasdev.wlp.maven.test.app;

import java.io.File;
import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * Web application test case
 * 
 */

public class DefaultAppDirectoryTest {

    public final String CONFIG_XML = "liberty-plugin-config.xml";

    @Test
    public void testConfigPropFileExist() throws Exception {
        File f = new File(CONFIG_XML);
        Assert.assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
    }
    
    @Test
    public void testApplicationFileExist() throws Exception {
        File f = new File("liberty/usr/servers/test/apps/appsdirectory-include-configured-it.war");
        Assert.assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
    }
    
    @Test
    public void testConfigDirectoryFileExist() throws Exception {
        File f = new File("liberty/usr/servers/test/testDir", "server.xml");
        Assert.assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
    }
}
