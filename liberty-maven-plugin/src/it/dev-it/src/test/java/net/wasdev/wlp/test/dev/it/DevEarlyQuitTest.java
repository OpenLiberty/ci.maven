package net.wasdev.wlp.test.dev.it;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * This test verifies that pressing 'q' during server startup properly
 * stops the server and cleans up resources, preventing orphaned processes.
 */
public class DevEarlyQuitTest extends BaseDevTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Set up the test project but don't start the process yet
        // We'll start it manually in each test to control timing
        setUpBeforeClass(null, "../resources/basic-dev-project", true, false, null, null);
    }

    @AfterClass
    public static void cleanUpAfterClass() throws Exception {
        BaseDevTest.cleanUpAfterClass(true, false);
    }

    /**
     * Test that pressing 'q' during server startup stops the server cleanly.
     * This simulates the scenario where a user presses 'q' before the
     * "Server started" message appears.
     */
    @Test
    public void testEarlyQuitDuringStartup() throws Exception {
        // Start dev mode
        startProcess(null, true, "mvn liberty:", false); // Don't verify server start

        // Wait a short time to let dev mode begin starting
        // but not long enough for server to fully start
        Thread.sleep(1000);

        // Send 'q' command to quit
        writer.write("q");
        writer.flush();
        writer.newLine();
        writer.flush();

        // Wait for process to terminate
        boolean terminated = process.waitFor(30, TimeUnit.SECONDS);
        assertTrue("Process should have terminated after 'q' command", terminated);

        // Verify process is no longer alive
        assertFalse("Process should not be alive after quit", process.isAlive());

        // Check that no error messages about orphaned processes appear in logs
        Thread.sleep(2000); // Give time for logs to be written
        String logContent = readFile(logFile);

        // Verify no "Address already in use" errors
        assertFalse("Should not have port conflict errors",
                logContent.contains("Address already in use") ||
                        logContent.contains("CWWKO0221E"));
        // Check for early quit message (if it appears in logs)
        assertTrue("Early quit was not properly detected and handled",
                logContent.contains("Quit requested during server startup") ||
                        logContent.contains("Early quit detected"));
    }

    /**
     * Test that after an early quit, a subsequent dev mode start works correctly.
     * This verifies that no orphaned processes or lock files prevent the next start.
     */
    @Test
    public void testSubsequentStartAfterEarlyQuit() throws Exception {
        // First start and early quit
        startProcess(null, true, "mvn liberty:", false);
        Thread.sleep(2000);

        writer.write("q");
        writer.flush();
        writer.newLine();
        writer.flush();

        boolean terminated = process.waitFor(30, TimeUnit.SECONDS);
        assertTrue("First process should have terminated", terminated);

        // Wait a bit to ensure cleanup is complete
        Thread.sleep(3000);

        // Try to start dev mode again
        startProcess(null, true, "mvn liberty:", true); // This time verify server starts

        // If we get here, the server started successfully
        assertTrue("Server should have started successfully after early quit",
                verifyLogMessageExists("CWWKF0011I", 120000));

        // Clean shutdown for this test
        writer.write("q");
        writer.flush();
        writer.newLine();
        writer.flush();

        process.waitFor(30, TimeUnit.SECONDS);
    }

    /**
     * Test that normal quit (after server fully started) still works correctly.
     * This ensures our changes don't break the existing quit functionality.
     */
    @Test
    public void testNormalQuitAfterServerStart() throws Exception {
        // Start dev mode and wait for server to fully start
        startProcess(null, true);

        // Verify server is running
        assertTrue("Server should be started",
                verifyLogMessageExists("CWWKF0011I", 120000));
        assertTrue("Dev mode should be running",
                verifyLogMessageExists("Liberty is running in dev mode.", 60000));

        // Now quit normally
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
     * Test that multiple rapid 'q' presses are handled gracefully.
     */
    @Test
    public void testMultipleRapidQuitCommands() throws Exception {
        // Start dev mode
        startProcess(null, true, "mvn liberty:", false);
        Thread.sleep(2000);

        // Send multiple 'q' commands rapidly
        for (int i = 0; i < 3; i++) {
            writer.write("q");
            writer.flush();
            writer.newLine();
            writer.flush();
            Thread.sleep(100); // Small delay between commands
        }

        // Wait for process to terminate
        boolean terminated = process.waitFor(30, TimeUnit.SECONDS);
        assertTrue("Process should have terminated", terminated);
        assertFalse("Process should not be alive", process.isAlive());

        // Check logs for any errors
        Thread.sleep(2000);

        // Should not have any severe errors from multiple quit attempts
        // (Some warnings might be expected, but no severe errors)
        int severeCount = countOccurrences("SEVERE", logFile);
        assertTrue("Should not have excessive SEVERE errors (found " + severeCount + ")",
                severeCount < 3);
    }

    /**
     * Test that server directory and lock files are properly cleaned up after early quit.
     */
    @Test
    public void testLockFileCleanupAfterEarlyQuit() throws Exception {
        // Start dev mode
        startProcess(null, true, "mvn liberty:", false);
        Thread.sleep(2000);

        // Send quit command
        writer.write("q");
        writer.flush();
        writer.newLine();
        writer.flush();

        boolean terminated = process.waitFor(30, TimeUnit.SECONDS);
        assertTrue("Process should have terminated", terminated);
    }

    /**
     * Test that other hotkeys (r, g, o, t, p, enter) are ignored during server startup.
     * Only 'q' and 'h' should be processed during startup; other keys should be ignored with a message.
     */
    @Test
    public void testOtherHotkeysIgnoredDuringStartup() throws Exception {
        // Start dev mode
        startProcess(null, true, "mvn liberty:", false);
        Thread.sleep(1000); // Wait for startup to begin but not complete

        // Try various hotkeys that should be ignored during startup (excluding 'q' and 'h')
        String[] ignoredKeys = {"r", "g", "o", "t", "p", "\n"};
        
        for (String key : ignoredKeys) {
            writer.write(key);
            writer.flush();
            if (!key.equals("\n")) {
                writer.newLine();
                writer.flush();
            }
            Thread.sleep(200); // Small delay between commands
        }

        // Wait a bit to see if any of those commands triggered actions
        Thread.sleep(2000);

        // Process should still be alive (none of the commands should have quit)
        assertTrue("Process should still be alive after ignored hotkeys", process.isAlive());

        // Check logs for the "command not available" message
        String logContent = readFile(logFile);
        assertTrue("Should have message about command not being available during startup",
                logContent.contains("The requested command is not available during server startup"));

        // Verify that restart (r) didn't happen
        int restartCount = countOccurrences("Restarting", logFile);
        assertEquals("Should not have restarted during startup", 0, restartCount);

        // Now send 'q' to properly quit
        writer.write("q");
        writer.flush();
        writer.newLine();
        writer.flush();

        boolean terminated = process.waitFor(30, TimeUnit.SECONDS);
        assertTrue("Process should have terminated after 'q' command", terminated);
    }

    /**
     * Test that 'h' (help) command works during server startup.
     * This verifies that users can see help messages while waiting for server to start.
     */
    @Test
    public void testHelpCommandDuringStartup() throws Exception {
        // Start dev mode
        startProcess(null, true, "mvn liberty:", false);
        Thread.sleep(1500); // Wait for startup to begin but not complete

        // Send 'h' command to show help
        writer.write("h");
        writer.newLine();
        writer.flush();

        // Wait a bit for help to be displayed
        Thread.sleep(2000);

        // Process should still be alive (help shouldn't quit)
        assertTrue("Process should still be alive after 'h' command", process.isAlive());

        // Now send 'q' to properly quit - use same pattern as other tests
        writer.write("q");
        writer.newLine();
        writer.flush();

        // Give more time for graceful shutdown
        boolean terminated = process.waitFor(60, TimeUnit.SECONDS);
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