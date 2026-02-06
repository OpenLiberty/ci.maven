package net.wasdev.wlp.maven.test.app;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import java.util.Scanner;

import org.junit.Test;

import org.junit.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * 
 * Web application test case
 * 
 */

public class ToolchainTest {

    public final String CONFIG_XML = "liberty-plugin-config.xml";
    public final String LOG_LOCATION = "liberty/usr/servers/test/logs/messages.log";
    static final String TOOLCHAIN_CONFIGURED_FOR_GOAL = "CWWKM4101I: The %s goal is using the configured toolchain JDK located at";


    @Test
    public void testConfigPropFileExist() throws Exception {
        File f = new File(CONFIG_XML);
        Assert.assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
    }

    
    @Test
    public void testApplicationFileExist() throws Exception {
        File f = new File("liberty/usr/servers/test/apps/toolchain-start-status-it.war");
        Assert.assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());

        f = new File(LOG_LOCATION);
        Assert.assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
        // should contain java.version = 11 since <jdkToolChain> is defined as Java 11
        Assert.assertTrue("Did not find toolchain version in messages.log", logContainsMessage(f, "java.version = 11"));
        File buildLog = new File("../build.log");
        Assert.assertTrue(buildLog.exists());

        Assert.assertTrue("Did not find toolchain honored message for create goal in build.log", logContainsMessage(buildLog, String.format(TOOLCHAIN_CONFIGURED_FOR_GOAL, "create")));
        Assert.assertTrue("Did not find toolchain honored message for start goal in build.log", logContainsMessage(buildLog, String.format(TOOLCHAIN_CONFIGURED_FOR_GOAL, "start")));
        Assert.assertTrue("Did not find toolchain honored message for status goal in build.log", logContainsMessage(buildLog, String.format(TOOLCHAIN_CONFIGURED_FOR_GOAL, "status")));
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
