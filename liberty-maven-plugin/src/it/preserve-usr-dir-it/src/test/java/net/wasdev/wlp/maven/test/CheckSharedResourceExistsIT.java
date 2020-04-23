package net.wasdev.wlp.maven.test;

import java.io.File;
import org.junit.Test;

import static junit.framework.Assert.*;

public class CheckSharedResourceExistsIT {
    
    public final String DERBY_RESOURCE = "target/liberty/wlp/usr/shared/resources/derby-10.13.1.1.jar";
    
    @Test
    public void testDerbyExists() throws Exception {
        File f = new File(DERBY_RESOURCE);
        assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
    }
}