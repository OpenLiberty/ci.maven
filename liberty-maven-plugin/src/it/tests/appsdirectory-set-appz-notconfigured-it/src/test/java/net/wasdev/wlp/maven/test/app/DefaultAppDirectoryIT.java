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
        File f = new File("liberty/usr/servers/test/dropins/appsdirectory-set-appz-notconfigured-it.war");
        Assert.assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
    }
}
