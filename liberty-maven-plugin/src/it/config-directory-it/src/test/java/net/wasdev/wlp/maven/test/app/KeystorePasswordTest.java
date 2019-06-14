package net.wasdev.wlp.maven.test.app;

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
        
        // Verify that the server.env file does not contain a keystore_password entry

        FileInputStream input = new FileInputStream(in);
        
        Properties prop = new Properties();
        // InputStream stream = getClass().getResourceAsStream( "liberty/usr/servers/test/server.env" );
        prop.load( input );
        String value = prop.getProperty("keystore_password");
        assertTrue("keystore_password property unexpectedly found", value == null);
        
    }
    

}
