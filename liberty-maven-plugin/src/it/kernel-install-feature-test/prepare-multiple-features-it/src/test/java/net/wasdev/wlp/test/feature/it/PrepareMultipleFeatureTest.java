/*******************************************************************************
 * (c) Copyright IBM Corporation 2022.
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
import org.junit.AfterClass;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import java.io.FilenameFilter;

public class PrepareMultipleFeatureTest {

	static File mavenLocalRepo = new File(System.getProperty("user.home")+ "/.m2/repository");
	static File userTestRepo = new File(mavenLocalRepo, "test/user/test/osgi");
	
	public static boolean deleteFolder(final File directory) {
		if (directory.isDirectory()) {
				File[] files = directory.listFiles();
				if (null != files) {
					for (File file : files) {
						if (file.isDirectory()) {
							deleteFolder(file);
						} else {
							if (!file.delete()) {
									file.deleteOnExit();
							}
						}
					}
				}
		}
		if(!directory.delete()){
				directory.deleteOnExit();
				return false;
		}
		return true;
	}
	

    @Test
    public void testPrepareFeature() throws Exception {
    	try {
			File jsonFile = new File(userTestRepo, "features/1.0/features-1.0.json");

            assert jsonFile.exists() : "features.json cannot be generated";
            
        } catch (Exception e) {
            throw new AssertionError ("Fail on task prepareFeature.", e);
        }
    }
    
    @Test
    public void testMultipleUsrFeatureInstall() throws Exception {
    	try {
			File helloFile = new File("liberty/wlp/usr/extension/lib/features/com.ibm.ws.install.helloWorld1.mf");
			File simpleFile = new File("liberty/wlp/usr/extension/lib/features/test.user.test.osgi.SimpleActivator.mf");

            assert helloFile.exists() : "helloWorld1.mf cannot be generated";
            assert simpleFile.exists() : "SimpleActivator.mf cannot be generated";
            
        } catch (Exception e) {
            throw new AssertionError ("Fail to install user features.", e);
        }
    }
    
    @AfterClass
	public static void cleanUp() {
		deleteFolder(userTestRepo);
	}

}
