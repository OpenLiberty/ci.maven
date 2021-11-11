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

// Check that the liberty runtime groupId, artifactId, and version were successfully overridden by the 
// liberty.runtime.groupId, liberty.runtime.artifactId, and liberty.runtime.version properties in the pom.xml
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
        assertEquals("The com.ibm.websphere.productVersion property has an unexpected value.","19.0.0.7",version);
    }

    @Test
    public void buildLogCheck() throws Exception {
        File buildLog = new File("../build.log");
        assertTrue(buildLog.exists());

        InputStream buildOutput = null;
        InputStreamReader in = null;
        Scanner s = null;

        final String GROUPID_MESSAGE = "[INFO] The runtimeArtifact groupId io.openliberty is overwritten by the liberty.runtime.groupId value io.openliberty.";
        boolean GROUPID_MESSAGE_FOUND = false;

        final String ARTIFACTID_MESSAGE = "[INFO] The runtimeArtifact artifactId openliberty-webProfile8 is overwritten by the liberty.runtime.artifactId value openliberty-runtime.";
        boolean ARTIFACTID_MESSAGE_FOUND = false;

        final String VERSION_MESSAGE = "[INFO] The runtimeArtifact version 19.0.0.6 is overwritten by the liberty.runtime.version value 19.0.0.7.";
        boolean VERSION_MESSAGE_FOUND = false;

        try {
            buildOutput = new FileInputStream(buildLog);
            in = new InputStreamReader(buildOutput);
            s = new Scanner(in);

            while (s.hasNextLine()) {
                String line = s.nextLine();
                if (line.equals(GROUPID_MESSAGE)) {
                    GROUPID_MESSAGE_FOUND = true;
                } else if (line.equals(ARTIFACTID_MESSAGE)) {
                    ARTIFACTID_MESSAGE_FOUND = true;
                } else if (line.equals(VERSION_MESSAGE)) {
                    VERSION_MESSAGE_FOUND = true;
                }
            }
        } catch (Exception e) {

        }

        assertTrue(GROUPID_MESSAGE_FOUND && ARTIFACTID_MESSAGE_FOUND && VERSION_MESSAGE_FOUND);
    }
    
}
