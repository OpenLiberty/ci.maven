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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.maven.shared.utils.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class MultiModuleRunM2InstalledTest extends BaseMultiModuleTest {

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      setUpMultiModule("typeA", "ear", null);
   }

   @Test
   public void purgeLocallyInstalledModule_LibertyRun_Test() throws Exception {

      // install everything to m2
      runCommand("mvn install");

      // ensure class was compiled
      File targetClass = new File(tempProj, "jar/target/classes/io/openliberty/guides/multimodules/lib/Converter.class");
      assertTrue(targetClass.exists());

      // delete the source file
      File srcClass = new File(tempProj, "jar/src/main/java/io/openliberty/guides/multimodules/lib/Converter.java");
      assertTrue(srcClass.delete());

      // clean targets
      runCommand("mvn clean");

      // run goal should purge the jar module from m2, so that the war module will show a failure due to the missing dependency.
      // i.e. it should not find the jar dependency from m2
      startProcess(null, false, "mvn io.openliberty.tools:liberty-maven-plugin:"+System.getProperty("mavenPluginVersion")+":", false);
      
      // TODO when https://github.com/OpenLiberty/ci.maven/issues/1203 is fixed, check for compilation error instead
      assertTrue(getLogTail(logFile), verifyLogMessageExists("Could not resolve dependencies for project", 5000));
      
      assertFalse(getLogTail(logFile), targetClass.exists());
   }

   @AfterClass
   public static void cleanUpAfterClass() throws Exception {
      // use a run-style cleanup (e.g. kill the process), and skip checking for server
      // shutdown since server should not have fully started
      BaseDevTest.cleanUpAfterClass(false, false);
   }

}

