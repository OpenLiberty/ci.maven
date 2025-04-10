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
import java.util.HashSet;
import java.util.List;
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
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class BaseGenerateFeaturesTest {

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
    static final String GENERATED_FEATURES_FILE_PATH = "/target/liberty/wlp/usr/servers/defaultServer/configDropins/overrides/" + GENERATED_FEATURES_FILE_NAME;

    protected static void setUpBeforeTest(String projectRoot) throws IOException, InterruptedException {
        basicProj = new File(projectRoot);
        tempProj = Files.createTempDirectory("temp").toFile();
        assertTrue(tempProj.exists());
        assertTrue(basicProj.exists());

        FileUtils.copyDirectory(basicProj, tempProj);
        assertTrue(tempProj.listFiles().length > 0);
        logFile = new File(basicProj, "logFile.txt");
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
        if (process.isAlive()) {
            process.waitFor(20, TimeUnit.SECONDS);
        }
        assertFalse("The process for command "+command+" did not finish in 40 seconds.", process.isAlive());

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

        content = content.replaceAll(str, replacement);
        Files.write(path, content.getBytes(charset));
    }

    protected static boolean verifyLogMessageExists(String message, int timeout, File log)
        throws InterruptedException, FileNotFoundException, IOException {
        int waited = 0;
        int sleep = 10;
        while (waited <= timeout) {
            if (readFile(message, log)) {
                return true;
            }
            Thread.sleep(sleep);
            waited += sleep;
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
        DocumentBuilderFactory inputBuilderFactory = DocumentBuilderFactory.newInstance();
        inputBuilderFactory.setIgnoringComments(true);
        inputBuilderFactory.setCoalescing(true);
        inputBuilderFactory.setIgnoringElementContentWhitespace(true);
        inputBuilderFactory.setValidating(false);
        inputBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false); 
        inputBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);    
        DocumentBuilder documentBuilder = inputBuilderFactory.newDocumentBuilder();
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
        runProcess("compile liberty:generate-features");
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
