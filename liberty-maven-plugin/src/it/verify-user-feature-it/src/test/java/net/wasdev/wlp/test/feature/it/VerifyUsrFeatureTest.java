/*******************************************************************************
 * (c) Copyright IBM Corporation 2023.
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
package net.wasdev.wlp.test.feature.it;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.After;
import org.junit.Assume;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.io.FileInputStream;

public class VerifyUsrFeatureTest {
	
	static File mavenLocalRepo = new File(System.getProperty("user.home")+ "/.m2/repository");
	static File userTestRepo = new File(mavenLocalRepo, "test/user/test/features");
	
	Logger logger = Logger.getLogger(VerifyUsrFeatureTest.class.getName());
	
    
    @Test
    public void testVerifyUsrFeature() throws Exception {
    	try {
    		File featureFile = new File("target/liberty/wlp/usr/extension/lib/features/test.user.test.osgi.SimpleActivator.mf");

            assert featureFile.exists() : "SimpleActivator.mf cannot be generated";
            
        } catch (Exception e) {
            throw new AssertionError ("Fail to install user feature.", e);
        }
    }
    
    @Test
    public void testVerifyAll() throws Exception {
    	Assume.assumeTrue(System.getProperty("verify").equals("all") && System.getProperty("keyid").equals("0x05534365803788CE"));
    	String VERIFIED_MESSAGE = "All features were successfully verified.";
    	assertTrue(buildLogCheck(VERIFIED_MESSAGE));
    }
    
    @Test
    public void testVerifyWarnWrongKeyId() throws Exception {
    	Assume.assumeTrue(System.getProperty("verify").equals("warn") && System.getProperty("keyid").equals("0xWRONGKEYID"));
    	//CWWKF1514E: The 0X05534365803788CE public key ID does not match the 0xWRONGKEYID provided key ID.
    	String CWWKF1514E_MESSAGE = "CWWKF1514E";
    	assertTrue(buildLogCheck(CWWKF1514E_MESSAGE));
    }
    
    @Test
    public void testVerifyEnforceWithoutUserKeyId() throws Exception {
    	Assume.assumeTrue(System.getProperty("verify").equals("enforce") && System.getProperty("keyid") == null);
    	//CWWKF1508E: The public key ID for the src/test/resources/SimpleActivatorValidKey.asc key URL was not provided.
    	String CWWKF1508E_MESSAGE = "CWWKF1508E";
    	assertTrue(buildLogCheck(CWWKF1508E_MESSAGE));
    }
    

    
    public boolean buildLogCheck(String msg) throws Exception {
        File buildLog = new File("build.log");
        assertTrue(buildLog.exists());        

        try (InputStream buildOutput = new FileInputStream(buildLog); InputStreamReader in = new InputStreamReader(buildOutput); Scanner s = new Scanner(in);) {
            while (s.hasNextLine()) {
                String line = s.nextLine();
                if(line.contains(msg)) {
                	return true;
                }
            }
        } catch (Exception e) {
        	System.out.println("Error checking build.log " + e.getMessage());
        }
        
        return false;
    }

}
