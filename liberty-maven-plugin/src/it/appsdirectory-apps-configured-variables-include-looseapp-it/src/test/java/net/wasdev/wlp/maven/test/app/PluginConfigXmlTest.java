package net.wasdev.wlp.maven.test.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

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
    public final String APP_WAR="liberty/usr/servers/test/apps/appsdirectory-apps-configured-variables-include-looseapp-it.war";
    public final String APP_WAR_XML= APP_WAR + ".xml";

    public final String MESSAGES_LOG = "liberty/usr/servers/test/logs/messages.log";
    public final String INCLUDE_REGEX_MESSAGE = ".* CWWKG0028A: Processing included configuration resource: .*/|\\\\target/|\\\\liberty/|\\\\usr/|\\\\shared/|\\\\config/|\\\\environment\\.xml";
    public final String APP_STARTED_MESSAGE = ".* CWWKZ0001I: Application appsdirectory-apps-configured-variables-include-looseapp-it started.*";

    @Test
    public void testMessagesLogFileExist() throws Exception {
        File f = new File(MESSAGES_LOG);
        assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
    }
    
    @Test
    public void testLooseApplicationConfiguredInApps() throws Exception {
        File in = new File(CONFIG_DROPINS_XML);
        assertFalse("config dropins xml file exists when it should not", in.exists());

        File warFile = new File(APP_WAR);
        assertFalse("war file exists in apps folder when it should not", warFile.exists());

        File looseAppFile = new File(APP_WAR_XML);
        assertTrue("loose app file is not in correct location in apps folder", looseAppFile.exists());
    }

    @Test
    public void checkMessagesLogFor() throws Exception {
    	File messagesLog = new File(MESSAGES_LOG);
                
        InputStream serverOutput = null;
        InputStreamReader in = null;
        Scanner s = null;

        boolean includeFound = false;
        boolean appStartedFound = false;

        try {
            // Read file and search
            serverOutput = new FileInputStream(messagesLog);
            in = new InputStreamReader(serverOutput);
            s = new Scanner(in);

            String foundString = null;
            Pattern pattern1 = Pattern.compile(INCLUDE_REGEX_MESSAGE);
            Pattern pattern2 = Pattern.compile(APP_STARTED_MESSAGE);

            while (s.hasNextLine()) {
                String line = s.nextLine();
                if (pattern1.matcher(line).find()) {
                    includeFound = true;
                } else if (pattern2.matcher(line).find()) {
                    appStartedFound = true;
                }
            }
        } catch (Exception e) {

        }
        s.close(); 
        serverOutput.close();
        in.close();

        assertTrue("Did not find include file processed message in messages.log", includeFound);
        assertTrue("Did not find app started message in messages.log", appStartedFound);

    }
}
