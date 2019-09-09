package net.wasdev.wlp.maven.test.app;

import java.io.File;

import org.junit.Test;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

import static junit.framework.Assert.*;

// Check that the liberty runtime version was successfully overridden by the 
// -Dliberty.runtime.version property
public class LibertyRuntimeVersionIT {
    
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
    
}
