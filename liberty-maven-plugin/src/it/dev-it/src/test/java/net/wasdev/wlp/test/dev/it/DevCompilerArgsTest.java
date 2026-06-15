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

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test that compiler arguments from maven-compiler-plugin configuration
 * are properly passed through during Liberty dev mode hot reload.
 */
public class DevCompilerArgsTest extends BaseDevTest {

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      setUpBeforeClass(null, "../resources/basic-dev-project-compiler-args");
   }

   @AfterClass
   public static void cleanUpAfterClass() throws Exception {
      BaseDevTest.cleanUpAfterClass();
   }

   @Test
   public void compilerArgsTest() throws Exception {
      tagLog("##compilerArgsTest start");
      
      assertTrue("Web app should be available", verifyLogMessageExists(WEB_APP_AVAILABLE, 60000));
      
      File resourceFile = new File(tempProj, "src/main/java/com/demo/rest/HelloResource.java");
      assertTrue("HelloResource.java should exist", resourceFile.exists());
      replaceString("@Path\\(\"/hello\"\\)", "// Modified\n@Path(\"/hello\")", resourceFile);

      Thread.sleep(8000);
      
      assertTrue("Hot reload should show 'Recompiling with compiler options' message",
                 verifyLogMessageExists("Recompiling with compiler options", 60000));
      assertTrue("Hot reload should include -parameters in compiler options",
                 verifyLogMessageExists("-parameters", 5000));
      assertTrue("Hot reload should include -Xlint:-processing in compiler options",
                 verifyLogMessageExists("-Xlint:-processing", 5000));
      assertTrue("Hot reload should include custom arg -Atest.custom.arg=compilerArgsTest in compiler options",
                 verifyLogMessageExists("-Atest.custom.arg=compilerArgsTest", 5000));
      
      assertTrue("Recompilation should succeed",
                 verifyLogMessageExists(COMPILATION_SUCCESSFUL, 60000));
      
      tagLog("##compilerArgsTest end");
   }
}
