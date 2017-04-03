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

public class DefaultConfigTest {

    private String serverName = "test";

    @Test
    public void testPackageFileLocation() {
        Path path = Paths.get(serverName + ".zip");
        assertTrue(Files.exists(path));
    }
}
