/*******************************************************************************
 * (c) Copyright IBM Corporation 2026.
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

import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies that {@code liberty:dev} starts correctly when the
 * {@code rar}-packaged upstream module has already been installed to the local
 * Maven repository by a prior {@code mvn install}.
 *
 * <p>This exercises the "artifact already exists" branch of
 * {@code getOrCreateRarArtifact()} — the happy path where the artifact is found
 * in {@code ~/.m2} and no in-memory redirect is needed.  It complements
 * {@link MultiModuleRarUpstreamTest} which tests the cold-start (no prior
 * install) path.
 */
public class MultiModuleRarPreInstalledTest extends BaseMultiModuleTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        setUpMultiModule("sample.rar", "rar-ear", null);
        // Pre-install the rar-ra module to ~/.m2 to simulate the "already installed"
        // scenario described in the bug report workaround. getOrCreateRarArtifact()
        // should detect the existing artifact and skip the in-memory redirect.
        runCommand("mvn install -pl rar-ra -am");
        run();
    }

    /**
     * Verifies startup and endpoint when the RAR artifact is pre-installed.
     * Exercises the "artifact already exists in ~/.m2" branch of
     * {@code getOrCreateRarArtifact()}.
     */
    @Test
    public void runTest() throws Exception {
        assertTrue("Liberty server did not start when rar-ra was pre-installed. " + getLogTail(),
                verifyLogMessageExists("CWWKF0011I:", 120000));

        assertEndpointContent("http://localhost:9080/rar-war", "Hello RAR World.");
    }

    @AfterClass
    public static void cleanUpAfterClass() throws Exception {
        BaseDevTest.cleanUpAfterClass();
    }
}
