package net.wasdev.wlp.maven.test.app;

import java.io.File;

import org.junit.Test;

import static junit.framework.Assert.*;

// Test server output directory that is overriden by WLP_OUTPUT_DIR in server.env
public class ServerOutputDirIT {
    
    @Test
    public void testWorkareaOutputDir() throws Exception {
        File f = new File("./target/LibertyOutput/test/workarea");
        assertTrue(f.getCanonicalFile() + " doesn't exist.", f.exists());
    }
    
    @Test
    public void testLogsOutputDir() throws Exception {
        File f = new File("./target/LibertyOutput/test/logs/messages.log");
        assertTrue(f.getCanonicalFile() + " doesn't exist.", f.exists());
    }
    
    @Test
    public void testDefaultServerWorkareaDir() throws Exception {
        File f = new File("./target/liberty/usr/servers/test/workarea");
        assertFalse(f.getCanonicalFile() + " is not expected.", f.exists());
    }
    
    @Test
    public void testDefaultServerLogsDir() throws Exception {
        File f = new File("./target/liberty/usr/servers/test/logs");
        assertFalse(f.getCanonicalFile() + " is not expected.", f.exists());
    }
}
