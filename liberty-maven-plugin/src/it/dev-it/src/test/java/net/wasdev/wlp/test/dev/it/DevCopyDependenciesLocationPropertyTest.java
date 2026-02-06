/*******************************************************************************
 * (c) Copyright IBM Corporation 2025.
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
 * Test property resolution in copyDependencies location paths.
 * This test verifies that Maven properties used in the location attribute
 * of dependencyGroups are correctly resolved.
 */
public class DevCopyDependenciesLocationPropertyTest extends BaseDevTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        setUpBeforeClass(null, "../resources/basic-dev-project", true, false, null, null);

        // Add a provided-scoped dependency with property references
        String additionalDependencies = "<dependency> <groupId>${pg.group.id}</groupId> <artifactId>${pg.artifact.id}</artifactId> <version>${pg.version}</version> <scope>provided</scope> </dependency>";
        replaceStringLiteral("<!-- ADDITIONAL_DEPENDENCIES -->", additionalDependencies, pom);

        // Configure copyDependencies with property reference in location
        String additionalConfiguration = "<copyDependencies> <dependencyGroup> <location>${custom.dir.location}</location> <dependency> <groupId>${pg.group.id}</groupId> <artifactId>${pg.artifact.id}</artifactId> </dependency> </dependencyGroup> </copyDependencies>";
        replaceStringLiteral("<!-- ADDITIONAL_CONFIGURATION -->", additionalConfiguration, pom);

        startProcess(null, true);
    }

    @AfterClass
    public static void cleanUpAfterClass() throws Exception {
        BaseDevTest.cleanUpAfterClass();
    }

    @Test
    public void propertyResolutionTest() throws Exception {
        // This test verifies that Maven properties are resolved correctly in:
        // 1. Dependency variables (${pg.group.id}, ${pg.artifact.id}, etc)
        // 2. Location path (${custom.dir.location})

        // Verify the dependency was copied successfully with resolved filename
        assertTrue("Dependency copy log message not found: " + getLogTail(),
                verifyLogMessageExists("copyDependencies copied file postgresql-42.1.1.jar", 2000));

        // Verify that the location property was resolved (customDirLocation instead of ${custom.dir.location})
        assertTrue("Location property resolution failed - expected 'customDirLocation' in log: "+getLogTail(),
                verifyLogMessageExists("customDirLocation", 2000));

        // Verify the file exists at the resolved location
        File f = new File(targetDir, "liberty/wlp/usr/servers/defaultServer/customDirLocation/postgresql-42.1.1.jar");
        assertTrue("Dependency was not copied to the resolved location: " + f.getAbsolutePath(), f.exists());
    }
}