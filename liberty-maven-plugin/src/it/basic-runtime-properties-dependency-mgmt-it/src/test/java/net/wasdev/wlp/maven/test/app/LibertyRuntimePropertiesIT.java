package net.wasdev.wlp.maven.test.app;

import java.io.File;

import org.junit.Test;

import java.io.FileInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Properties;
import java.util.Scanner;

import static org.junit.Assert.*;

// Check that the liberty runtime groupId, artifactId, and version were successfully used from dependency management.
public class LibertyRuntimePropertiesIT {
    public static final String LOG_LOCATION = "../build.log";
    
    @Test
    public void testLibertyVersionInstalled() throws Exception {
        File f = new File("./liberty/wlp/lib/versions/openliberty.properties");
        assertTrue(f.getCanonicalFile() + " doesn't exist.", f.exists());

        try (FileInputStream input = new FileInputStream(f);) {
            Properties libertyProductProperties = new Properties();

            libertyProductProperties.load(input);
            String version = libertyProductProperties.getProperty("com.ibm.websphere.productVersion");
            assertNotNull("The com.ibm.websphere.productVersion property does not exist.",version);
            assertEquals("The com.ibm.websphere.productVersion property has an unexpected value.","21.0.0.12",version);
        }
    }

    @Test
    public void buildLogCheck() throws Exception {
        final String MESSAGE = "[INFO] CWWKM2102I: Using artifact based assembly archive : io.openliberty:openliberty-webProfile8:null:21.0.0.12:zip.";
        assertTrue("Expected message not found: "+MESSAGE, logContainsMessage(MESSAGE));
    }
    
    public boolean logContainsMessage(String message) throws FileNotFoundException {
        File logFile = new File(LOG_LOCATION);
        assertTrue("Log file not found at location: "+LOG_LOCATION, logFile.exists());
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
