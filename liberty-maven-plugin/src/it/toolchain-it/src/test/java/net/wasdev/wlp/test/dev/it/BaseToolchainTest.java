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

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.io.FileUtils;

import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class BaseToolchainTest {

    static String customLibertyModule;
    static String customPomModule;
    static File tempProj;
    static File basicDevProj;
    static File logFile;
    static File logErrorFile;
    static File targetDir;
    static File pom;
    static BufferedWriter writer;
    static Process process;
    static final String TOOLCHAIN_INITIALIZED = "CWWKM4100I: Using toolchain from build context";
    static final String TOOLCHAIN_CONFIGURED_FOR_GOAL = "CWWKM4101I: %s goal is using Toolchain JDK in located at";
    static final String JAVA_11_SE_REQUIRED_FOR_FEATURE = "CWWKF0032E: The %s feature requires a minimum Java runtime environment version of JavaSE 11";

    protected static void setUpBeforeClass(String params, String projectRoot, String libertyConfigModule, String pomModule, String goal) throws IOException, InterruptedException, FileNotFoundException {
        customLibertyModule = libertyConfigModule;
        customPomModule = pomModule;

        basicDevProj = new File(projectRoot);

        tempProj = Files.createTempDirectory("temp").toFile();
        assertTrue("temp directory does not exist", tempProj.exists());

        assertTrue(projectRoot + " directory does not exist", basicDevProj.exists());

        FileUtils.copyDirectory(basicDevProj, tempProj);
        assertTrue("temp directory does not contain expected copied files from " + projectRoot, Objects.requireNonNull(tempProj.listFiles()).length > 0);

        // in case cleanup was not successful, try to delete the various log files so we can proceed
        logFile = new File(basicDevProj, "logFile.txt");
        if (logFile.exists()) {
            assertTrue("Could not delete log file: " + logFile.getCanonicalPath(), logFile.delete());
        }
        assertTrue("log file already existed: " + logFile.getCanonicalPath(), logFile.createNewFile());
        logErrorFile = new File(basicDevProj, "logErrorFile.txt");
        if (logErrorFile.exists()) {
            assertTrue("Could not delete logError file: " + logErrorFile.getCanonicalPath(), logErrorFile.delete());
        }
        assertTrue("logError file already existed: " + logErrorFile.getCanonicalPath(), logErrorFile.createNewFile());

        if (customPomModule == null) {
            pom = new File(tempProj, "pom.xml");
        } else {
            pom = new File(new File(tempProj, customPomModule), "pom.xml");
        }
        assertTrue(pom.getCanonicalPath() + " file does not exist", pom.exists());

        replaceVersion();
        startProcess(params, "mvn liberty:", goal);
    }

    protected static void startProcess(String params, String mavenPluginCommand, String goal) throws IOException, InterruptedException, FileNotFoundException {
        StringBuilder command = new StringBuilder(mavenPluginCommand + goal);
        if (params != null) {
            command.append(" " + params);
        }
        ProcessBuilder builder = buildProcess(command.toString());

        builder.redirectOutput(logFile);
        builder.redirectError(logErrorFile);
        if (customPomModule != null) {
            builder.directory(new File(tempProj, customPomModule));
        }
        process = builder.start();
        assertTrue("process is not alive", process.isAlive());

        OutputStream stdin = process.getOutputStream();

        writer = new BufferedWriter(new OutputStreamWriter(stdin));
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
            for (int i = reversedLines.size() - 1; i >= 0; i--) {
                result.append(reversedLines.get(i) + "\n");
            }
            return "Last " + numLines + " lines of log at " + log.getAbsolutePath() + ":\n" +
                    "===================== START =======================\n" +
                    result.toString() +
                    "====================== END ========================\n";
        } finally {
            if (object != null) {
                object.close();
            }
        }
    }

    private static void stopProcess(boolean isDevMode, boolean checkForShutdownMessage) throws IOException, InterruptedException, FileNotFoundException, IllegalThreadStateException {
        // shut down dev mode
        if (writer != null) {
            int serverStoppedOccurrences = countOccurrences("CWWKE0036I", logFile);

            try {
                if (isDevMode) {
                    writer.write("exit\n"); // trigger dev mode to shut down
                } else {
                    process.destroyForcibly(); // stop run
                    String os = System.getProperty("os.name");
                    if (os != null && os.toLowerCase().startsWith("windows")) {
                        // wait for some time in windows
                        Thread.sleep(30000);
                    }
                }
                writer.flush();

            } catch (IOException ignored) {
            } finally {
                try {
                    writer.close();
                } catch (IOException ignored) {
                }
            }
            try {
                if (process.isAlive()) {
                    process.waitFor(120, TimeUnit.SECONDS);
                }
            } catch (InterruptedException ignored) {
            }

            // test that the server has shut down
            if (checkForShutdownMessage) {
                assertTrue(getLogTail(), verifyLogMessageExists("CWWKE0036I", 20000, logFile, ++serverStoppedOccurrences));
            }
        }
    }

    protected static void cleanUpAfterClass(boolean isDevMode, boolean checkForShutdownMessage) throws Exception {

        stopProcess(isDevMode, checkForShutdownMessage);
        if (tempProj != null && tempProj.exists()) {
            FileUtils.deleteDirectory(tempProj);
        }
        if (logFile != null && logFile.exists()) {
            assertTrue("Could not delete log file: " + logFile.getCanonicalPath(), logFile.delete());
        }
        if (logErrorFile != null && logErrorFile.exists()) {
            assertTrue("Could not delete logError file: " + logErrorFile.getCanonicalPath(), logErrorFile.delete());
        }
    }

    protected static boolean readFile(String str, File file) throws IOException {
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

    protected static ProcessBuilder buildProcess(String processCommand) {
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

    /**
     * Replaces all occurrences of a regex pattern in a file.
     *
     * @param str         the regex pattern to search for
     * @param replacement the replacement string (supports regex syntax like $1 for backreferences)
     * @param file        the file to modify
     * @throws IOException if the file cannot be read or written
     */
    protected static void replaceString(String str, String replacement, File file) throws IOException {
        Path path = file.toPath();
        Charset charset = StandardCharsets.UTF_8;
        String content = new String(Files.readAllBytes(path), charset);

        content = content.replaceAll(str, replacement);
        Files.write(path, content.getBytes(charset));
    }

    protected static boolean verifyLogMessageExists(String message, int timeout)
            throws InterruptedException, IOException {
        return verifyLogMessageExists(message, timeout, logFile);
    }

    protected static boolean verifyLogMessageExists(String message, int timeout, File log)
            throws InterruptedException, IOException {
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

    protected static void tagLog(String line) throws Exception {
        writer.write(line + "\n");
        writer.flush();
    }

    /**
     * Count number of lines that contain the given string
     */
    protected static int countOccurrences(String str, File file) throws IOException {
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
            } catch (IOException e) {
            }
        }
    };
}
