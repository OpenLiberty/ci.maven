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

public class PackagefileConfigTestIT {

    private String projectBuildName = "config-packagefile-runnable-it-1.0-SNAPSHOT";
    
    @Test
    public void testPackageFileZipNotExists() {
        Path path = Paths.get(System.getProperty("user.dir") + "/target/" + projectBuildName + ".zip");
        assertFalse(Files.exists(path));
    }
}
