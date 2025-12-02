package net.wasdev.wlp.test.it;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.io.FileUtils;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class BaseDevTest {

   static String customLibertyModule;
   static String customPomModule;
   static File basicDevProj;
   static File logFile;
   static File logErrorFile;
   static File targetDir;
   static File pom;
   static BufferedWriter writer;
   static Process process;

   /**
    * Setup and optionally start dev/run
    *
    * @param params Params for the dev/run goal
    * @param projectRoot The Maven project root
    * @param isDevMode Use dev if true, use run if false.  Ignored if startProcessDuringSetup is false.
    * @param startProcessDuringSetup If this method should start the actual dev/run process
    * @param libertyConfigModule For multi module project, the module where Liberty configuration is located
    * @param pomModule For multi module project, the module where the pom is located.  If null, use the project root.
    * @throws IOException
    * @throws InterruptedException
    * @throws FileNotFoundException
    */
   protected static void setUpBeforeClass(String params, String projectRoot, boolean isDevMode, boolean startProcessDuringSetup, 
                                          String libertyConfigModule, String pomModule) throws IOException, InterruptedException, FileNotFoundException {
      customLibertyModule = libertyConfigModule;
      customPomModule = pomModule;

      basicDevProj = new File(projectRoot);

      assertTrue(projectRoot+" directory does not exist", basicDevProj.exists());

      logFile = new File(basicDevProj, "logFile.txt");
      logErrorFile = new File(basicDevProj, "logErrorFile.txt");
      
      if (customPomModule == null) {
         pom = new File(basicDevProj, "pom.xml");
      } else {
         pom = new File(new File(basicDevProj, customPomModule), "pom.xml");
      }
      assertTrue(pom.getCanonicalPath()+" file does not exist", pom.exists());

      replaceVersion();

      if (startProcessDuringSetup) {
         startProcess(params, isDevMode);
      }
   }

   protected static void startProcess(String params, boolean isDevMode) throws IOException, InterruptedException, FileNotFoundException {
      startProcess(params, isDevMode, "mvn liberty:");
   }

   protected static void startProcess(String params, boolean isDevMode, String mavenPluginCommand) throws IOException, InterruptedException, FileNotFoundException {
      startProcess(params, isDevMode, mavenPluginCommand, true);
   }

   protected static void startProcess(String params, boolean isDevMode, String mavenPluginCommand, boolean verifyServerStart) throws IOException, InterruptedException, FileNotFoundException {
      // run dev mode on project
      String goal;
      if(isDevMode) {
         goal = "dev";
      } else {
         goal = "run";
      }

      StringBuilder command = new StringBuilder(mavenPluginCommand + goal);
      if (params != null) {
         command.append(" " + params);
      }
      ProcessBuilder builder = buildProcess(command.toString());

      builder.redirectOutput(logFile);
      builder.redirectError(logErrorFile);
      process = builder.start();
      assertTrue("process is not alive", process.isAlive());

      OutputStream stdin = process.getOutputStream();

      writer = new BufferedWriter(new OutputStreamWriter(stdin));

      if (verifyServerStart) {
         // check that the server has started
         assertTrue(getLogTail(), verifyLogMessageExists("CWWKF0011I", 120000));
         if (isDevMode) {
            assertTrue(verifyLogMessageExists("Liberty is running in dev mode.", 60000));
         }

         // verify that the target directory was created
         if (customLibertyModule == null) {
            targetDir = new File(basicDevProj, "target");
         } else {
            targetDir = new File(new File(basicDevProj, customLibertyModule), "target");
         }
         assertTrue("target directory does not exist: "+targetDir.getCanonicalPath(), targetDir.exists());
      }
   }

   protected static String getLogTail() throws IOException {
      return getLogTail(logFile);
   }

   protected static String getLogTail(File log) throws IOException {
      int numLines = 100;
      ReversedLinesFileReader object = null;
      try {
         object = new ReversedLinesFileReader(log, StandardCharsets.UTF_8);
         List<String> reversedLines = new ArrayList<String>();

         for (int i = 0; i < numLines; i++) {
            String line = object.readLine();
            if (line == null) {
               break;
            }
            reversedLines.add(line);
         }
         StringBuilder result = new StringBuilder();
         for (int i = reversedLines.size() - 1; i >=0; i--) {
            result.append(reversedLines.get(i) + "\n");
         }
         return "Last "+numLines+" lines of log at "+log.getAbsolutePath()+":\n" + 
            "===================== START =======================\n" + 
            result.toString() +
            "====================== END ========================\n";
      } finally {
         if (object != null) {
            object.close();
         }
      }
   }

   protected static void cleanUpAfterClass() throws Exception {
      cleanUpAfterClass(true);
   }

   protected static void cleanUpAfterClass(boolean isDevMode) throws Exception {
      cleanUpAfterClass(isDevMode, true);
   }

   protected static void cleanUpAfterClass(boolean isDevMode, boolean checkForShutdownMessage) throws Exception {
      stopProcess(isDevMode, checkForShutdownMessage);

      if (logFile != null && logFile.exists()) {
          assertTrue("Could not delete log file: "+logFile.getCanonicalPath(), logFile.delete());
      }
      if (logErrorFile != null && logErrorFile.exists()) {
          assertTrue("Could not delete logError file: "+logErrorFile.getCanonicalPath(), logErrorFile.delete());
      }
   }

   protected static void clearLogFile() throws Exception {
      if (logFile != null && logFile.exists()) {
         BufferedWriter logWriter = new BufferedWriter(new FileWriter(logFile));
         logWriter.close();
      }
   }

   private static void stopProcess(boolean isDevMode, boolean checkForShutdownMessage) throws IOException, InterruptedException, FileNotFoundException, IllegalThreadStateException {
      // shut down dev mode
      if (writer != null) {
         int serverStoppedOccurrences = countOccurrences("CWWKE0036I", logFile);

         try {
            if(isDevMode) {
               writer.write("exit\n"); // trigger dev mode to shut down
            } else {
               process.destroy(); // stop run
            }
            writer.flush();

         } catch (IOException e) {
         } finally {
            try {
               writer.close();
            } catch (IOException io) {
            }
         }

         try {
            process.waitFor(120, TimeUnit.SECONDS);
         } catch (InterruptedException e) {
         }

         // test that the server has shut down
         if (checkForShutdownMessage) {
            assertTrue(getLogTail(), verifyLogMessageExists("CWWKE0036I", 20000, ++serverStoppedOccurrences));
         }
      }
   }

   protected static ProcessBuilder buildProcess(String processCommand) {
      ProcessBuilder builder = new ProcessBuilder();
      builder.directory(basicDevProj);

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
      return verifyLogMessageExists(message, timeout, logFile);
   }

   protected static boolean verifyLogMessageExists(String message, int timeout, File log)
         throws InterruptedException, FileNotFoundException, IOException {
      int waited = 0;
      int sleep = 10;
      while (waited <= timeout) {
         Thread.sleep(sleep);
         waited += sleep;
         if (readFile(message, log)) {
            return true;
         }
      }
      return false;
   }

   protected static boolean verifyLogMessageExists(String message, int timeout, int occurrences)
         throws InterruptedException, FileNotFoundException, IOException {
      return verifyLogMessageExists(message, timeout, logFile, occurrences);
   }

   protected static boolean verifyLogMessageExists(String message, int timeout, File log, int occurrences)
         throws InterruptedException, FileNotFoundException, IOException {
      int waited = 0;
      int sleep = 10;
      while (waited <= timeout) {
         Thread.sleep(sleep);
         waited += sleep;
         if (countOccurrences(message, log) == occurrences) {
            return true;
         }
      }
      return false;
   }   

   protected static boolean readFile(String str, File file) throws FileNotFoundException, IOException {
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

   /**
    * Count number of lines that contain the given string
    */
    protected static int countOccurrences(String str, File file) throws FileNotFoundException, IOException {
      int occurrences = 0;
      BufferedReader br = new BufferedReader(new FileReader(file));
      String line = br.readLine();
      try {
         while (line != null) {
            if (line.contains(str)) {
               occurrences++;
            }
            line = br.readLine();
         }
      } finally {
         br.close();
      }
      return occurrences;
   }

    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override
        protected void failed(Throwable thr, Description description) {
            try {
                System.out.println("Failure log in " + logFile + ", tail of contents = " + getLogTail(logFile));
            } catch (IOException e) {}
        }
    };
}
