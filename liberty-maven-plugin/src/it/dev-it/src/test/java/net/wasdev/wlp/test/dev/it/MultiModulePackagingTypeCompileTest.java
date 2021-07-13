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

public class MultiModulePackagingTypeCompileTest extends BaseMultiModuleTest {

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      setUpMultiModule("typeE", "pom", null);
   }

   @Test
   public void skipCompileForPackagingTypesTest() throws Exception {

      String javaContent = "package A; public class A { public static int a() { return 1; } }";

      File earSrcClass = new File(tempProj, "ear/src/main/java/A/A.java");
      earSrcClass.getParentFile().mkdirs();
      assertTrue(earSrcClass.createNewFile());
      File earTargetClass = new File(tempProj, "ear/target/classes/A/A.class");
      Files.write(earSrcClass.toPath(), javaContent.getBytes());

      File pomSrcClass = new File(tempProj, "pom/src/main/java/A/A.java");
      pomSrcClass.getParentFile().mkdirs();
      assertTrue(pomSrcClass.createNewFile());
      File pomTargetClass = new File(tempProj, "pom/target/classes/A/A.class");
      Files.write(pomSrcClass.toPath(), javaContent.getBytes());

      run();
      
      // asset ear and pom sources were not compiled
      assertFalse(getLogTail(logFile), readFile("guide-maven-multimodules-ear source compilation was successful", logFile));
      assertFalse(getLogTail(logFile), readFile("guide-maven-multimodules-pom source compilation was successful", logFile));
      assertFalse(getLogTail(logFile), earTargetClass.exists());
      assertFalse(getLogTail(logFile), pomTargetClass.exists());
   }

}

