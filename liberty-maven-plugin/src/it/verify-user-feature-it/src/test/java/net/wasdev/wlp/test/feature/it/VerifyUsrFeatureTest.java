/*******************************************************************************
 * (c) Copyright IBM Corporation 2021.
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

import static junit.framework.Assert.*;
import org.junit.Test;
import org.junit.After;
import org.junit.Assume;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import java.io.FilenameFilter;

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
    public void testInvalidVerifyUsrFeature() throws Exception {
    	Assume.assumeTrue(System.getProperty("verify").equals("all"));
    	Assume.assumeTrue(System.getProperty("keyid").equals("0xWRONGKEYID"));
    	try {
    		File featureFile = new File("target/liberty/wlp/usr/extension/lib/features/test.user.test.osgi.SimpleActivator.mf");
            assert !featureFile.exists() : "SimpleActivator.mf should not be generated";
            
        } catch (Exception e) {
            throw new AssertionError ("Fail to install user feature.", e);
        }
    }
    
    


}
