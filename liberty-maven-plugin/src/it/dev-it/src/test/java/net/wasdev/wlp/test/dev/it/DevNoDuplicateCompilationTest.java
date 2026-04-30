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

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class DevNoDuplicateCompilationTest extends BaseDevTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        setUpBeforeClass("-DgenerateFeatures=true");
    }

    @AfterClass
    public static void cleanUpAfterClass() throws Exception {
        BaseDevTest.cleanUpAfterClass();
    }

    @Test
    public void testNoDuplicateCompilation() throws Exception {
        assertTrue(verifyLogMessageExists("Liberty is running in dev mode.", 10000));
        assertTrue(verifyLogMessageExists(WEB_APP_AVAILABLE, 20000));

        File srcHelloWorld = new File(tempProj, "src/main/java/com/demo/HelloWorld.java");
        File targetHelloWorld = new File(targetDir, "classes/com/demo/HelloWorld.class");
        assertTrue(srcHelloWorld.exists());
        assertTrue(targetHelloWorld.exists());

        // Count initial compilation messages
        int initialCompilationCount = countOccurrences(COMPILATION_SUCCESSFUL, logFile);
        int initialHotReloadCount = countOccurrences(SERVER_CONFIG_SUCCESS, logFile);

        waitLongEnough();
        long lastModified = targetHelloWorld.lastModified();

        // Modify the Java file
        String modification = "// Test modification for duplicate compilation check";
        BufferedWriter javaWriter = null;
        try {
            javaWriter = new BufferedWriter(new FileWriter(srcHelloWorld, true));
            javaWriter.append('\n');
            javaWriter.append(modification);
        } finally {
            if (javaWriter != null) {
                javaWriter.close();
            }
        }

        assertTrue("Class file was not recompiled", waitForCompilation(targetHelloWorld, lastModified, 10000));
        assertTrue("Source compilation message not found", 
            verifyLogMessageExists(COMPILATION_SUCCESSFUL, 10000, ++initialCompilationCount));
        assertTrue("Liberty hot reload message (CWWKZ0003I) not found",
            verifyLogMessageExists(SERVER_CONFIG_SUCCESS, 10000, ++initialHotReloadCount));

        Thread.sleep(3000);

        // Count final compilation messages
        int finalCompilationCount = countOccurrences(COMPILATION_SUCCESSFUL, logFile);
        int finalHotReloadCount = countOccurrences(SERVER_CONFIG_SUCCESS, logFile);

        assertEquals("Duplicate compilation detected - compilation happened more than once",
            initialCompilationCount, finalCompilationCount);
        assertEquals("Multiple hot reloads detected - should only reload once per change",
            initialHotReloadCount, finalHotReloadCount);
    }

    @Test
    public void testMultipleSequentialChanges() throws Exception {
        assertTrue(verifyLogMessageExists("Liberty is running in dev mode.", 10000));
        assertTrue(verifyLogMessageExists(WEB_APP_AVAILABLE, 20000));

        File srcHelloWorld = new File(tempProj, "src/main/java/com/demo/HelloWorld.java");
        File targetHelloWorld = new File(targetDir, "classes/com/demo/HelloWorld.class");
        assertTrue(srcHelloWorld.exists());
        assertTrue(targetHelloWorld.exists());

        // Perform multiple sequential changes
        for (int i = 1; i <= 3; i++) {
            int compilationCountBefore = countOccurrences(COMPILATION_SUCCESSFUL, logFile);
            int hotReloadCountBefore = countOccurrences(SERVER_CONFIG_SUCCESS, logFile);

            waitLongEnough();
            long lastModified = targetHelloWorld.lastModified();

            // Modify the Java file
            String modification = "// Test modification #" + i + " for duplicate compilation check";
            BufferedWriter javaWriter = null;
            try {
                javaWriter = new BufferedWriter(new FileWriter(srcHelloWorld, true));
                javaWriter.append('\n');
                javaWriter.append(modification);
            } finally {
                if (javaWriter != null) {
                    javaWriter.close();
                }
            }

            assertTrue("Class file was not recompiled for change #" + i,
                waitForCompilation(targetHelloWorld, lastModified, 10000));
            assertTrue("Source compilation message not found for change #" + i,
                verifyLogMessageExists(COMPILATION_SUCCESSFUL, 10000, ++compilationCountBefore));
            assertTrue("Liberty hot reload message (CWWKZ0003I) not found for change #" + i,
                verifyLogMessageExists(SERVER_CONFIG_SUCCESS, 10000, ++hotReloadCountBefore));

            Thread.sleep(3000);

            // Count compilation messages after change
            int compilationCountAfter = countOccurrences(COMPILATION_SUCCESSFUL, logFile);
            int hotReloadCountAfter = countOccurrences(SERVER_CONFIG_SUCCESS, logFile);

            assertEquals("Duplicate compilation detected for change #" + i,
                compilationCountBefore, compilationCountAfter);
            assertEquals("Multiple hot reloads detected for change #" + i,
                hotReloadCountBefore, hotReloadCountAfter);
        }
    }
}
