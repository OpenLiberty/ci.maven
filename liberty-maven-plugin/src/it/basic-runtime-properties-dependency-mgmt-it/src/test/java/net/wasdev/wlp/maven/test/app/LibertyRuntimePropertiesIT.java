package net.wasdev.wlp.maven.test.app;

import java.io.File;

import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.Scanner;

import static junit.framework.Assert.*;

// Check that the liberty runtime groupId, artifactId, and version were successfully used from dependency management.
public class LibertyRuntimePropertiesIT {
    
    @Test
    public void testLibertyVersionInstalled() throws Exception {
        File f = new File("./liberty/wlp/lib/versions/openliberty.properties");
        assertTrue(f.getCanonicalFile() + " doesn't exist.", f.exists());

        FileInputStream input = new FileInputStream(f);
        Properties libertyProductProperties = new Properties();

        libertyProductProperties.load(input);
        String version = libertyProductProperties.getProperty("com.ibm.websphere.productVersion");
        assertNotNull("The com.ibm.websphere.productVersion property does not exist.",version);
        assertEquals("The com.ibm.websphere.productVersion property has an unexpected value.","21.0.0.12",version);
    }

    @Test
    public void buildLogCheck() throws Exception {
        File buildLog = new File("../build.log");
        assertTrue(buildLog.exists());

        InputStream buildOutput = null;
        InputStreamReader in = null;
        Scanner s = null;

        final String MESSAGE = "[INFO] CWWKM2102I: Using artifact based assembly archive : io.openliberty:openliberty-webProfile8:null:21.0.0.12:zip.";


        boolean MESSAGE_FOUND = false;

        try {
            buildOutput = new FileInputStream(buildLog);
            in = new InputStreamReader(buildOutput);
            s = new Scanner(in);

            while (s.hasNextLine()) {
                String line = s.nextLine();
                if (line.equals(MESSAGE)) {
                    MESSAGE_FOUND = true;
                    break;
                }
            }
        } catch (Exception e) {

        }

        assertTrue("Expected message not found: "+MESSAGE, MESSAGE_FOUND);
    }
    
}
