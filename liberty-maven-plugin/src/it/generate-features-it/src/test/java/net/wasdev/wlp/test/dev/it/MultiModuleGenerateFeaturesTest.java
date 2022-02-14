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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class MultiModuleGenerateFeaturesTest extends GenerateFeaturesTest {

    @Override
    @Before
    public void setUp() throws Exception {
        setUpBeforeTest("../resources/multi-module-project");
        pom = new File(tempProj, "pom/pom.xml");
        assertTrue(pom.exists());
        replaceVersion(new File(tempProj, "pom")); // "pom" module is liberty configuration module
        newFeatureFile = new File(tempProj, "pom" + GENERATED_FEATURES_FILE_PATH);
        serverXmlFile = new File(tempProj, "pom/src/main/liberty/config/server.xml");
        targetDir = new File(tempProj, "war/target");
    }

    @Override
    protected void runCompileAndGenerateFeatures() throws IOException, InterruptedException {
        runProcess("compile io.openliberty.tools:liberty-maven-plugin:" + System.getProperty("mavenPluginVersion")
                + ":generate-features");
    }

    @Override
    protected void runGenerateFeaturesGoal() throws IOException, InterruptedException {
        runProcess("io.openliberty.tools:liberty-maven-plugin:" + System.getProperty("mavenPluginVersion")
                + ":generate-features");
    }

    @Override
    protected Set<String> getExpectedGeneratedFeaturesSet() {
        return new HashSet<String>(Arrays.asList("cdi-2.0", "mpMetrics-3.0", "jaxrs-2.1"));
    }
 
    @Override
    protected Set<String> getCdi12ConflictingFeatures() {
        // cdi-2.0 (EE8) conflicts with cdi-1.2 (EE7)
        Set<String> conflictingFeatures = new HashSet<String>();
        conflictingFeatures.add("cdi-1.2");
        conflictingFeatures.add("cdi-2.0");
        return conflictingFeatures;
    }

    @Override
    @Ignore // TODO re-enable this test once https://github.com/OpenLiberty/ci.maven/issues/1429 is resolved
    @Test
    public void userAndGeneratedConflictTest() throws Exception {
    }

}
