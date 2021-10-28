package net.wasdev.wlp.test.servlet.it;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * Deployed app archive to apps test case
 * 
 */
public class AppFileTest {
    @Test
    public void testApplicationFileExist() throws Exception {
        File f = new File("liberty/wlp/usr/servers/myTestServer/apps/my-war.war");
        Assert.assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
    }
}
