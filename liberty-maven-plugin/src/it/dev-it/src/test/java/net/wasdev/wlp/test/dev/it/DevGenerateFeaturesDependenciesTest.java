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
package net.wasdev.wlp.test.dev.it;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Start dev mode, update umbrella dependencies in the pom.xml, ensure that the correct version
 * of features are generated
 */
public class DevGenerateFeaturesDependenciesTest extends BaseDevTest {

    @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      setUpBeforeClass(null, "../resources/basic-dev-project-umbrella-deps");
   }
 
    @AfterClass
    public static void cleanUpAfterClass() throws Exception {
       BaseDevTest.cleanUpAfterClass();
    }

    @Test
    public void updateDependencyTest() throws Exception {
       assertTrue(verifyLogMessageExists("Liberty is running in dev mode.", 10000));

       File generatedFeaturesFile = getGeneratedFeaturesFile();
       File targetGeneratedFeaturesFile = getTargetGeneratedFeaturesFile();
       assertTrue(pom.exists());
       assertTrue(generatedFeaturesFile.exists());
       assertTrue(targetGeneratedFeaturesFile.exists());
       long lastModified = generatedFeaturesFile.lastModified();
       waitLongEnough();

       // verify mpHealth-2.2 is in generated features file
       assertTrue(verifyLogMessageExists("mpHealth-2.2", 10000, generatedFeaturesFile));
       assertTrue(verifyLogMessageExists("mpHealth-2.2", 10000)); // should appear in the message "CWWKF0012I: The server installed the following features:"

       int generateFeaturesCount = countOccurrences("Running liberty:generate-features", logFile);
       assertTrue(verifyLogMessageExists("Source compilation was successful.", 10000));

       // modify MicroProfile umbrella dependency in pom.xml
       replaceString("<dependency>\n"
             + "        <groupId>org.eclipse.microprofile</groupId>\n"
             + "        <artifactId>microprofile</artifactId>\n"
             + "        <version>3.3</version>\n"
             + "        <type>pom</type>\n"
             + "        <scope>provided</scope>\n",
             "<dependency>\n"
                   + "        <groupId>org.eclipse.microprofile</groupId>\n"
                   + "        <artifactId>microprofile</artifactId>\n"
                   + "        <version>4.1</version>\n"
                   + "        <type>pom</type>\n"
                   + "        <scope>provided</scope>\n",
             pom);

       // Dev mode should now run the generate features mojo
       assertTrue(getLogTail(), verifyLogMessageExists("Generated the following features:", 15000, logFile, ++generateFeaturesCount)); // mojo ran
       assertTrue(generatedFeaturesFile.exists());
       assertTrue(getLogTail(), lastModified < generatedFeaturesFile.lastModified());
       assertTrue(targetGeneratedFeaturesFile.exists());

       // verify that mpHealth-3.0 is now in the generated features file
       assertTrue(getLogTail(), verifyLogMessageExists("mpHealth-3.1", 10000, generatedFeaturesFile));
       assertTrue(getLogTail(), verifyLogMessageExists("mpHealth-3.1", 10000)); // should appear in the message "CWWKF0012I: The server installed the following features:"
       
       // verify that mpHealth-2.2 is no longer in the generated features file
       assertFalse(getLogTail(), verifyLogMessageExists("mpHealth-2.2", 10000, generatedFeaturesFile));
    }

}
