package net.wasdev.wlp.maven.test.app;

import static org.junit.Assert.*;

import java.io.File;
import org.junit.Test;

/**
 * 
 * Web application test case
 * 
 */

public class ConfigDropinsTest {

    public final String CONFIG_XML = "liberty-plugin-config.xml";

    @Test
    public void testApplicationFileExist() throws Exception {
        File f = new File("liberty/usr/servers/test/apps/appsdirectory-configdropins-it.war");
        assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
    }
    
    @Test
    public void testConfigDirectoryFileExist() throws Exception {
        File f = new File("liberty/usr/servers/test/configDropins/overrides");
        assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
    }
}
