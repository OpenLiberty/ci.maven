package net.wasdev.wlp.maven.test.app;


import java.io.File;
import java.io.FileNotFoundException;

import java.util.Scanner;

import org.junit.Test;

import org.junit.Assert;

/**
 * 
 * Web application test case
 * 
 */

public class ToolchainTest {

    public final String CONFIG_XML = "liberty-plugin-config.xml";
    public final String LOG_LOCATION = "liberty/usr/servers/test/logs/messages.log";
    static final String TOOLCHAIN_NOT_HONORED_WARNING = "CWWKM4101W: The toolchain JDK configuration for goal %s is not honored because the JAVA_HOME property is specified in the server.env or jvm.options file.";


    @Test
    public void testConfigPropFileExist() throws Exception {
        File f = new File(CONFIG_XML);
        Assert.assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
    }

    
    @Test
    public void testApplicationFileExist() throws Exception {
        File f = new File("liberty/usr/servers/test/apps/toolchain-java-warning-server-env-custom-file-it.war");
        Assert.assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
    }

    @Test
    public void testToolchainLogExists() throws Exception {
        File f = new File(LOG_LOCATION);
        Assert.assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
        File buildLog = new File("../build.log");
        Assert.assertTrue(buildLog.exists());

        Assert.assertTrue("Did not find project properties contain java.home message for create goal in build.log", logContainsMessage(buildLog, String.format(TOOLCHAIN_NOT_HONORED_WARNING, "create")));
        Assert.assertTrue("Did not find project properties contain java.home message for start goal in build.log", logContainsMessage(buildLog, String.format(TOOLCHAIN_NOT_HONORED_WARNING, "start")));
        Assert.assertTrue("Did not find project properties contain java.home message for status goal in build.log", logContainsMessage(buildLog, String.format(TOOLCHAIN_NOT_HONORED_WARNING, "status")));
    }

    @Test
    public void verifyLogMessageForExpansionVariables() throws Exception {
        File buildLog = new File("../build.log");
        Assert.assertTrue(buildLog.exists());
        String os = System.getProperty("os.name");
        if (os != null && os.toLowerCase().startsWith("windows")) {
            Assert.assertTrue("Did not find variable expansion message in build.log", logContainsMessage(buildLog, "Resolving Property EXP_VAR for expression !EXP_VAR!_!EXP_VAR3!. Resolved expression value is TEST"));
            Assert.assertTrue("Did not find second variable expansion message in build.log", logContainsMessage(buildLog, "Resolving Property EXP_VAR3 for expression !EXP_VAR!_!EXP_VAR3!. Resolved expression value is TEST_WINDOWS"));
        }else {
            Assert.assertTrue("Did not find variable expansion message in build.log", logContainsMessage(buildLog, "Resolving Property EXP_VAR for expression ${EXP_VAR}_${EXP_VAR2}. Resolved expression value is TEST"));
            Assert.assertTrue("Did not find second variable expansion message in build.log", logContainsMessage(buildLog, "Resolving Property EXP_VAR2 for expression ${EXP_VAR}_${EXP_VAR2}. Resolved expression value is TEST_UNIX"));
        }

    }

    private boolean logContainsMessage( File logFile, String message) throws FileNotFoundException {

        Assert.assertTrue("Log file not found at location: "+ LOG_LOCATION, logFile.exists());
        boolean found = false;

        try (Scanner scanner = new Scanner(logFile);) {
            while (scanner.hasNextLine()) {
                if(scanner.nextLine().contains(message)) {
                    found = true;
                }
            }
        }

        return found;
    }
}
