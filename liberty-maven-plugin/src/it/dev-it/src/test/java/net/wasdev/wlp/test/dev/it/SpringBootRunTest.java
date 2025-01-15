/*******************************************************************************
 * (c) Copyright IBM Corporation 2019, 2022.
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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static io.openliberty.tools.common.plugins.util.BinaryScannerUtil.*;

public class SpringBootRunTest extends BaseDevTest {

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      setUpBeforeClass(null, "../resources/springboot-project", false, false, null, null);
   }

   @AfterClass
   public static void cleanUpAfterClass() throws Exception {
      BaseDevTest.cleanUpAfterClass(false);
   }

   /**
    * As part of the multi-module validation, the run goal is skipped on projects with a "jar" packaging type.
    * There is an exception for Spring Boot projects. This test will validate that the spring boot project is not skipped
    * 
    * @throws Exception
    */
   @Test
   public void validateRunExecutionNotSkipped() throws Exception {
	   // Make sure we are not skipping the project
	   assertTrue(verifyLogMessageDoesNotExist("Skipping module springboot-project which is not included in this invocation of the run goal", 30000));
   
	   // Check that the server has started
       assertTrue(verifyLogMessageExists("CWWKF0011I", 120000));
   }
}
