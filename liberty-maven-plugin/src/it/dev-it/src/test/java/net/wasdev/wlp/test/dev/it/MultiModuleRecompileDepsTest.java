/*******************************************************************************
 * (c) Copyright IBM Corporation 2021.
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
import java.io.IOException;
import java.nio.file.Files;

import org.apache.maven.shared.utils.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;

public class MultiModuleRecompileDepsTest extends BaseMultiModuleTest {

      @BeforeClass
      public static void setUpBeforeClass() throws Exception {
            setUpMultiModule("typeA", "ear", null);
            // test setting recompileDependencies to false for a multi module project
            run("-DrecompileDependencies=false -DhotTests=true");
      }

      @Test
      public void runTest() throws Exception {
            assertTrue(verifyLogMessageExists(
                        "The recompileDependencies parameter is set to \"false\". On a file change only the affected classes will be recompiled.",
                        20000));

            verifyStartupHotTests("guide-maven-multimodules-jar", "guide-maven-multimodules-war",
                        "guide-maven-multimodules-ear");

            // verify that when modifying a jar class, classes in dependent modules are
            // not recompiled as well
            File targetWebClass = getTargetFileForModule(
                        "war/src/main/java/io/openliberty/guides/multimodules/web/HeightsBean.java",
                        "war/target/classes/io/openliberty/guides/multimodules/web/HeightsBean.class");
            long webLastModified = targetWebClass.lastModified();
            testEndpointsAndUpstreamRecompile();

            // TODO verify that tests ran

            // verify a source class in the war module was not compiled
            assertTrue(targetWebClass.lastModified() == webLastModified);

            // Verify that with recompileDependencies=false, failing classes from upstream and downstream modules are still
            // re-tried for compilation on source file change

            // create compilation error in jar module
            modifyFileForModule("jar/src/main/java/io/openliberty/guides/multimodules/lib/Converter.java", "return inches;", "return invalid");
            Thread.sleep(5000); // wait for compilation
            assertTrue(getLogTail(), verifyLogMessageExists("guide-maven-multimodules-jar source compilation had errors.", 10000));

            // create compilation error in ear module
            modifyFileForModule("ear/src/test/java/it/io/openliberty/guides/multimodules/ConverterAppIT.java", "String war", "invalid");
            Thread.sleep(5000); // wait for compilation
            assertTrue(getLogTail(), verifyLogMessageExists("guide-maven-multimodules-ear tests compilation had errors.", 10000));

            clearLogFile(); // need to clear log file so that we are checking for the correct compilation messages below

            modifyFileForModule("war/src/main/java/io/openliberty/guides/multimodules/web/HeightsBean.java", "return heightCm;", "return \"24\";");
            assertTrue(getLogTail(), verifyLogMessageExists("guide-maven-multimodules-jar source compilation had errors.", 10000));
            assertTrue(getLogTail(), verifyLogMessageExists("guide-maven-multimodules-war source compilation was successful.", 10000));
            assertTrue(getLogTail(), verifyLogMessageExists("guide-maven-multimodules-ear tests compilation had errors.", 10000));

            // verify that tests did not run
            verifyTestsDidNotRun("guide-maven-multimodules-jar", "guide-maven-multimodules-war",
            "guide-maven-multimodules-ear");
      }
}
