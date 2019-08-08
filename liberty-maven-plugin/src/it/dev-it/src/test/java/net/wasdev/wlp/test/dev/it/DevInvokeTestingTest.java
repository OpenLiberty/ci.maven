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

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertFalse;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class DevInvokeTestingTest extends BaseDevTest {

   static File testResources = new File("../resources");

   @Test
   public void manualTestsInvocationTest() throws Exception {

      if (isWindows) return;

      assertFalse(checkLogMessage(4000,  "Press the Enter key to run tests on demand."));

      writer.write("\n");
      writer.flush();

      assertFalse(checkLogMessage(4000,  "Unit tests finished."));
      assertFalse(checkLogMessage(4000,  "Integration tests finished."));
   }

   @Test
   public void passingTestsInvocationTest() throws Exception {

      if (isWindows) return;

      assertFalse(checkLogMessage(4000,  "Press the Enter key to run tests on demand."));

      FileUtils.deleteDirectory(new File(tempProj, "src/test/java/com/demo/test/"));
      FileUtils.copyFile(new File(testResources, "HelloTest.java"), new File(tempProj, "src/test/java/com/demo/test/HelloTest.java"));
      FileUtils.copyFile(new File(testResources, "HelloIT.java"), new File(tempProj, "src/test/java/it/com/demo/test/HelloIT.java"));

      Thread.sleep(4000); // wait for compilation

      writer.write("\n");
      writer.flush();
      
      assertFalse(checkLogMessage(10000,  "Running com.demo.test.HelloTest"));
      assertFalse(checkLogMessage(10000,  "Tests run: 1, Failures: 0"));
      assertFalse(checkLogMessage(10000,  "Unit tests finished."));

      assertFalse(checkLogMessage(10000,  "Running it.com.demo.test.HelloIT"));
      assertFalse(checkLogMessage(10000,  "Tests run: 1, Failures: 0"));
      assertFalse(checkLogMessage(10000,  "Integration tests finished."));
   }

   @Test
   public void failingUTTest() throws Exception {

      if (isWindows) return;

      assertFalse(checkLogMessage(4000,  "Press the Enter key to run tests on demand."));

      FileUtils.deleteDirectory(new File(tempProj, "src/test/java/com/demo/test/"));
      FileUtils.copyFile(new File(testResources, "HelloFailTest.java"), new File(tempProj, "src/test/java/com/demo/test/HelloFailTest.java"));
      FileUtils.copyFile(new File(testResources, "HelloIT.java"), new File(tempProj, "src/test/java/it/com/demo/test/HelloIT.java"));

      Thread.sleep(4000); // wait for compilation

      writer.write("\n");
      writer.flush();

      assertFalse(checkLogMessage(10000,  "Running com.demo.test.HelloFailTest"));
      assertFalse(checkLogMessage(10000,  "Tests run: 1, Failures: 1"));
      assertFalse(checkLogMessage(10000,  "Unit tests finished."));

      assertFalse(checkLogMessage(10000,  "Running it.com.demo.test.HelloIT"));
      assertFalse(checkLogMessage(10000,  "Tests run: 1, Failures: 0"));
      assertFalse(checkLogMessage(10000,  "Integration tests finished."));
   }

   @Test
   public void failingITTest() throws Exception {

      if (isWindows) return;

      assertFalse(checkLogMessage(4000,  "Press the Enter key to run tests on demand."));

      FileUtils.deleteDirectory(new File(tempProj, "src/test/java/com/demo/test/"));
      FileUtils.copyFile(new File(testResources, "HelloTest.java"), new File(tempProj, "src/test/java/com/demo/test/HelloTest.java"));
      FileUtils.copyFile(new File(testResources, "HelloFailIT.java"), new File(tempProj, "src/test/java/it/com/demo/test/HelloFailIT.java"));

      Thread.sleep(4000); // wait for compilation

      writer.write("\n");
      writer.flush();

      assertFalse(checkLogMessage(10000,  "Running com.demo.test.HelloTest"));
      assertFalse(checkLogMessage(10000,  "Tests run: 1, Failures: 0"));
      assertFalse(checkLogMessage(10000,  "Unit tests finished."));

      assertFalse(checkLogMessage(10000,  "Running it.com.demo.test.HelloFailIT"));
      assertFalse(checkLogMessage(10000,  "Tests run: 1, Failures: 1"));
      assertFalse(checkLogMessage(10000,  "Integration tests finished."));
   }

}
