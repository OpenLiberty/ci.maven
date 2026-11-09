package net.wasdev.wlp.maven.test.app;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

/**
 * Verifies that the start goal copies the configuration files into the server
 * directory even when the server directory did not exist beforehand (no create goal).
 * See https://github.com/OpenLiberty/ci.maven/issues/2139
 */
public class StartMissingServerDirTest {

    private static final File SERVER_DIR = Paths.get("liberty", "wlp", "usr", "servers", "defaultServer").toFile();

    private void assertCopied(String fileName, String expectedContent) throws Exception {
        File f = new File(SERVER_DIR, fileName);
        assertTrue(f.getCanonicalPath() + " doesn't exist", f.exists());
        String content = new String(Files.readAllBytes(f.toPath()), "UTF-8");
        assertTrue(fileName + " should contain " + expectedContent, content.contains(expectedContent));
    }

    @Test
    public void testServerEnvCopied() throws Exception {
        assertCopied("server.env", "MISSING_DIR_TEST_PROP=fromServerEnvFile");
    }

    @Test
    public void testBootstrapPropertiesCopied() throws Exception {
        assertCopied("bootstrap.properties", "missingDirTestBootstrap=fromBootstrapFile");
    }

    @Test
    public void testJvmOptionsCopied() throws Exception {
        assertCopied("jvm.options", "-Dmissing.dir.test=fromJvmOptionsFile");
    }

    @Test
    public void testServerXmlCopied() throws Exception {
        assertCopied("server.xml", "start without create");
    }
}
