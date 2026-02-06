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

import static org.junit.Assert.assertTrue;
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
      //debugPort set as 7077
      assertTrue(verifyLogMessageExists("Listening for transport dt_socket at address: 7077", 20000) || verifyLogMessageExists("The debug port 7077 is not available.",20000));
      assertTrue(verifyLogMessageExists("Recompile skipped for dev-sample-proj since earlier compilation is successful", 20000));
      assertTrue(verifyLogMessageExists("Tests will run automatically", 20000));
   
      testModifyJavaFile();

      assertTrue(verifyLogMessageExists("Unit tests finished.", 2000));
      assertTrue(verifyLogMessageExists("Integration tests finished.", 2000));   
   }

}
