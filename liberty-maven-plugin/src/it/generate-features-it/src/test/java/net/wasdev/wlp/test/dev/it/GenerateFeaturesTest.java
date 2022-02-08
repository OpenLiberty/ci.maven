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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.openliberty.tools.maven.server.GenerateFeaturesMojo;

/**
 * liberty:generate-features goal tests
 */
public class GenerateFeaturesTest extends BaseGenerateFeaturesTest {

    static File newFeatureFile;
    static File serverXmlFile;
    static File pom;
    static File targetDir;

    @Before
    public void setUp() throws Exception {
        setUpBeforeTest("../resources/basic-dev-project");
        pom = new File(tempProj, "pom.xml");
        assertTrue(pom.exists());
        replaceVersion(tempProj);

        newFeatureFile = new File(tempProj, GENERATED_FEATURES_FILE_PATH);
        serverXmlFile = new File(tempProj, "src/main/liberty/config/server.xml");
        targetDir = new File(tempProj, "target");
    }

    @After
    public void cleanUp() throws Exception {
        cleanUpAfterTest();
    }

    @Test
    public void basicTest() throws Exception {
        runCompileGoal();
        runGenerateFeaturesGoal();
        // verify that the target directory was created
        assertTrue(targetDir.exists());

        // verify that the generated features file was created
        assertTrue(newFeatureFile.exists());

        // verify that the correct features are in the generated-features.xml
        Set<String> features = readFeatures(newFeatureFile);
        Set<String> expectedFeatures = getExpectedGeneratedFeaturesSet();
        assertEquals(expectedFeatures.size(), features.size());
        assertEquals(expectedFeatures, features);

        // place generated features in server.xml
        replaceString("<!--replaceable-->",
                "<featureManager>\n" +
                        getExpectedFeatureElementString() +
                        "</featureManager>\n",
                serverXmlFile);

        runGenerateFeaturesGoal();
        // no additional features should be generated
        assertTrue(newFeatureFile.exists());
        features = readFeatures(newFeatureFile);
        assertEquals(0, features.size());
    }

    @Test
    public void noClassFiles() throws Exception {
        // do not compile before running generate-features
        runGenerateFeaturesGoal();

        // verify that generated features file was not created
        assertFalse(newFeatureFile.exists());

        // verify class files not found warning message
        assertTrue(processOutput.contains(GenerateFeaturesMojo.NO_CLASSES_DIR_WARNING));
    }

    @Test
    public void customFeaturesTest() throws Exception {
        // complete the setup of the test
        replaceString("<!--replaceable-->",
            "<featureManager>\n" +
            "  <feature>usr:custom-1.0</feature>\n" +
            "</featureManager>\n", serverXmlFile);
        assertFalse("Before running", newFeatureFile.exists());
        // run the test
        runCompileGoal();
        runGenerateFeaturesGoal();

        // verify that the generated features file was created
        assertTrue(newFeatureFile.exists());

        // verify that the correct feature is in the generated-features.xml
        Set<String> features = readFeatures(newFeatureFile);
        Set<String> expectedFeatures = getExpectedGeneratedFeaturesSet();
        assertEquals(processOutput, expectedFeatures.size(), features.size());
        assertEquals(expectedFeatures, features);
    }

    @Test
    public void serverXmlCommentNoFMTest() throws Exception {
        // initially the expected comment is not found in server.xml
        assertFalse(verifyLogMessageExists(GenerateFeaturesMojo.FEATURES_FILE_MESSAGE, 10, serverXmlFile));
        // also we wish to test behaviour when there is no <featureManager> element so test that
        assertFalse(verifyLogMessageExists("<featureManager>", 10, serverXmlFile));

        runCompileGoal();
        runGenerateFeaturesGoal();

        // verify that generated features file was created
        assertTrue(newFeatureFile.exists());

        // verify expected comment found in server.xml
        Charset charset = StandardCharsets.UTF_8;
        String serverXmlContents = new String(Files.readAllBytes(serverXmlFile.toPath()), charset);
        serverXmlContents = "\n" + serverXmlContents;
        assertTrue(serverXmlContents,
            verifyLogMessageExists(GenerateFeaturesMojo.FEATURES_FILE_MESSAGE, 100, serverXmlFile));
    }

    @Test
    public void serverXmlCommentFMTest() throws Exception {
        replaceString("<!--replaceable-->",
                "<!--Feature generation comment goes below this line-->\n" +
                        "  <featureManager>\n" +
                        "    <feature>servlet-4.0</feature>\n" +
                        "  </featureManager>\n",
                serverXmlFile);

        // initially the expected comment is not found in server.xml
        assertFalse(verifyLogMessageExists(GenerateFeaturesMojo.FEATURES_FILE_MESSAGE, 10, serverXmlFile));

        runCompileGoal();
        runGenerateFeaturesGoal();

        // verify that generated features file was created
        assertTrue(newFeatureFile.exists());

        // verify expected comment found in server.xml
        Charset charset = StandardCharsets.UTF_8;
        String serverXmlContents = new String(Files.readAllBytes(serverXmlFile.toPath()), charset);
        serverXmlContents = "\n" + serverXmlContents;
        assertTrue(serverXmlContents,
            verifyLogMessageExists(GenerateFeaturesMojo.FEATURES_FILE_MESSAGE, 100, serverXmlFile));
    }

    protected void runGenerateFeaturesGoal() throws IOException, InterruptedException {
        runProcess("liberty:generate-features");
    }

    protected void runCompileGoal() throws IOException, InterruptedException {
        runProcess("compile");
    }

    protected Set<String> getExpectedGeneratedFeaturesSet() {
        return new HashSet<String>(Arrays.asList("servlet-4.0", "jaxrs-2.1"));
    }

    // get the expected features in string format to insert in server.xml featureManager block
    private String getExpectedFeatureElementString() {
        Set<String> expectedFeatures = getExpectedGeneratedFeaturesSet();
        StringBuilder str = new StringBuilder();
        for (String feat : expectedFeatures) {
            str.append("  <feature>"+ feat + "</feature>\n");
        }
        return str.toString();
    }
}
