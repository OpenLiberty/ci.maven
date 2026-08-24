package net.wasdev.wlp.maven.test.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.junit.Assert;
import org.junit.Test;

/**
 * Verifies that the Liberty Maven Plugin honours a toolchain selected via
 * maven-toolchains-plugin auto-discovery (select-jdk-toolchain goal) when no
 * toolchains.xml is present.
 *
 * Before the fix, initToolchain() only called getToolchains() which reads
 * toolchains.xml directly, bypassing the build context.  When auto-discovery
 * was used, no toolchains.xml existed, getToolchains() returned an empty list,
 * and the plugin emitted CWWKM4100W instead of honouring the toolchain.
 *
 * After the fix, initToolchain() first calls getToolchainFromBuildContext() so
 * that the JDK registered by select-jdk-toolchain is picked up.
 */
public class ToolchainAutodiscoveryTest {

    // CWWKM4100I: toolchain initialised via maven-toolchains-plugin auto-discovery (build context)
    static final String TOOLCHAIN_INITIALIZED_CONTEXT =
            "CWWKM4100I: Using toolchain JDK from build context (auto-discovery)";

    // CWWKM4101I: liberty goal applied the toolchain JDK (success path)
    static final String TOOLCHAIN_CONFIGURED_FOR_GOAL =
            "CWWKM4101I: The %s goal is using the configured toolchain JDK located at";

    // CWWKM4100W: toolchain not found — must NOT appear when auto-discovery is used
    static final String TOOLCHAIN_NOT_FOUND =
            "CWWKM4100W: Toolchain configured for liberty server but no matching JDK was found via build context or toolchains.xml.";

    @Test
    public void testToolchainInitializedFromBuildContext() throws Exception {
        File buildLog = new File("../build.log");
        Assert.assertTrue("build.log does not exist", buildLog.exists());

        Assert.assertTrue(
                "Expected CWWKM4100I (toolchain from build context / auto-discovery) in build.log — " +
                "indicates select-jdk-toolchain auto-discovery result was NOT honoured",
                logContainsMessage(buildLog, TOOLCHAIN_INITIALIZED_CONTEXT));

        Assert.assertFalse(
                "Expected CWWKM4100W to be absent — it means the toolchain was not found, " +
                "which indicates the auto-discovery build context was still being ignored",
                logContainsMessage(buildLog, TOOLCHAIN_NOT_FOUND));
    }

    @Test
    public void testToolchainAppliedToCreateGoal() throws Exception {
        File buildLog = new File("../build.log");
        Assert.assertTrue("build.log does not exist", buildLog.exists());

        Assert.assertTrue(
                "Expected CWWKM4101I for the 'create' goal in build.log",
                logContainsMessage(buildLog, String.format(TOOLCHAIN_CONFIGURED_FOR_GOAL, "create")));
    }

    @Test
    public void testToolchainAppliedToStartGoal() throws Exception {
        File buildLog = new File("../build.log");
        Assert.assertTrue("build.log does not exist", buildLog.exists());

        Assert.assertTrue(
                "Expected CWWKM4101I for the 'start' goal in build.log",
                logContainsMessage(buildLog, String.format(TOOLCHAIN_CONFIGURED_FOR_GOAL, "start")));
    }

    @Test
    public void testServerStarted() throws Exception {
        File messagesLog = new File("liberty/usr/servers/test/logs/messages.log");
        Assert.assertTrue(
                "messages.log does not exist — Liberty server did not start",
                messagesLog.exists());
        Assert.assertTrue(
                "Liberty server ready message not found in messages.log",
                logContainsMessage(messagesLog, "CWWKF0011I"));
    }

    private boolean logContainsMessage(File logFile, String message) throws FileNotFoundException {
        Assert.assertTrue("Log file not found: " + logFile.getAbsolutePath(), logFile.exists());
        try (Scanner scanner = new Scanner(logFile)) {
            while (scanner.hasNextLine()) {
                if (scanner.nextLine().contains(message)) {
                    return true;
                }
            }
        }
        return false;
    }
}
