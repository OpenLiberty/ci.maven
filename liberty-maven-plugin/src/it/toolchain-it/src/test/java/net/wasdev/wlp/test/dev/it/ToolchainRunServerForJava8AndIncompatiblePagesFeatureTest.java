/*******************************************************************************
 * (c) Copyright IBM Corporation 2025.
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

import org.apache.commons.lang3.JavaVersion;
import org.apache.commons.lang3.SystemUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Negative Test scenario
 * This test uses feature pages-3.1 in server.xml, but we setup jdkToolchain with java 8, which causes incompatibility issues in server run
 */
public class ToolchainRunServerForJava8AndIncompatiblePagesFeatureTest extends BaseToolchainTest {

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      setUpBeforeClass(null, "../resources/basic-toolchain-project-fail-on-java8", null, null, "run");
   }

   @AfterClass
   public static void cleanUpAfterClass() throws Exception {
   //  BaseToolchainTest.cleanUpAfterClass(false, false);
   }

   @Test
   public void runServerTest() throws Exception {
      tagLog("##runServerTestWithJava8Pages3 start");
      assertTrue(getLogTail(), verifyLogMessageExists(TOOLCHAIN_INITIALIZED, 10000));
      if(SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_11)) {
         assertTrue(getLogTail(), verifyLogMessageExists(String.format(TOOLCHAIN_CONFIGURED_FOR_GOAL, "create"), 10000));
         assertTrue(getLogTail(), verifyLogMessageExists(String.format(TOOLCHAIN_CONFIGURED_FOR_GOAL, "run"), 10000));
         assertTrue(getLogTail(), verifyLogMessageExists(String.format(JAVA_11_SE_REQUIRED_FOR_FEATURE, "pages-3.1"), 10000));
      }else {
         // throws compilation error since jakartaee10 is not supported in java 1.8
         assertTrue(getLogTail(), verifyLogMessageExists("bad class file:", 10000));
      }
      tagLog("##runServerTestWithJava8Pages3 end");
   }
}
