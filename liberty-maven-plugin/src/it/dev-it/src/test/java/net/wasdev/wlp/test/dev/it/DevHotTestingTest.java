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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class DevHotTestingTest extends BaseDevTest {

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      setUpBeforeClass("-DhotTests=true");
   }

   @AfterClass
   public static void cleanUpAfterClass() throws Exception {
      BaseDevTest.cleanUpAfterClass();
   }

   @Test
   public void autoTestsInvocationTest() throws Exception {
//      Thread.sleep(10000);
      assertFalse(checkLogMessage(20000,  "Tests will run automatically"));
   
      testModifyJavaFile();

      assertFalse(checkLogMessage(2000,  "Unit tests finished."));
      assertFalse(checkLogMessage(2000,  "Integration tests finished."));   
   }

}
