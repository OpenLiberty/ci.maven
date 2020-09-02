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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.maven.shared.utils.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class BaseDevTest {

   static File tempProj;
   static File basicDevProj;
   static File logFile;
   static File targetDir;
   static File pom;
   static BufferedWriter writer;
   static Process process;

   protected static void setUpBeforeClass(String devModeParams) throws IOException, InterruptedException, FileNotFoundException {
   	setUpBeforeClass(devModeParams, "../resources/basic-dev-project");
   }

   protected static void setUpBeforeClass(String devModeParams, boolean isDevMode) throws IOException, InterruptedException, FileNotFoundException {
      setUpBeforeClass(devModeParams, "../resources/basic-dev-project", isDevMode);
   }

   protected static void setUpBeforeClass(String devModeParams, String projectRoot) throws IOException, InterruptedException, FileNotFoundException {
      setUpBeforeClass(devModeParams, projectRoot, true);
   }

   protected static void setUpBeforeClass(String params, String projectRoot, boolean isDevMode) throws IOException, InterruptedException, FileNotFoundException {
      basicDevProj = new File(projectRoot);

      tempProj = Files.createTempDirectory("temp").toFile();
      assertTrue(tempProj.exists());

      assertTrue(basicDevProj.exists());

      FileUtils.copyDirectoryStructure(basicDevProj, tempProj);
      assertTrue(tempProj.listFiles().length > 0);

      logFile = new File(basicDevProj, "logFile.txt");
      assertTrue(logFile.createNewFile());

      pom = new File(tempProj, "pom.xml");
      assertTrue(pom.exists());

      replaceVersion();

      startProcess(params, isDevMode);
   }

   private static void startProcess(String params, boolean isDevMode) throws IOException, InterruptedException, FileNotFoundException {
      // run dev mode on project
      String goal;
      if(isDevMode) {
         goal = "dev";
      } else {
         goal = "run";
      }

      StringBuilder command = new StringBuilder("mvn liberty:" + goal);
      if (params != null) {
         command.append(" " + params);
      }
      ProcessBuilder builder = buildProcess(command.toString());

      builder.redirectOutput(logFile);
      builder.redirectError(logFile);
      process = builder.start();
      assertTrue(process.isAlive());

      OutputStream stdin = process.getOutputStream();

      writer = new BufferedWriter(new OutputStreamWriter(stdin));

      // check that the server has started
      Thread.sleep(5000);
      assertTrue(verifyLogMessageExists("CWWKF0011I", 120000));
      if (isDevMode) {
         assertTrue(verifyLogMessageExists("Enter key to run tests on demand", 60000));
      }

      // verify that the target directory was created
      targetDir = new File(tempProj, "target");
      assertTrue(targetDir.exists());
   }

   protected static void cleanUpAfterClass() throws Exception {
      cleanUpAfterClass(true);
   }

   protected static void cleanUpAfterClass(boolean isDevMode) throws Exception {
      stopProcess(isDevMode);

      if (tempProj != null && tempProj.exists()) {
         FileUtils.deleteDirectory(tempProj);
      }

      if (logFile != null && logFile.exists()) {
         assertTrue(logFile.delete());
      }
   }

   private static void stopProcess(boolean isDevMode) throws IOException, InterruptedException, FileNotFoundException, IllegalThreadStateException {
      // shut down dev mode
      if (writer != null) {
         if(isDevMode) {
            writer.write("exit\n"); // trigger dev mode to shut down
         }
         else {
            process.destroy(); // stop run
         }
         writer.flush();
         writer.close();

         process.waitFor(120, TimeUnit.SECONDS);
         process.exitValue();

         // test that dev mode has stopped running
         assertTrue(verifyLogMessageExists("CWWKE0036I", 20000));
      }
   }

   protected static void testModifyJavaFile() throws IOException, InterruptedException {
      // modify a java file
      File srcHelloWorld = new File(tempProj, "src/main/java/com/demo/HelloWorld.java");
      File targetHelloWorld = new File(targetDir, "classes/com/demo/HelloWorld.class");
      assertTrue(srcHelloWorld.exists());
      assertTrue(targetHelloWorld.exists());

      long lastModified = targetHelloWorld.lastModified();
      String str = "// testing";
      BufferedWriter javaWriter = new BufferedWriter(new FileWriter(srcHelloWorld, true));
      javaWriter.append(' ');
      javaWriter.append(str);

      javaWriter.close();

      Thread.sleep(5000); // wait for compilation
      boolean wasModified = targetHelloWorld.lastModified() > lastModified;
      assertTrue(wasModified);
   }

   private static boolean readFile(String str, File file) throws FileNotFoundException, IOException {
      BufferedReader br = new BufferedReader(new FileReader(file));
      String line = br.readLine();
      try {
         while (line != null) {
            if (line.contains(str)) {
               return true;
            }
            line = br.readLine();
         }
      } finally {
         br.close();
      }
      return false;
   }

   private static ProcessBuilder buildProcess(String processCommand) {
      ProcessBuilder builder = new ProcessBuilder();
      builder.directory(tempProj);

      String os = System.getProperty("os.name");
      if (os != null && os.toLowerCase().startsWith("windows")) {
         builder.command("CMD", "/C", processCommand);
      } else {
         builder.command("bash", "-c", processCommand);
      }
      return builder;
   }

   private static void replaceVersion() throws IOException {
      String pluginVersion = System.getProperty("mavenPluginVersion");
      replaceString("SUB_VERSION", pluginVersion, pom);
      String runtimeVersion = System.getProperty("runtimeVersion");
      replaceString("RUNTIME_VERSION", runtimeVersion, pom);
   }

   protected static void replaceString(String str, String replacement, File file) throws IOException {
      Path path = file.toPath();
      Charset charset = StandardCharsets.UTF_8;

      String content = new String(Files.readAllBytes(path), charset);

      content = content.replaceAll(str, replacement);
      Files.write(path, content.getBytes(charset));
   }

   protected static boolean verifyLogMessageExists(String message, int timeout)
         throws InterruptedException, FileNotFoundException, IOException {
      int waited = 0;
      int sleep = 10;
      while (waited <= timeout) {
         Thread.sleep(sleep);
         waited += sleep;
         if (readFile(message, logFile)) {
            return true;
         }
      }
      return false;
   }
}
