/*******************************************************************************
 * (c) Copyright IBM Corporation 2022.
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
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.maven.shared.utils.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class BaseScannerTest {

    static File tempProj;
    static File basicProj;
    static File logFile;
    static BufferedWriter writer;
    static Process process;
    static File targetDir;
    static String processOutput = "";
    static File newFeatureFile;
    static File pom;
    static File serverXmlFile;

    static final String GENERATED_FEATURES_FILE_NAME = "generated-features.xml";
    static final String GENERATED_FEATURES_FILE_PATH = "/src/main/liberty/config/configDropins/overrides/" + GENERATED_FEATURES_FILE_NAME;
    static final String TARGET_EE_NULL = "targetJavaEE: null";
    static final String TARGET_MP_NULL = "targetMicroP: null";
    static final String JEE9_UMBRELLA = "<dependency>\n" +
        "        <groupId>jakarta.platform</groupId>\n" +
        "        <artifactId>jakarta.jakartaee-api</artifactId>\n" +
        "        <version>9.1.0</version>\n" +
        "        <scope>provided</scope>\n" +
        "    </dependency>";
    static final String JEE8_UMBRELLA = "<dependency>\n" +
        "        <groupId>jakarta.platform</groupId>\n" +
        "        <artifactId>jakarta.jakartaee-api</artifactId>\n" +
        "        <version>8.0.0</version>\n" +
        "        <scope>provided</scope>\n" +
        "    </dependency>";
    static final String EE8_UMBRELLA = "<dependency>\n" +
        "        <groupId>javax</groupId>\n" +
        "        <artifactId>javaee-api</artifactId>\n" +
        "        <version>8.0</version>\n" +
        "        <scope>provided</scope>\n" +
        "    </dependency>";
    static final String ESA_JEE9_DEPENDENCY = "<dependency>\n" +
        "        <groupId>io.openliberty.features</groupId>\n" +
        "        <artifactId>servlet-5.0</artifactId>\n" +
        "        <type>esa</type>\n" +
        "        <scope>provided</scope>\n" +
        "    </dependency>\n" +
        "    <dependency>\n" +
        "        <groupId>io.openliberty.features</groupId>\n" +
        "        <artifactId>restfulWS-3.0</artifactId>\n" +
        "        <type>esa</type>\n" +
        "        <scope>provided</scope>\n" +
        "    </dependency>";
    // Dependency declared in basic-dev-project7
    static final String MP1_UMBRELLA = "<dependency>\n" +
        "        <groupId>org.eclipse.microprofile</groupId>\n" +
        "        <artifactId>microprofile</artifactId>\n" +
        "        <version>1.4</version>\n" +
        "        <type>pom</type>\n" +
        "        <scope>provided</scope>\n" +
        "    </dependency>";
    // Dependency declared in basic-dev-project8
    static final String MP4_UMBRELLA = "<dependency>\n" +
        "        <groupId>org.eclipse.microprofile</groupId>\n" +
        "        <artifactId>microprofile</artifactId>\n" +
        "        <version>4.1</version>\n" +
        "        <type>pom</type>\n" +
        "        <scope>provided</scope>\n" +
        "    </dependency>";
    // Dependency declared in basic-dev-project9
    static final String MP5_UMBRELLA = "<dependency>\n" +
        "        <groupId>org.eclipse.microprofile</groupId>\n" +
        "        <artifactId>microprofile</artifactId>\n" +
        "        <version>5.0</version>\n" +
        "        <type>pom</type>\n" +
        "        <scope>provided</scope>\n" +
        "    </dependency>";
    static final String ESA_MP_DEPENDENCY = "<dependency>\n" +
        "        <groupId>io.openliberty.features</groupId>\n" +
        "        <artifactId>mpHealth-4.0</artifactId>\n" +
        "        <type>esa</type>\n" +
        "        <scope>provided</scope>\n" +
        "    </dependency>";

    // @Health supported in MP 1, 2 and 3. @Liveness supported in MP 4, 5+
    static final String EE8_CODE_FIX1a = "org.eclipse.microprofile.health.Health;";
    static final String EE8_CODE_FIX1b = "org.eclipse.microprofile.health.Liveness;";
    static final String EE8_CODE_FIX2a = "@Health";
    static final String EE8_CODE_FIX2b = "@Liveness";
    static final String EE8_CODE_FILENAME = "src/main/java/com/demo/HelloWorld.java";

    static Set<String> EE6_FEATURES = new HashSet<String>(Arrays.asList("servlet-3.0","jaxrs-1.1"));

    static Map<String, Set<String>> EE7_FEATURES = new HashMap<>();
    static { // Maven version number and Liberty feature names
        EE7_FEATURES.put("1.2", new HashSet<String>(Arrays.asList("servlet-3.1", "mpHealth-1.0", "jaxrs-2.0")));
        EE7_FEATURES.put("1.3", new HashSet<String>(Arrays.asList("servlet-3.1", "mpHealth-1.0", "jaxrs-2.0")));
        EE7_FEATURES.put("1.4", new HashSet<String>(Arrays.asList("servlet-3.1", "mpHealth-1.0", "jaxrs-2.0")));
    }

    // In v2 of MicroProfile support was changed to Java EE8. If you want to use EE7 use MicroProfile v1.
    static Map<String, Set<String>> EE8_FEATURES_JAVAEE = new HashMap<>();
    static { // Maven version number and Liberty feature names
        EE8_FEATURES_JAVAEE.put("2.0.1", new HashSet<String>(Arrays.asList("servlet-4.0", "mpHealth-1.0", "jaxrs-2.1"))); // 2.0 has an error
        EE8_FEATURES_JAVAEE.put("2.1", new HashSet<String>(Arrays.asList("servlet-4.0", "mpHealth-1.0", "jaxrs-2.1")));
        EE8_FEATURES_JAVAEE.put("2.2", new HashSet<String>(Arrays.asList("servlet-4.0", "mpHealth-1.0", "jaxrs-2.1")));
        EE8_FEATURES_JAVAEE.put("3.0", new HashSet<String>(Arrays.asList("servlet-4.0", "mpHealth-2.0", "jaxrs-2.1")));
        EE8_FEATURES_JAVAEE.put("3.2", new HashSet<String>(Arrays.asList("servlet-4.0", "mpHealth-2.1", "jaxrs-2.1")));
        EE8_FEATURES_JAVAEE.put("3.3", new HashSet<String>(Arrays.asList("servlet-4.0", "mpHealth-2.2", "jaxrs-2.1")));
    }

    // In v4 of MicroProfile support was changed to Jakarta EE8. If you want to use Java EE8 use MicroProfile v3.3.
    static Map<String, Set<String>> EE8_FEATURES_JAKARTA = new HashMap<>();
    static { // Maven version number and Liberty feature names
        EE8_FEATURES_JAKARTA.put("4.0.1", new HashSet<String>(Arrays.asList("servlet-4.0", "mpHealth-3.0", "jaxrs-2.1"))); // 4.0 has an error
        EE8_FEATURES_JAKARTA.put("4.1", new HashSet<String>(Arrays.asList("servlet-4.0", "mpHealth-3.1", "jaxrs-2.1")));
    }
    static Map<String, Set<String>> EE9_FEATURES = new HashMap<>();
    static { // Maven version number and Liberty feature names
        EE9_FEATURES.put("5.0", new HashSet<String>(Arrays.asList("restfulWS-3.0", "servlet-5.0", "mpHealth-4.0")));
    }

    protected static void setUpBeforeTest(String projectRoot) throws IOException, InterruptedException {
        basicProj = new File(projectRoot);
        tempProj = Files.createTempDirectory("temp").toFile();
        assertTrue(tempProj.exists());
        assertTrue(basicProj.exists());

        FileUtils.copyDirectoryStructure(basicProj, tempProj);
        assertTrue(tempProj.listFiles().length > 0);
        logFile = new File(basicProj, "logFile.txt");
        logFile.delete();
        assertTrue(logFile.createNewFile());

        newFeatureFile = new File(tempProj, GENERATED_FEATURES_FILE_PATH);
        pom = new File(tempProj, "pom.xml");
        assertTrue(pom.exists());
        replaceVersion(tempProj);

        serverXmlFile = new File(tempProj, "src/main/liberty/config/server.xml");
        targetDir = new File(tempProj, "target");
    }

    protected static void cleanUpAfterTest() throws Exception {
        if (tempProj != null && tempProj.exists()) {
            FileUtils.deleteDirectory(tempProj);
        }
        if (logFile != null && logFile.exists()) {
            assertTrue(logFile.delete());
        }
    }

    /**
     * Runs process and waits for it to finish
     * Times out after 20 seconds
     * 
     * @param command - command to run
     */
    protected static void runProcess(String processCommand) throws IOException, InterruptedException {
        StringBuilder command = new StringBuilder("mvn " + processCommand);
        ProcessBuilder builder = buildProcess(command.toString());
        builder.redirectOutput(logFile);
        builder.redirectError(logFile);
        process = builder.start();
        assertTrue(process.isAlive());

        OutputStream stdin = process.getOutputStream();
        writer = new BufferedWriter(new OutputStreamWriter(stdin));
        // wait for process to finish max 20 seconds
        process.waitFor(20, TimeUnit.SECONDS);
        assertFalse(process.isAlive());

        // save and print process output
        Path path = logFile.toPath();
        Charset charset = StandardCharsets.UTF_8;
        processOutput = new String(Files.readAllBytes(path), charset);
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

    protected static void replaceVersion(File dir) throws IOException {
        File pomFile = new File(dir, "pom.xml");
        String pluginVersion = System.getProperty("mavenPluginVersion");
        replaceString("SUB_VERSION", pluginVersion, pomFile);
        String runtimeVersion = System.getProperty("runtimeVersion");
        replaceString("RUNTIME_VERSION", runtimeVersion, pomFile);
    }

    protected static void replaceString(String str, String replacement, File file) throws IOException {
        Path path = file.toPath();
        Charset charset = StandardCharsets.UTF_8;
        String content = new String(Files.readAllBytes(path), charset);

        if (!content.contains(str)) throw new IOException("can't find:"+str);
        content = content.replaceAll(str, replacement);
        Files.write(path, content.getBytes(charset));
    }

    protected static boolean verifyLogMessageExists(String message, int timeout, File log)
        throws InterruptedException, FileNotFoundException, IOException {
        int waited = 0;
        int sleep = 10;
        while (waited <= timeout) {
            if (readFile(message, log) != null) {
                return true;
            }
            Thread.sleep(sleep);
            waited += sleep;
        }
        return false;
    }

    protected static String findLogMessage(String message, int timeout, File log)
        throws InterruptedException, FileNotFoundException, IOException {
        int waited = 0;
        int sleep = 10;
        while (waited <= timeout) {
            String line = readFile(message, log);
            if (line != null) {
                return line;
            }
            Thread.sleep(sleep);
            waited += sleep;
        }
        return null;
    }

    protected static String readFile(String str, File file) throws FileNotFoundException, IOException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line = br.readLine();
        try {
            while (line != null) {
                if (line.contains(str)) {
                    return line;
                }
                line = br.readLine();
            }
        } finally {
            br.close();
        }
        return null;
    }
  
    /**
     * Given an configuration XML file return the features in the featureManager
     * element if any
     * 
     * @param file configuration XML file
     * @return set of features, empty list if no features are found
     */
    protected static Set<String> readFeatures(File configurationFile) throws Exception {
        Set<String> features = new HashSet<String>();

        // return empty list if file does not exist or is not an XML file
        if (!configurationFile.exists() || !configurationFile.getName().endsWith(".xml")) {
            return features;
        }

        // read configuration xml file
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setIgnoringComments(true);
        docBuilderFactory.setCoalescing(true);
        docBuilderFactory.setIgnoringElementContentWhitespace(true);
        docBuilderFactory.setValidating(false);
        DocumentBuilder documentBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = documentBuilder.parse(configurationFile);

        XPath xPath = XPathFactory.newInstance().newXPath();
        String expression = "/server/featureManager/feature";
        NodeList nodes = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
        for (int i = 0; i < nodes.getLength(); i++) {
            features.add(nodes.item(i).getTextContent());
        }
        return features;
    }

    protected void runCompileAndGenerateFeatures() throws IOException, InterruptedException {
        runCompileAndGenerateFeatures("");
    }
    protected void runCompileAndGenerateFeatures(String args) throws IOException, InterruptedException {
        runProcess("compile liberty:generate-features " + args);
    }

    protected void runGenerateFeaturesGoal() throws IOException, InterruptedException {
        runProcess("liberty:generate-features");
    }

    // Format the output to help debug test failures.
    // The problem is that the test case log looks just like the JUnit log of
    // the calling process.
    // For a short stream just print it. Add a start and end string to separate
    // it from the rest of the log.
    // For long output streams, add a header which indicates how many lines to skip
    // if you want to read the end. Also add a trailer to similarly show how many
    // lines to scroll up to find the beginning. Number each line to help parse
    // the output.
    protected String formatOutput(String output) {
        if (output == null || output.length() < 101) {
            return "\n==Process Output==\n" + output + "\n==End==";
        }
        String[] lines = output.split("\r\n|\r|\n");
        StringBuffer result = new StringBuffer(String.format("==Process Output %d lines==\n", lines.length));
        int count = 1;
        for (String line : lines) {
            result.append(String.format("%5d>%s\n", count++, line));
        }
        result.append(String.format("==Process Output End %d lines==\n", lines.length));
        return result.toString();
    }
}
