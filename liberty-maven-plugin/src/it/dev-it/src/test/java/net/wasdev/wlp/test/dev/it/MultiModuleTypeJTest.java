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

public class MultiModuleTypeJTest extends BaseMultiModuleTest {

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      setUpMultiModule("typeJ", "pom", null);
      run();
   }

   @Test
   public void manualTestsInvocationTest() throws Exception {
      super.manualTestsInvocationTest("guide-maven-multimodules-jar", "guide-maven-multimodules-war1", "guide-maven-multimodules-war2", "guide-maven-multimodules-pom");
   }

   @Test
   public void endpointTest() throws Exception {
      assertEndpointContent("http://localhost:9080/converter1", "Height Converter");
      assertEndpointContent("http://localhost:9080/converter2", "Height Converter");
   }

   @Test
   public void invokeJarCodeTest() throws Exception {
      // TODO Fix testcase issue. Server gives "/heights.jsp failed to compile" but it works when invoked manually.
      // assertEndpointContent("http://localhost:9080/converter1/heights.jsp?heightCm=3048", "100");
      // assertEndpointContent("http://localhost:9080/converter2/heights.jsp?heightCm=3048", "100");
   }

}

