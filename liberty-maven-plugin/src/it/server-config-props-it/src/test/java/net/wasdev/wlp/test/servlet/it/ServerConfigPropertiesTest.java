/*******************************************************************************
 * (c) Copyright IBM Corporation 2020, 2026
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
package net.wasdev.wlp.test.servlet.it;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Test that the plugin will use the correct properties in ServerConfigDocument once the server config has been copied/installed
 * The test checks for the correct app install message, and appsDirectory
 * Will fail if a duplicate application message is found
 */

public class ServerConfigPropertiesTest {

    public final String MESSAGES_LOG = "liberty/wlp/usr/servers/test/logs/messages.log";
    public final String DUPLICATE_APP_MESSAGE = ".* CWWKZ0013E: It is not possible to start two applications called .*\\.";
    public final String APPS_DIR_MESSAGE = ".* CWWKM2185I: The liberty-maven-plugin configuration parameter \"appsDirectory\" value defaults to \"apps\"\\.";
    public final String APP_INSTALLED_MESSAGE = ".* CWWKM2160I: Installing application .*\\.";

    private static DocumentBuilderFactory factory;
    private static DocumentBuilder builder;

    private static File pomFilePath;
    private static Document pomFile;

    private static XPathFactory xpathFactory;
    private static XPath xpath;
    private static String pomVersion;

    private static Properties props;

    @BeforeClass
    public static void setup() throws Exception {
        factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false); 
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);    
        builder = factory.newDocumentBuilder();

        xpathFactory = XPathFactory.newInstance();
        xpath = xpathFactory.newXPath();

        pomFilePath = new File("../pom.xml");
        pomFile = builder.parse(pomFilePath);
        pomFile.getDocumentElement().normalize();
        pomVersion = xpath.evaluate("/project/build/plugins/plugin[artifactId='liberty-maven-plugin']/version", pomFile);

        props = new Properties();
        props.put("pluginVersion", pomVersion);
    }

    @Test
    public void checkMessagesLogForIncludes() throws Exception {
        Scanner s = null;
        File logFile = new File("logFile.txt");
        List<String> duplicateMatches = new ArrayList<String>();
        List<String> appDirMatches = new ArrayList<String>();
        List<String> appInstalledMatches = new ArrayList<String>();

        try {
            //Create server and copy config
            ProcessBuilder builder = buildProcess(logFile, "mvn liberty:create");
            Process process = builder.start();
            int exitCode = process.waitFor();

            Assert.assertTrue("Server not created successfully, rc=" + exitCode, exitCode == 0);

            builder = buildProcess(logFile, "mvn liberty:dev");
            process = builder.start();
            OutputStream stdin = process.getOutputStream();

            //Wait for dev mode to run until user input is needed
            Thread.sleep(30000);

            //Stop dev mode
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
            writer.write("exit\n");
            writer.flush();
            writer.close();
            exitCode = process.waitFor();

            s = new Scanner(logFile);

            String foundString = null;
            Pattern pattern1 = Pattern.compile(DUPLICATE_APP_MESSAGE);
            Pattern pattern2 = Pattern.compile(APPS_DIR_MESSAGE);
            Pattern pattern3 = Pattern.compile(APP_INSTALLED_MESSAGE);

            while (s.hasNextLine()) {
                String line = s.nextLine();
                if (pattern1.matcher(line).find()) {
                    duplicateMatches.add(line);
                } else if (pattern2.matcher(line).find()) {
                    appDirMatches.add(line);
                } else if (pattern3.matcher(line).find()) {
                    appInstalledMatches.add(line);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        	if (s != null) s.close();
        }

        //Check app name/appsDir resolved correctly during create, deploy, and start
        String logText = new String(Files.readAllBytes(logFile.toPath()), StandardCharsets.UTF_8);
        Assert.assertTrue("Found duplicate application message in console output, log=\n" + logText, duplicateMatches.size() == 0);
        // 4: once for each goal - create, create (from dev mode), deploy and start
        Assert.assertEquals("appsDirMessage size: " + appDirMatches.size() + ", log=\n" + logText, 4, appDirMatches.size());
        Assert.assertTrue("Did not find app install message in console output, log=\n" + logText, appInstalledMatches.size() == 1);

        String appMessage = appInstalledMatches.get(0);

        Assert.assertTrue("server-config-props-it.war.xml was not installed correctly", appMessage.endsWith("server-config-props-it.war.xml."));
    }

    private ProcessBuilder buildProcess(File logFile, String processCommand) throws Exception {
        ProcessBuilder builder = new ProcessBuilder();
        builder.redirectOutput(Redirect.appendTo(logFile)); // since we are calling this method twice we need to append
        builder.redirectErrorStream(true); // redirect to the output stream
        builder.directory(new File(".."));

        String os = System.getProperty("os.name");
        if (os != null && os.toLowerCase().startsWith("windows")) {
           builder.command("CMD", "/C", processCommand);
        } else {
           builder.command("bash", "-c", processCommand);
        }
        return builder;
     }
}