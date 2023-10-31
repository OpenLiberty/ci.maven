package net.wasdev.wlp.maven.test.app;

import java.io.File;

import org.junit.Test;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

import static org.junit.Assert.*;

// Check that maven properties with format `liberty.bootstrap.{var}` end up in the bootstrap.properties file
// with `var` as the property name and the value copied over. Also checks that the maven property does not override
// a specific <bootstrapProperties> element in the pom.xml.
public class BootstrapPropertiesFileTest {
    
    @Test
    public void testBootstrapPropertiesFileContents() throws Exception {
        File f = new File("liberty/usr/servers/test/bootstrap.properties");
        assertTrue(f.getCanonicalFile() + " doesn't exist.", f.exists());

        try (FileInputStream input = new FileInputStream(f)) {        
            Properties bootstrapProps = new Properties();

            bootstrapProps.load(input);

            String type = bootstrapProps.getProperty("type");
            assertNotNull("The type bootstrap property does not exist.",type);
            assertEquals("The type bootstrap property has an unexpected value.","war",type);

            String test = bootstrapProps.getProperty("test");
            assertNotNull("The type bootstrap property does not exist.",test);
            assertEquals("The test bootstrap property has an unexpected value.","mavenPropertyCopiedToBootstrapPropertiesFile",test);
        }
    }
    
}
