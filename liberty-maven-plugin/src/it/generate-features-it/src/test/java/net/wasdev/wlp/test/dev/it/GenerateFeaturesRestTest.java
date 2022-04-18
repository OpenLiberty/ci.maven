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
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * liberty:generate-features goal tests for various MicroProfile and Java EE versions
 * Test to ensure the binary scanner honours the version of MicroProfile and Java EE
 * specified in the pom.xml
 */
public class GenerateFeaturesRestTest extends BaseGenerateFeaturesTest {

    @Before
    public void setUp() throws Exception {
        setUpBeforeTest("../resources/restful");
    }

    @After
    public void cleanUp() throws Exception {
        cleanUpAfterTest();
    }

    @Test
    public void mp71Test() throws Exception {
        // Test Java EE 7.0 and MicroProfile 1.x
        File pomFile = new File(tempProj, "pom.xml");
        replaceString("EE_VERSION", "7.0", pomFile);
        replaceString("MP_VERSION", "1.4", pomFile);
        runCompileAndGenerateFeatures();

        // verify that the generated features file was created
        assertTrue(processOutput, newFeatureFile.exists());

        // verify that the correct features are in the generated-features.xml
        Set<String> features = readFeatures(newFeatureFile);
        assertTrue(features.contains("mpRestClient-1.1"));
        assertTrue(features.contains("cdi-1.2"));
    }

    @Test
    public void mp81Test() throws Exception {
        // Test Java EE 8.0 and MicroProfile 1.x
        // EE 8 forces use of cdi 2.0
        File pomFile = new File(tempProj, "pom.xml");
        replaceString("EE_VERSION", "8.0", pomFile);
        replaceString("MP_VERSION", "1.4", pomFile);
        runCompileAndGenerateFeatures();

        // verify that the generated features file was created
        assertTrue(processOutput, newFeatureFile.exists());

        // verify that the correct features are in the generated-features.xml
        Set<String> features = readFeatures(newFeatureFile);
        assertTrue(features.contains("mpRestClient-1.1"));
        assertTrue(features.contains("cdi-2.0"));
    }

    @Test
    public void mp21Test() throws Exception {
        // Test Java EE 8.0 and MicroProfile 2.x
        // MicroProfile 2.1 uses cdi-2.0 so it requires EE8
        File pomFile = new File(tempProj, "pom.xml");
        replaceString("EE_VERSION", "8.0", pomFile);
        replaceString("MP_VERSION", "2.1", pomFile);
        runCompileAndGenerateFeatures();

        // verify that the generated features file was created
        assertTrue(processOutput, newFeatureFile.exists());

        // verify that the correct features are in the generated-features.xml
        Set<String> features = readFeatures(newFeatureFile);
        assertTrue(features.contains("mpRestClient-1.1"));
        assertTrue(features.contains("cdi-2.0"));
    }

    @Test
    public void mp30Test() throws Exception {
        // Test Java EE 8.0 and MicroProfile 3.x
        File pomFile = new File(tempProj, "pom.xml");
        replaceString("EE_VERSION", "8.0", pomFile);
        replaceString("MP_VERSION", "3.0", pomFile);
        runCompileAndGenerateFeatures();

        // verify that the generated features file was created
        assertTrue(processOutput, newFeatureFile.exists());

        // verify that the correct features are in the generated-features.xml
        Set<String> features = readFeatures(newFeatureFile);
        assertTrue(features.contains("mpRestClient-1.3"));
        assertTrue(features.contains("cdi-2.0"));
    }

    @Test
    public void mp41Test() throws Exception {
        // Test Java EE 8.0 and MicroProfile 4.x
        File pomFile = new File(tempProj, "pom.xml");
        replaceString("EE_VERSION", "8.0", pomFile);
        replaceString("MP_VERSION", "4.1", pomFile);
        runCompileAndGenerateFeatures();

        // verify that the generated features file was created
        assertTrue(processOutput, newFeatureFile.exists());

        // verify that the correct features are in the generated-features.xml
        Set<String> features = readFeatures(newFeatureFile);
        assertTrue(features.contains("mpRestClient-2.0"));
        assertTrue(features.contains("cdi-2.0"));
    }

    @Test
    public void jakartaTest() throws Exception {
        // Test Jakarta EE 8.0 and MicroProfile 4.x
        File pomFile = new File(tempProj, "pom.xml");
        replaceString(
            "        <dependency>\n" +
            "            <groupId>javax</groupId>\n" +
            "            <artifactId>javaee-api</artifactId>\n" +
            "            <version>EE_VERSION</version>\n" +
            "            <scope>provided</scope>\n" +
            "        </dependency>\n",
            "        <dependency>\n" +
            "            <groupId>jakarta.platform</groupId>\n" +
            "            <artifactId>jakarta.jakartaee-api</artifactId>\n" +
            "            <version>8.0.0</version>\n" +
            "            <scope>provided</scope>\n" +
            "        </dependency>\n",
            pomFile);
        replaceString("MP_VERSION", "4.1", pomFile);
        runCompileAndGenerateFeatures();

        // verify that the generated features file was created
        assertTrue(processOutput, newFeatureFile.exists());

        // verify that the correct features are in the generated-features.xml
        Set<String> features = readFeatures(newFeatureFile);
        assertTrue(features.contains("mpRestClient-2.0"));
        assertTrue(features.contains("cdi-2.0"));
    }
}
