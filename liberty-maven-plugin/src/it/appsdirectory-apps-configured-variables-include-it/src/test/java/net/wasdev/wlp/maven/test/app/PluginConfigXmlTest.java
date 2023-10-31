package net.wasdev.wlp.maven.test.app;

import java.io.File;
import java.io.FileNotFoundException;

import java.util.Scanner;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

/**
 * 
 * Web application test case
 * 
 */

public class PluginConfigXmlTest {

    public final String CONFIG_DROPINS_XML="liberty/usr/servers/test/configDropins/defaults/install_apps_configuration_1491924271.xml";
    public final String APP_WAR="liberty/usr/servers/test/apps/appsdirectory-apps-configured-variables-include-it.war";

    public final String LOG_LOCATION = "liberty/usr/servers/test/logs/messages.log";
    public final String INCLUDE_REGEX_MESSAGE = ".* CWWKG0028A: Processing included configuration resource: .*/|\\\\target/|\\\\liberty/|\\\\usr/|\\\\shared/|\\\\config/|\\\\environment\\.xml";
    public final String APP_STARTED_MESSAGE = ".* CWWKZ0001I: Application appsdirectory-apps-configured-variables-include-it started.*";

    @Test
    public void testMessagesLogFileExist() throws Exception {
        File f = new File(LOG_LOCATION);
        assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
    }
    
    @Test
    public void testApplicationConfiguredInApps() throws Exception {
        File in = new File(CONFIG_DROPINS_XML);
        assertFalse("config dropins xml file exists when it should not", in.exists());

        File warFile = new File(APP_WAR);
        assertTrue("war file is not in correct location in apps folder", warFile.exists());
    }

    @Test
    public void checkMessagesLogFor() throws Exception {
        assertTrue("Did not find include file processed message in messages.log", logContainsMessage(INCLUDE_REGEX_MESSAGE));
        assertTrue("Did not find app started message in messages.log", logContainsMessage(APP_STARTED_MESSAGE));
    }

    public boolean logContainsMessage(String regex) throws FileNotFoundException {
        File logFile = new File(LOG_LOCATION);
        assertTrue("Log file not found at location: "+LOG_LOCATION, logFile.exists());
        boolean found = false;
        Pattern pattern = Pattern.compile(regex);

        try (Scanner scanner = new Scanner(logFile);) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (pattern.matcher(line).find()) {
                    found = true;
                }
            }
        }
                
        return found;
    }
}
