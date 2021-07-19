/*******************************************************************************
 * (c) Copyright IBM Corporation 2018.
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
import org.junit.AfterClass;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;

public class PrepareFeatureTest extends BaseInstallFeature {
	
	static File resourceDir = new File("../resources");
	static File mavenLocalRepo = new File(System.getProperty("user.home")+ "/.m2/repository");
	static File userTestRepo = new File(mavenLocalRepo, "test/user/test/features");
	static File resourceBom = new File(resourceDir, "features-bom-19.0.0.8.pom");
	static File resourceEsa = new File(resourceDir, "testesa1-19.0.0.8.esa");
	static File featuresBom = new File(userTestRepo, "features-bom/19.0.0.8/features-bom-19.0.0.8.pom");
	static File testEsa = new File(userTestRepo, "testesa1/19.0.0.8/testesa1-19.0.0.8.esa");
	
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
			File jsonFile = new File(userTestRepo, "features/19.0.0.8/features-19.0.0.8.json");

            assert jsonFile.exists() : "features.json cannot be generated";
            
        } catch (Exception e) {
            throw new AssertionError ("Fail on task prepareFeature. "+e);
        }
    }
    
    @AfterClass
	public static void cleanUp() {
		deleteFolder(userTestRepo);
	}

}
