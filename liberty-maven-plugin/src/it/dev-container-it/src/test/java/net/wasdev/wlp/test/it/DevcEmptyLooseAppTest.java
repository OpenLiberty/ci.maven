package net.wasdev.wlp.test.it;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration test for ci.common fix: {@code libertyDevc} must not throw
 * {@code NullPointerException} when the loose application XML file is empty
 * or malformed (e.g. written as an empty placeholder before the EAR build
 * completes).
 *
 * <p>Before starting container dev mode the test pre-creates an empty
 * {@code target/.libertyDevc/apps/rest.war.xml} file to simulate the race
 * condition described in the issue. Dev mode must reach "Liberty is running
 * in dev mode." and the log must contain no {@code NullPointerException}.
 */
public class DevcEmptyLooseAppTest extends BaseDevTest {

    private static final String PROJECT_ROOT = "../resources/container-empty-looseapp-project";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        setUpBeforeClass(null, PROJECT_ROOT, true, false, null, null);

        // Pre-create an empty loose-app XML placeholder to simulate the race
        // condition where the file exists but has not been written yet.
        File looseAppDir = new File(PROJECT_ROOT + "/target/.libertyDevc/apps");
        looseAppDir.mkdirs();
        File emptyLooseApp = new File(looseAppDir, "rest.war.xml");
        try (FileWriter fw = new FileWriter(emptyLooseApp)) {
            // intentionally empty — zero bytes
        }
        assertTrue("Empty loose-app placeholder must exist before starting dev mode",
                emptyLooseApp.exists());
        assertTrue("Empty loose-app placeholder must be zero bytes",
                emptyLooseApp.length() == 0);

        startProcess("-Dcontainer -Dliberty.dev.podman=true -DcontainerBuildTimeout=599", true);
    }

    @AfterClass
    public static void cleanUpAfterClass() throws Exception {
        BaseDevTest.cleanUpAfterClass();
    }

    /**
     * Verifies that dev mode starts successfully despite the empty loose-app XML.
     * Before the fix, a {@code NullPointerException} terminated startup immediately.
     */
    @Test
    public void devcStartsWithoutNpe() throws Exception {
        assertTrue("Dev mode must start successfully (container image built): " + getLogTail(),
                verifyLogMessageExists("Completed building container image.", 120000));
        assertTrue("Liberty must start in dev mode despite the empty loose-app XML: " + getLogTail(),
                verifyLogMessageExists("Liberty is running in dev mode.", 120000));
    }

    /**
     * Verifies that no {@code NullPointerException} was emitted during startup.
     * This is the primary regression guard for the NPE fix.
     */
    @Test
    public void noNullPointerExceptionInLog() throws Exception {
        // Give dev mode a moment to complete startup before inspecting logs.
        assertTrue("Dev mode must have started before checking for NPE: " + getLogTail(),
                verifyLogMessageExists("Liberty is running in dev mode.", 120000));

        assertFalse("NullPointerException must not appear in the dev mode log: " + getLogTail(),
                readFile("NullPointerException", logFile));
    }

}
