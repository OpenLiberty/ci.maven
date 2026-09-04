package net.wasdev.wlp.test.it;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration test for ci.common fix: {@code libertyDevc} / {@code liberty:devc} must
 * publish the correct container port when Liberty's HTTP/HTTPS port is overridden via
 * {@code liberty.var.*} Maven properties (written to
 * {@code configDropins/overrides/liberty-plugin-variable-config.xml}).
 *
 * <p>Before the fix, {@code getContainerCommand()} always mapped the hardcoded defaults
 * (9080 / 9443) regardless of the configured port, causing
 * "Unable to retrieve locally mapped port" at startup for any non-default port.
 *
 * <p>This test project sets {@code liberty.var.default.http.port=9090} and
 * {@code liberty.var.default.https.port=9453}. The container run command logged
 * by the plugin must contain {@code :9090} (not {@code :9080}) and must not contain
 * the "Unable to retrieve locally mapped port" error message.
 */
public class DevcNonDefaultPortTest extends BaseDevTest {

    private static final String PROJECT_ROOT = "../resources/container-nondefault-port-project";

    /** The non-default HTTP port configured via {@code liberty.var.default.http.port}. */
    private static final String EXPECTED_HTTP_PORT = "9090";

    /** The non-default HTTPS port configured via {@code liberty.var.default.https.port}. */
    private static final String EXPECTED_HTTPS_PORT = "9453";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        setUpBeforeClass(null, PROJECT_ROOT, true, false, null, null);
        startProcess("-Dcontainer -Dliberty.dev.podman=true -DcontainerBuildTimeout=599", true);
    }

    @AfterClass
    public static void cleanUpAfterClass() throws Exception {
        BaseDevTest.cleanUpAfterClass();
    }

    /**
     * Verifies that the container run command logged by the plugin publishes the
     * non-default HTTP port ({@code :9090}) and not the hardcoded default ({@code :9080}).
     *
     * <p>The log line looks like:
     * {@code Container command: podman run --rm -p 54321:9090 -p 54322:9453 ...}
     *
     * <p>Before the fix the mapping was always {@code :9080} regardless of configuration.
     */
    @Test
    public void containerRunCommandUsesConfiguredHttpPort() throws Exception {
        assertTrue("Dev mode must have started before inspecting the container command: " + getLogTail(),
                verifyLogMessageExists("Liberty is running in dev mode.", 120000));

        assertTrue("Container run command must publish the configured HTTP port :"
                        + EXPECTED_HTTP_PORT + " (not the hardcoded default :9080): " + getLogTail(),
                verifyLogMessageExists(":" + EXPECTED_HTTP_PORT, 5000));

        assertFalse("Container run command must NOT use the hardcoded default HTTP port :9080 "
                        + "when a non-default port is configured: " + getLogTail(),
                readFile("-p 9080:9080", logFile));
    }

    /**
     * Verifies that the container run command publishes the non-default HTTPS port.
     */
    @Test
    public void containerRunCommandUsesConfiguredHttpsPort() throws Exception {
        assertTrue("Dev mode must have started before inspecting the container command: " + getLogTail(),
                verifyLogMessageExists("Liberty is running in dev mode.", 120000));

        assertTrue("Container run command must publish the configured HTTPS port :"
                        + EXPECTED_HTTPS_PORT + " (not the hardcoded default :9443): " + getLogTail(),
                verifyLogMessageExists(":" + EXPECTED_HTTPS_PORT, 5000));

        assertFalse("Container run command must NOT use the hardcoded default HTTPS port :9443 "
                        + "when a non-default port is configured: " + getLogTail(),
                readFile("-p 9443:9443", logFile));
    }

    /**
     * Verifies that the "Unable to retrieve locally mapped port" error no longer appears.
     * This message was the user-visible symptom of the bug: the port was published on 9080
     * but Liberty started on 9090, so the port query always failed.
     */
    @Test
    public void noUnretrievablePortError() throws Exception {
        assertTrue("Dev mode must have started before checking for port error: " + getLogTail(),
                verifyLogMessageExists("Liberty is running in dev mode.", 120000));

        assertFalse("'Unable to retrieve locally mapped port' must not appear when the "
                        + "correct port is published: " + getLogTail(),
                readFile("Unable to retrieve locally mapped port", logFile));
    }
}
