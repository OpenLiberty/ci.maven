/*******************************************************************************
 * (c) Copyright IBM Corporation 2019.
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

public class BaseMultiModuleTest extends BaseDevTest {

   public static void setUpMultiModule(String testcaseFolder, String libertyModule) throws Exception {
      startProcessDuringSetup = false;

      setUpBeforeClass(null, "../resources/multi-module-projects/" + testcaseFolder);
      libertyConfigModule = libertyModule;

      optionalReplaceVersion(tempProj);
      optionalReplaceVersion(new File(tempProj, "pom"));
      optionalReplaceVersion(new File(tempProj, "ear"));
      optionalReplaceVersion(new File(tempProj, "war"));
      optionalReplaceVersion(new File(tempProj, "war2"));
   }

   protected static void run() throws Exception {
      startProcess(null, true, "mvn io.openliberty.tools:liberty-maven-plugin:"+System.getProperty("mavenPluginVersion")+":");
   }

   private static void replaceVersion(File dir) throws IOException {
      File pom = new File(dir, "pom.xml");
      String pluginVersion = System.getProperty("mavenPluginVersion");
      replaceString("SUB_VERSION", pluginVersion, pom);
      String runtimeVersion = System.getProperty("runtimeVersion");
      replaceString("RUNTIME_VERSION", runtimeVersion, pom);
   }

   private static void optionalReplaceVersion(File pom) {
      try {
         replaceVersion(pom);
      } catch (IOException e) {
         // ignore failures
      }
   }

   public void manualTestsInvocationTest() throws Exception {
      assertTrue(verifyLogMessageExists("To run tests on demand, press Enter.", 30000));

      writer.write("\n");
      writer.flush();

      assertTrue(verifyLogMessageExists(".*Unit tests .* finished.", 10000));
      assertTrue(verifyLogMessageExists(".*Integration tests .* finished.", 2000));

      assertFalse("Found CWWKM2179W message indicating incorrect app deployment", verifyLogMessageExists("CWWKM2179W", 2000));
   }

   @AfterClass
   public static void cleanUpAfterClass() throws Exception {
      BaseDevTest.cleanUpAfterClass();
   }
}

