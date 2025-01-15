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

public class DevRecompileWithDefaultExecutionIdTest extends BaseDevTest {

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      setUpBeforeClass(null, "../resources/basic-dev-project-with-default-execution-id", true, false, null, null);
   }

   @AfterClass
   public static void cleanUpAfterClass() throws Exception {
      BaseDevTest.cleanUpAfterClass(true);
   }

   @Test
   public void validateRunExecutionNotSkipped() throws Exception {
      String mavenPluginCommand = "mvn compile io.openliberty.tools:liberty-maven-plugin:"+System.getProperty("mavenPluginVersion")+":dev";

      StringBuilder command = new StringBuilder(mavenPluginCommand);
      ProcessBuilder builder = buildProcess(command.toString());

      builder.redirectOutput(logFile);
      builder.redirectError(logFile);
      process = builder.start();
      assertTrue(process.isAlive());

      OutputStream stdin = process.getOutputStream();

      writer = new BufferedWriter(new OutputStreamWriter(stdin));

      assertTrue(getLogTail(), verifyLogMessageExists("Nothing to compile - all classes are up to date.", 120000));
      // Check that the correct execution id is picked up
      // in this case, we are not passing any execution id in pom.xml, hence default execution id will be taken up
      assertTrue(getLogTail(), verifyLogMessageExists("Running maven-compiler-plugin:compile#default-compile", 120000));
   }
}