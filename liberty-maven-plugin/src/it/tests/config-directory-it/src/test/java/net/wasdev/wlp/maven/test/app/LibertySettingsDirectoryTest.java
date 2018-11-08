package net.wasdev.wlp.maven.test.app;

import static org.junit.Assert.*;

import java.io.File;
import org.junit.Test;

/**
 * 
 * configDirectory test case
 * 
 */

public class LibertySettingsDirectoryTest {

    @Test
    public void testLibertyConfigDirValidDir() throws Exception {
        File f1 = new File("liberty/etc", "repository.properties");
        assertTrue(f1.getCanonicalFile() + " doesn't exist", f1.exists());
    }
}