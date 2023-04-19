/*******************************************************************************
 * (c) Copyright IBM Corporation 2021, 2023.
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
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class MultipleConcurrentLibertyModulesPlTest extends BaseMultiModuleTest {

   static BufferedWriter writer2;
   static Process process2;
   static File logFile2;

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      setUpMultiModule("multipleLibertyModules", null /* this param is not used in this scenario */, null);
   }

   @Test
   public void multipleLibertyModulesPlTest() throws Exception {
      // Start first project
      String mavenPluginCommand = "mvn io.openliberty.tools:liberty-maven-plugin:"+System.getProperty("mavenPluginVersion")+":dev -pl ear1 -am -DdebugPort=7777";

      StringBuilder command = new StringBuilder(mavenPluginCommand);
      ProcessBuilder builder = buildProcess(command.toString());

      builder.redirectOutput(logFile);
      builder.redirectError(logFile);
      process = builder.start();
      assertTrue(process.isAlive());

      OutputStream stdin = process.getOutputStream();

      writer = new BufferedWriter(new OutputStreamWriter(stdin));



      // Start second project
      logFile2 = new File(basicDevProj, "logFile2.txt");
      assertTrue(logFile2.createNewFile());

      String mavenPluginCommand2 = "mvn io.openliberty.tools:liberty-maven-plugin:"+System.getProperty("mavenPluginVersion")+":dev -pl ear2 -am -DdebugPort=7778";

      StringBuilder command2 = new StringBuilder(mavenPluginCommand2);
      ProcessBuilder builder2 = buildProcess(command2.toString());

      builder2.redirectOutput(logFile2);
      builder2.redirectError(logFile2);
      process2 = builder2.start();
      assertTrue(process2.isAlive());

      OutputStream stdin2 = process2.getOutputStream();

      writer2 = new BufferedWriter(new OutputStreamWriter(stdin2));


      // Check both dev mode instances
      assertTrue(getLogTail(logFile), verifyLogMessageExists("CWWKF0011I", 120000, logFile));
      assertTrue(verifyLogMessageExists("Liberty is running in dev mode.", 60000, logFile));

      assertTrue(getLogTail(logFile2), verifyLogMessageExists("CWWKF0011I", 120000, logFile2));
      assertTrue(verifyLogMessageExists("Liberty is running in dev mode.", 60000, logFile2));

      Thread.sleep(5000);

      // check endpoints on both projects
      assertEndpointContent("http://localhost:9080/converter", "Height Converter");
      assertEndpointContent("http://localhost:9081/converter", "Height Converter");

      // invoke upstream java code
      assertEndpointContent("http://localhost:9080/converter/heights.jsp?heightCm=3048", "100");
      assertEndpointContent("http://localhost:9081/converter/heights.jsp?heightCm=3048", "100");

      // test modify a Java file in an upstream module
      modifyJarClass();

      Thread.sleep(5000);

      assertEndpointContent("http://localhost:9080/converter/heights.jsp?heightCm=3048", "200");
      assertEndpointContent("http://localhost:9081/converter/heights.jsp?heightCm=3048", "200");
   }

   @AfterClass
   public static void cleanUpAfterClass() throws Exception {
      stopProcess(writer, process, logFile);
      stopProcess(writer2, process2, logFile2);

      if (tempProj != null && tempProj.exists()) {
         FileUtils.deleteDirectory(tempProj);
      }

      if (logFile != null && logFile.exists()) {
         assertTrue(logFile.delete());
      }

      if (logFile2 != null && logFile2.exists()) {
         assertTrue(logFile2.delete());
      }
   }

   private static void stopProcess(BufferedWriter writer, Process process, File logFile) throws IOException, InterruptedException, FileNotFoundException, IllegalThreadStateException {
      // shut down dev mode
      if (writer != null) {
         try {
            writer.write("exit\n"); // trigger dev mode to shut down

            writer.flush();
         } catch (IOException e) {
         } finally {
            try {
               writer.close();
            } catch (IOException io) {
            }
         }

         process.waitFor(120, TimeUnit.SECONDS);

         // test that dev mode has stopped running
         assertTrue(verifyLogMessageExists("CWWKE0036I", 20000, logFile));
      }
   }

}

