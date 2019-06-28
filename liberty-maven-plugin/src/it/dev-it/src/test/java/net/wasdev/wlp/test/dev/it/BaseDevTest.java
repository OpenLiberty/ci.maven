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

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import org.apache.maven.shared.utils.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BaseDevTest {

   File tempProj;
   File basicDevProj;

   @Before
   public void setUp() throws Exception {
      tempProj = Files.createTempDirectory("temp").toFile();
      assertTrue(tempProj.exists());
      
      basicDevProj = new File("../resources/basic-dev-project");
      assertTrue(basicDevProj.exists());

      FileUtils.copyDirectoryStructure(basicDevProj, tempProj);
      assertTrue(tempProj.listFiles().length > 0);
   }

   @After
   public void cleanUp() throws Exception {
      if (tempProj != null && tempProj.exists()) {
         FileUtils.deleteDirectory(tempProj);
      }
   }

   @Test
   public void basicTest() throws Exception {

      // run dev mode on project
      ProcessBuilder builder = new ProcessBuilder();
      builder.directory(tempProj);
      String processCommand = "mvn liberty:dev";

      String os = System.getProperty("os.name");
      if (os != null && os.toLowerCase().startsWith("windows")) {
         builder.command("CMD", "/C", processCommand);
      } else {
         builder.command("bash", "-c", processCommand);
      }

      File logFile = new File(basicDevProj, "/logFile.txt");
      Files.write(logFile.toPath(), "".getBytes());

      builder.redirectOutput(logFile);
      builder.redirectError(logFile);
      Process process = builder.start();
      assertTrue(process.isAlive());

      OutputStream stdin = process.getOutputStream();

      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
      Thread.sleep(40000); // wait for dev mode to start up

      // check that the server has started
      int startTimeout = 100000;
      int startWaited = 0;
      boolean startFlag = false;
      while (!startFlag && startWaited <= startTimeout) {
         int sleep = 10;
         Thread.sleep(sleep);
         startWaited += sleep;
         if (readFile("CWWKF0011I", logFile) == true) {
            startFlag = true;
         }
     }
   
      // verify that the target directory was created
      File targetDir = new File(tempProj, "/target");
      assertTrue(targetDir.exists());

      // modify a java file
      File srcHelloWorld = new File(tempProj, "/src/main/java/com/demo/HelloWorld.java");
      File targetHelloWorld = new File(targetDir, "/classes/com/demo/HelloWorld.class");
      assertTrue(srcHelloWorld.exists());
      assertTrue(targetHelloWorld.exists());

      long lastModified = targetHelloWorld.lastModified();
      String str = "// testing";
      BufferedWriter javaWriter = new BufferedWriter(new FileWriter(srcHelloWorld, true));
      javaWriter.append(' ');
      javaWriter.append(str);

      javaWriter.close();

      Thread.sleep(2000); // wait for compilation
      boolean wasModified = targetHelloWorld.lastModified() > lastModified;
      assertTrue(wasModified);

      // shut down dev mode
      writer.write("exit"); // trigger dev mode to shut down
      writer.flush();
      writer.close();
      Thread.sleep(2000); // wait for dev mode to shut down

      process.waitFor();

      // test that dev mode has stopped running
      int stopTimeout = 100000;
      int stopWaited = 0;
      boolean stopFlag = false;
      while (!stopFlag && stopWaited <= stopTimeout) {
         int sleep = 10;
         Thread.sleep(sleep);
         stopWaited += sleep;
         if (readFile("CWWKE0036I", logFile) == true) {
            stopFlag = true;
         }
     }
   }

   private boolean readFile(String str, File file) throws FileNotFoundException {
      Scanner scanner = new Scanner(file);

      while (scanner.hasNextLine()) {
         String line = scanner.nextLine();
         if (line.contains(str)) {
            return true;
         }
      }
      return false;
   }
}
