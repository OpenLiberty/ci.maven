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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.openliberty.tools.maven.server.GenerateFeaturesMojo;

/**
 * liberty:generate-features goal tests
 */
public class GenerateFeaturesTest extends BaseGenerateFeaturesTest {

    @Before
    public void setUp() throws Exception {
        setUpBeforeTest("../resources/basic-dev-project");
    }

    @After
    public void cleanUp() throws Exception {
        cleanUpAfterTest();
    }

    @Test
    public void basicTest() throws Exception {
        runProcess("compile liberty:generate-features");
        // verify that the target directory was created
        targetDir = new File(tempProj, "target");
        assertTrue(targetDir.exists());

        // verify that the generated features file was created
        assertTrue(newFeatureFile.exists());

        // verify that the correct features are in the generated-features.xml
        List<String> features = readFeatures(newFeatureFile);
        assertEquals(2, features.size());
        List<String> expectedFeatures = Arrays.asList("servlet-4.0", "jaxrs-2.1");
        assertEquals(expectedFeatures, features);
    }

    @Test
    public void noClassFiles() throws Exception {
        // do not compile before running generate-features
        runProcess("liberty:generate-features");

        // verify that generated features file was not created
        assertFalse(newFeatureFile.exists());

        // verify class files not found warning message
        assertTrue(processOutput.contains(GenerateFeaturesMojo.NO_CLASSES_DIR_WARNING));
    }

    @Test
    public void customFeaturesTest() throws Exception {
        // complete the setup of the test
        File serverXmlFile = new File(tempProj, "src/main/liberty/config/server.xml");
        replaceString("<!--replaceable-->",
        "<featureManager>\n" +
        "  <feature>servlet-4.0</feature>\n" +
        "  <feature>usr:custom-1.0</feature>\n" +
        "</featureManager>\n", serverXmlFile);
        assertFalse("Before running", newFeatureFile.exists());
        // run the test
        runProcess("compile liberty:generate-features");

        // verify that the generated features file was created
        assertTrue(newFeatureFile.exists());

        // verify that the correct feature is in the generated-features.xml
        List<String> features = readFeatures(newFeatureFile);
        assertEquals(processOutput, 1, features.size());
        List<String> expectedFeatures = Arrays.asList("jaxrs-2.1");
        assertEquals(expectedFeatures, features);
    }

    @Test
    public void serverXmlCommentTest() throws Exception {
        // initially the expected comment is not found in server.xml
        File serverXmlFile = new File(tempProj, "src/main/liberty/config/server.xml");
        assertFalse(verifyLogMessageExists(GenerateFeaturesMojo.FEATURES_FILE_MESSAGE, 10, serverXmlFile));
        // also we wish to test behaviour when there is no <featureManager> element so test that
        assertFalse(verifyLogMessageExists("<featureManager>", 10, serverXmlFile));

        runProcess("compile liberty:generate-features");

        // verify that generated features file was created
        assertTrue(newFeatureFile.exists());

        // verify expected comment found in server.xml
        assertTrue(verifyLogMessageExists(GenerateFeaturesMojo.FEATURES_FILE_MESSAGE, 100, serverXmlFile));
    }
}
