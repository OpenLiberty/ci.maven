package net.wasdev.wlp.maven.test.app;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;


/**
 *
 * Default Config Test Case
 *
 */

public class PackagefileConfigAllTestIT {

    private String projectBuildName = "config-packagefile-all-it-1.0-SNAPSHOT";
    
    @Test
    public void testPackageFileJarNotExists() {
        Path path = Paths.get(System.getProperty("user.dir") + "/target/" + projectBuildName + ".jar");
        assertFalse(Files.exists(path));
    }
    
    @Test
    public void testPackageFileZipExists() {
        Path path = Paths.get(System.getProperty("user.dir") + "/target/" + projectBuildName + ".zip");
        assertTrue(Files.exists(path));
    }
}
