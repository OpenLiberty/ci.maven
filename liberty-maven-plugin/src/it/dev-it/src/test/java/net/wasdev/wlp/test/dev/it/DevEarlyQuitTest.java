package net.wasdev.wlp.test.dev.it;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Basic test for early quit functionality during dev mode startup for issue #1638.
 * This test verifies that dev mode starts successfully with the early hotkey reader enabled.
 * 
 * Note: Full keyboard input testing during startup is difficult to test reliably in integration
 * tests due to timing issues and framework limitations. The core functionality is tested in
 * unit tests (ci.common DevUtilTest) and can be verified manually.
 */
public class DevEarlyQuitTest extends BaseDevTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        setUpBeforeClass(null, "../resources/basic-dev-project", true, false, null, null);
    }

    @AfterClass
    public static void cleanUpAfterClass() throws Exception {
        BaseDevTest.cleanUpAfterClass(true, false);
    }

    /**
     * Test that dev mode starts successfully with early hotkey reader enabled.
     * This verifies the integration doesn't break normal dev mode startup.
     */
    @Test
    public void testDevModeStartsWithEarlyHotkeyReader() throws Exception {
        // Start dev mode and wait for server to fully start
        startProcess(null, true);

        // Verify server is running
        assertTrue("Server should be started",
                verifyLogMessageExists("CWWKF0011I", 120000));
        assertTrue("Dev mode should be running",
                verifyLogMessageExists("Liberty is running in dev mode.", 60000));

        // Verify process is alive
        assertTrue("Process should be alive", process.isAlive());

        // Normal quit
        writer.write("q");
        writer.flush();
        writer.newLine();
        writer.flush();

        // Wait for process to terminate
        boolean terminated = process.waitFor(30, TimeUnit.SECONDS);
        assertTrue("Process should have terminated after 'q' command", terminated);
        assertFalse("Process should not be alive after quit", process.isAlive());
    }

    /**
     * Test that help command works (verifies hotkey reader is functional).
     */
    @Test
    public void testHelpCommandWorks() throws Exception {
        // Start dev mode and wait for server to fully start
        startProcess(null, true);

        // Verify server is running
        assertTrue("Server should be started",
                verifyLogMessageExists("CWWKF0011I", 120000));

        // Send 'h' command to show help
        writer.write("h");
        writer.newLine();
        writer.flush();

        // Wait a bit for help to be displayed
        Thread.sleep(2000);

        // Process should still be alive (help shouldn't quit)
        assertTrue("Process should still be alive after 'h' command", process.isAlive());

        // Normal quit
        writer.write("q");
        writer.newLine();
        writer.flush();

        boolean terminated = process.waitFor(30, TimeUnit.SECONDS);
        assertTrue("Process should have terminated after 'q' command", terminated);
    }

    // Helper method to read file content
    private String readFile(File file) throws IOException {
        if (!file.exists()) {
            return "";
        }
        return new String(java.nio.file.Files.readAllBytes(file.toPath()));
    }
}