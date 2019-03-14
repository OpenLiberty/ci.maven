package net.wasdev.wlp.maven.test.app;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * Web application test case
 * 
 */

public class DefaultAppDirectoryIT {
    
    @Test
    public void testApplicationFileExist() throws Exception {
        File f = new File("target/liberty/usr/servers/test/apps/appsdirectory-set-appz-configured-it.war.xml");
        Assert.assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
    }
}
