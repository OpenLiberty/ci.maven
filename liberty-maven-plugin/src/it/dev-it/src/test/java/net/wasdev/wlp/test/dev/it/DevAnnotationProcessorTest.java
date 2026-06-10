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

public class DevAnnotationProcessorTest extends BaseDevTest {

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      setUpBeforeClass(null, "../resources/basic-dev-project-lombok");
   }

   @AfterClass
   public static void cleanUpAfterClass() throws Exception {
      BaseDevTest.cleanUpAfterClass();
   }

   @Test
   public void annotationProcessorTest() throws Exception {
      tagLog("##annotationProcessorTest start");
      
      assertTrue("Web app should be available", verifyLogMessageExists(WEB_APP_AVAILABLE, 60000));
      
      assertFalse("Should not have compilation errors during startup",
                  verifyLogMessageExists("variable name not initialized", 2000));
      
      File pojoFile = new File(tempProj, "src/main/java/com/demo/rest/Pojo.java");
      assertTrue("Pojo.java should exist", pojoFile.exists());
      replaceString("@RequiredArgsConstructor", "// Modified\n@RequiredArgsConstructor", pojoFile);

      Thread.sleep(8000);
      
      assertTrue("Recompilation should succeed with annotation processor",
                 verifyLogMessageExists(COMPILATION_SUCCESSFUL, 60000));
      assertFalse("Should not have compilation errors",
                  verifyLogMessageExists("variable name not initialized", 5000));

      assertTrue("Transitive dependency 'mapstruct' should be resolved and included in the processor path",
                 verifyLogMessageExists("mapstruct-", 5000));
      
      tagLog("##annotationProcessorTest end");
   }
}
