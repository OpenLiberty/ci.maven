package net.wasdev.wlp.test.servlet.it;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * 
 * KeystorePasswordTest test case
 * 
 */

public class KeystorePasswordTest {

    @Test
    public void testServerEnvFileContents() throws Exception {
        File in = new File("liberty/usr/servers/test", "server.env");
        assertTrue(in.getCanonicalFile() + " doesn't exist", in.exists());
        
        // Verify that the server.env file does contain a keystore_password entry
        FileInputStream input = new FileInputStream(in);
   
        Properties prop = new Properties();
        prop.load( input );
        String value = prop.getProperty("keystore_password");
        assertTrue("Expected keystore_password property not found", value != null);
        
    }
    

}
