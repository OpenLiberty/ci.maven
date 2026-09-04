/*******************************************************************************
 * (c) Copyright IBM Corporation 2024.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies that {@code liberty:dev} correctly handles a {@code rar}-packaged
 * upstream module in a multi-module project so that the downstream EAR can
 * resolve the artifact during its dependency resolution phase.
 *
 * <p>Without the fix in {@code DevMojo.java}, dev mode falls into the generic
 * {@code else} branch for upstream modules and only runs {@code resources} +
 * {@code compile}.  The RAR artifact is never made resolvable, so the downstream
 * EAR's Maven dependency resolution fails with "Could not find artifact" before
 * Liberty even starts.
 *
 * <p>The fix adds a dedicated {@code rar} branch in {@code doDevMode()} that
 * calls {@code getOrCreateRarArtifact()} — mirroring the existing EAR treatment
 * — which points the in-memory artifact reference at the module's {@code target/}
 * directory so that downstream resolution succeeds without requiring a prior
 * {@code mvn install}.
 */
public class MultiModuleRarUpstreamTest extends BaseMultiModuleTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        setUpMultiModule("sample.rar", "rar-ear", null);
        run();
    }

    /**
     * Single sequential test that:
     * <ol>
     *   <li>Verifies the server started (dependency resolution on the {@code rar-ra}
     *       artifact succeeded — the core bug fix assertion).</li>
     *   <li>Confirms no dependency resolution error was logged.</li>
     *   <li>Confirms the WAR inside the EAR is reachable.</li>
     *   <li>Modifies the WAR servlet and confirms dev mode hot-reloads it —
     *       verifying the dev mode watcher loop still functions in a project
     *       with a {@code rar}-packaged upstream module.</li>
     * </ol>
     */
    @Test
    public void runTest() throws Exception {
        // 1. Server must have started — proves rar-ra artifact was resolvable
        assertTrue("Liberty server did not start. " + getLogTail(),
                verifyLogMessageExists("CWWKF0011I:", 120000));

        // 2. No dependency resolution error
        assertFalse("Found 'Could not find artifact' in log. " + getLogTail(),
                verifyLogMessageExists("Could not find artifact", 2000));
        assertFalse("Found 'Could not resolve dependencies' in log. " + getLogTail(),
                verifyLogMessageExists("Could not resolve dependencies", 2000));

        // 3. WAR endpoint is live
        assertEndpointContent("http://localhost:9080/rar-war", "Hello RAR World.");

        // 4. Hot-reload: modify the WAR servlet and verify the app updates
        modifyWarServlet();

        assertEndpointContent("http://localhost:9080/rar-war", "Hello RAR World Updated.");
    }

    private static void modifyWarServlet() throws Exception {
        int appUpdatedCount = countOccurrences("CWWKZ0003I:", logFile);

        File srcFile = new File(tempProj, "rar-war/src/main/java/sample/rar/web/HelloServlet.java");
        File targetClass = new File(tempProj, "rar-war/target/classes/sample/rar/web/HelloServlet.class");
        assertTrue(srcFile.exists());
        assertTrue(targetClass.exists());

        long lastModified = targetClass.lastModified();
        waitLongEnough();
        replaceString("Hello RAR World.", "Hello RAR World Updated.", srcFile);

        assertTrue(getLogTail(), verifyLogMessageExists("CWWKZ0003I:", 10000, logFile, ++appUpdatedCount));
        assertTrue(waitForCompilation(targetClass, lastModified, 6000));
    }

    @AfterClass
    public static void cleanUpAfterClass() throws Exception {
        BaseDevTest.cleanUpAfterClass();
    }
}
