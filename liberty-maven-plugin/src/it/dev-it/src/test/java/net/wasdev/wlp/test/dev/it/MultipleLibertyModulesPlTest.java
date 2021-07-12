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
import static org.junit.Assert.assertEquals;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import org.apache.maven.shared.utils.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class MultipleLibertyModulesPlTest extends BaseMultiModuleTest {

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      setUpMultiModule("multipleLibertyModules", "ear1", null);
   }

   @Test
   public void multipleLibertyModulesPlTest() throws Exception {
      String mavenPluginCommand = "mvn io.openliberty.tools:liberty-maven-plugin:"+System.getProperty("mavenPluginVersion")+":dev -pl ear1 -am";

      StringBuilder command = new StringBuilder(mavenPluginCommand);
      ProcessBuilder builder = buildProcess(command.toString());

      builder.redirectOutput(logFile);
      builder.redirectError(logFile);
      process = builder.start();
      assertTrue(process.isAlive());

      OutputStream stdin = process.getOutputStream();

      writer = new BufferedWriter(new OutputStreamWriter(stdin));

      assertTrue(getLogTail(), verifyLogMessageExists("CWWKF0011I", 120000));
      assertTrue(verifyLogMessageExists("Liberty is running in dev mode.", 60000));
   }

}

