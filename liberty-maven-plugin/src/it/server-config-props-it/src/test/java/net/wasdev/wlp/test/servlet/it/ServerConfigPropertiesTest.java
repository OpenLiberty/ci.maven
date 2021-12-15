package net.wasdev.wlp.maven.test.servlet.it;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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
        InputStream serverOutput = null;
        InputStreamReader in = null;
        Scanner s = null;
        File logFile = new File("logFile.txt");
        List<String> duplicateMatches = new ArrayList<String>();
        List<String> appDirMatches = new ArrayList<String>();
        List<String> appInstalledMatches = new ArrayList<String>();

        try {
            //Create server and copy config
            InvocationRequest createRequest = new DefaultInvocationRequest()
            .setPomFile(pomFilePath)
            .setGoals(Collections.singletonList("liberty:create"))
            .setProperties(props);

            Invoker invoker = new DefaultInvoker();

            InvocationResult createResult = invoker.execute( createRequest );

            Assert.assertTrue("Server not created successfully.", createResult.getExitCode() == 0);

            //Start server in dev mode with generate-features disabled
            ProcessBuilder builder = buildProcess(logFile, "mvn liberty:dev -DgenerateFeatures=false");
            Process process = builder.start();
            OutputStream stdin = process.getOutputStream();
      
            //Wait for dev mode to run
            Thread.sleep(10000);

            //Stop dev mode
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
            writer.write("exit\n");
            writer.flush();
            writer.close();

            serverOutput = new FileInputStream(logFile);
            in = new InputStreamReader(serverOutput);
            s = new Scanner(in);

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
        } catch (Exception e) {}

        s.close();
        in.close();
        serverOutput.close();

        //Check app name/appsDir resolved correctly during create, deploy, and start
        Assert.assertTrue("Found duplicate application message in console output", duplicateMatches.size() == 0);
        Assert.assertTrue("Did not find appsDirectory message in console output", appDirMatches.size() == 1);
        Assert.assertTrue("Did not find app install message in console output", appInstalledMatches.size() == 1);

        String appMessage = appInstalledMatches.get(0);

        Assert.assertTrue("server-config-props-it.war.xml was not installed correctly", appMessage.endsWith("server-config-props-it.war.xml."));
    }

    private ProcessBuilder buildProcess(File logFile, String processCommand) throws Exception {
        ProcessBuilder builder = new ProcessBuilder();
        builder.redirectOutput(logFile);
        builder.redirectError(logFile);
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