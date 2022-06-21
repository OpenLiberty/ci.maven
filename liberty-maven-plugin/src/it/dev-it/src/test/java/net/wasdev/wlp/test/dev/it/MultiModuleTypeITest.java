/*******************************************************************************
 * (c) Copyright IBM Corporation 2021, 2022.
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

public class MultiModuleTypeITest extends BaseMultiModuleTest {

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      setUpMultiModule("typeI", "ear", null);
      run();
   }

   @Test
   public void runTest() throws Exception {
      assertTrue(verifyLogMessageExists(
            "The recompileDependencies parameter is set to \"true\". On a file change all dependent modules will be recompiled.",
            20000));

      // verify ear test class did not compile successfully
      File targetEarClass = new File(tempProj,
            "ear/target/test-classes/it/io/openliberty/guides/multimodules/IT.class");
      assertFalse(targetEarClass.exists());
      assertTrue(getLogTail(), verifyLogMessageExists("guide-maven-multimodules-ear tests compilation had errors", 10000));

      // add a dependency to parent pom and check that it resolves compile errors in
      // child modules
      File parentPom = new File(tempProj, "parent/pom.xml");
      assertTrue(parentPom.exists());
      replaceString("<!-- SUB JUNIT -->",
            "<dependency> <groupId>org.junit.jupiter</groupId> <artifactId>junit-jupiter</artifactId> <version>5.6.2</version> <scope>test</scope> </dependency>",
            parentPom);
      // wait for compilation
      assertTrue(getLogTail(), verifyLogMessageExists("guide-maven-multimodules-ear tests compilation was successful.", 6000, logFile));
      assertTrue(getLogTail(), verifyFileExists(targetEarClass, 10000));

      super.manualTestsInvocation("guide-maven-multimodules-jar", "guide-maven-multimodules-war",
            "guide-maven-multimodules-ear");

      testEndpointsAndUpstreamRecompile();
   }

}
