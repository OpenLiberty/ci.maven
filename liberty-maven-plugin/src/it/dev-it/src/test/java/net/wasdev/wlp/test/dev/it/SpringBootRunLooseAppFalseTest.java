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

public class SpringBootRunLooseAppFalseTest extends BaseDevTest {

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      setUpBeforeClass(null, "../resources/springboot-package-project", false, false, null, null);
   }

   @AfterClass
   public static void cleanUpAfterClass() throws Exception {
      BaseDevTest.cleanUpAfterClass(false);
   }

   /**
    * The liberty:run mojo, as an "all-in-one" goal, assumes it needs to do a package step if "loose" is disabled (set to false), and so it calls the war:war goal.
    * This wrongly replaces the SpringBoot-repackaged WAR with a plain WAR, leading to this error.
    * A fix is added to skip repackaging into jar/war if "spring-boot-project" is specified as deployPackage
    * 
    * @throws Exception
    */
   @Test
   public void validateRunExecutionNotSkipped() throws Exception {
	   String mavenPluginCommand = "mvn package io.openliberty.tools:liberty-maven-plugin:"+System.getProperty("mavenPluginVersion")+":run  -DlooseApplication=false  -DdeployPackages=spring-boot-project";

       StringBuilder command = new StringBuilder(mavenPluginCommand);
       ProcessBuilder builder = buildProcess(command.toString());

       builder.redirectOutput(logFile);
       builder.redirectError(logFile);
       process = builder.start();
       assertTrue(process.isAlive());

       OutputStream stdin = process.getOutputStream();

       writer = new BufferedWriter(new OutputStreamWriter(stdin));
	      
	   // Make sure we are skipping the repackaging for the project
	   assertTrue(getLogTail(), verifyLogMessageExists("Skipping project repackaging as deploy package is configured as spring-boot-project", 30000));

       // Check that the springboot application has started
       assertTrue(getLogTail(), verifyLogMessageExists("CWWKZ0001I", 120000));
	   // Check that the server has started
       assertTrue(getLogTail(), verifyLogMessageExists("CWWKF0011I", 10000));
   }
}
