/*******************************************************************************
 * (c) Copyright IBM Corporation 2019.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
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
