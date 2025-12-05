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
    static final String INVALID_TOOLCHAIN_CONFIGURED = "CWWKM4100W: Toolchain requested but not available for JDK requirement";

    @Test
    public void testConfigPropFileExist() throws Exception {
        File f = new File(CONFIG_XML);
        Assert.assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
    }

    
    @Test
    public void testApplicationFileExist() throws Exception {
        File f = new File("liberty/usr/servers/test/apps/toolchain-invalid-jdk-it.war");
        Assert.assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());

        f = new File(LOG_LOCATION);
        Assert.assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
        File buildLog = new File("../build.log");
        Assert.assertTrue(buildLog.exists());

        Assert.assertTrue("Did not find toolchain not honored warning message in build.log", logContainsMessage(buildLog, INVALID_TOOLCHAIN_CONFIGURED));
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
