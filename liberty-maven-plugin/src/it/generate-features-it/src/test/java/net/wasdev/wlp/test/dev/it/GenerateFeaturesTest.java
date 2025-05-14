/*******************************************************************************
 * (c) Copyright IBM Corporation 2022, 2025
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
import static io.openliberty.tools.common.plugins.util.BinaryScannerUtil.*;

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
        runCompileAndGenerateFeatures();
        // verify that the target directory was created
        assertTrue(targetDir.exists());

        // verify that the generated features file was created
        assertTrue(formatOutput(processOutput), newFeatureFile.exists());

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
    public void generateToSrcTest() throws Exception {
        newFeatureFile.delete(); // delete file if it exists but do not assert
        assertFalse(newFeatureFileSrc.exists());
        runCompileAndGenerateFeaturesToSrc();

        // verify that the generated features file was created
        // Assume the contents are correct based on prior testing
        assertTrue(formatOutput(processOutput), newFeatureFileSrc.exists());
        assertTrue(newFeatureFileSrc.delete());
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
        runCompileAndGenerateFeatures();

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

        runCompileAndGenerateFeatures();

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

        runCompileAndGenerateFeatures();

        // verify that generated features file was created
        assertTrue(newFeatureFile.exists());

        // verify expected comment found in server.xml
        Charset charset = StandardCharsets.UTF_8;
        String serverXmlContents = new String(Files.readAllBytes(serverXmlFile.toPath()), charset);
        serverXmlContents = "\n" + serverXmlContents;
        assertTrue(serverXmlContents,
            verifyLogMessageExists(GenerateFeaturesMojo.FEATURES_FILE_MESSAGE, 100, serverXmlFile));
    }

    /**
     * Conflict between user specified features.
     * Check for BINARY_SCANNER_CONFLICT_MESSAGE2 (conflict between configured features)
     * 
     * @throws Exception
     */
    @Test
    public void userConflictTest() throws Exception {
        // place expected generated features in server.xml and conflicting cdi-1.2
        replaceString("<!--replaceable-->",
                "<featureManager>\n" +
                        getExpectedFeatureElementString() +
                        "<feature>cdi-1.2</feature>\n" +
                        "</featureManager>\n",
                serverXmlFile);
        runCompileAndGenerateFeatures();

        // Verify BINARY_SCANNER_CONFLICT_MESSAGE2 error is thrown (BinaryScannerUtil.RecommendationSetException)
        Set<String> recommendedFeatureSet = new HashSet<String>();
        recommendedFeatureSet.addAll(getExpectedGeneratedFeaturesSet());
        assertTrue("Could not find the feature conflict message in the process output.\n " + formatOutput(processOutput),
                processOutput.contains(
                        String.format(BINARY_SCANNER_CONFLICT_MESSAGE2, getCdi12ConflictingFeatures(), recommendedFeatureSet)));
    }

    /**
     * Conflict between user specified features and API usage.
     * Check for BINARY_SCANNER_CONFLICT_MESSAGE1 (conflict between configured features and API usage)
     * 
     * @throws Exception
     */
    @Test
    public void userAndGeneratedConflictTest() throws Exception {
        // place conflicting feature cdi-1.2 in server.xml
        replaceString("<!--replaceable-->",
                "<featureManager>\n" +
                        "<feature>cdi-1.2</feature>\n" +
                        "</featureManager>\n",
                serverXmlFile);
        runCompileAndGenerateFeatures();

        // Verify BINARY_SCANNER_CONFLICT_MESSAGE1 error is thrown (BinaryScannerUtil.FeatureModifiedException)
        Set<String> recommendedFeatureSet = new HashSet<String>();
        recommendedFeatureSet.add("cdi-2.0");
        recommendedFeatureSet.addAll(getExpectedGeneratedFeaturesSet());
        assertTrue("Could not find the feature conflict message in the process output.\n" + formatOutput(processOutput),
            processOutput.contains(
                String.format(BINARY_SCANNER_CONFLICT_MESSAGE1, getCdi12ConflictingFeatures(), recommendedFeatureSet)));
    }

    // TODO add an integration test for feature conflict for API usage (BINARY_SCANNER_CONFLICT_MESSAGE3), ie. MP4 and EE9

    /**
     * Conflict between required features in API usage or configured features and MP/EE level specified
     * Check for BINARY_SCANNER_CONFLICT_MESSAGE5 (feature unavailable for required MP/EE levels)
     * 
     * @throws Exception
     */
    @Test
    public void featureUnavailableConflictTest() throws Exception {
        // change MP 4.1 to MP 1.2
        modifyMPVersion("4.1", "1.2");

        // add mpOpenAPI-1.0 feature to server.xml, not available in MP 1.2
        replaceString("<!--replaceable-->",
                "<featureManager>\n" +
                        "<feature>mpOpenAPI-1.0</feature>\n" +
                        "</featureManager>\n",
                serverXmlFile);
        runCompileAndGenerateFeatures();

        Set<String> conflictingFeatureSet = getExpectedGeneratedFeaturesSet();
        conflictingFeatureSet.add("mpOpenAPI-1.0");
        Set<String> removeFeatures = new HashSet<String>(Arrays.asList("mpOpenAPI"));
        assertTrue("Could not find the feature conflict message in the process output.\n " + processOutput,
        processOutput.contains(
                String.format(BINARY_SCANNER_CONFLICT_MESSAGE5, conflictingFeatureSet, "mp1.2", "ee8", removeFeatures)));
    }

    // get the app features that conflict with cdi-1.2
    protected Set<String> getCdi12ConflictingFeatures() {
        // jaxrs-2.1 and servlet-4.0 (EE8) conflicts with cdi-1.2 (EE7)
        Set<String> conflictingFeatures = new HashSet<String>();
        conflictingFeatures.add("servlet-4.0");
        conflictingFeatures.add("cdi-1.2");
        conflictingFeatures.add("jaxrs-2.1");
        return conflictingFeatures;
    }

    protected Set<String> getExpectedGeneratedFeaturesSet() {
        return new HashSet<String>(Arrays.asList("servlet-4.0", "jaxrs-2.1"));
    }

    // change MP version in pom file
    protected void modifyMPVersion(String currentVersion, String updatedVersion) throws IOException { 
        replaceString("<artifactId>microprofile</artifactId>\n" + 
        "        <version>" + currentVersion + "</version>", "<artifactId>microprofile</artifactId>\n" + 
        "        <version>" + updatedVersion + "</version>", pom);
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
