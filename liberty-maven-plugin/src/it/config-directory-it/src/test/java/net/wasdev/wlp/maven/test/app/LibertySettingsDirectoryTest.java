package net.wasdev.wlp.maven.test.app;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * 
 * libertySettingsFolder test case
 * 
 */

public class LibertySettingsDirectoryTest {

    @Test
    public void testLibertyConfigDirValidDir() throws Exception {
        File f1 = new File("liberty/etc", "repository.properties");
        assertTrue(f1.getCanonicalFile() + " doesn't exist", f1.exists());
    }

    
    @Test
    public void testLibertyConfigDirInvalidDir() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false); 
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);    
        DocumentBuilder builder = factory.newDocumentBuilder();

        File pomFilePath = new File("../pom.xml");
        Document pomFile = builder.parse(pomFilePath);
        pomFile.getDocumentElement().normalize();

        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
        String pomVersion = xpath.evaluate("/project/build/plugins/plugin[artifactId='liberty-maven-plugin']/version", pomFile);

        Properties props = new Properties();
        props.put("pluginVersion", pomVersion);

        InvocationRequest request = new DefaultInvocationRequest()
        .setPomFile( new File("../src/test/resources/invalidDirPom.xml"))
        .setGoals( Collections.singletonList("package"))
        .setProperties(props);

        InvocationOutputHandler outputHandler = new InvocationOutputHandler(){
            @Override
            public void consumeLine(String line) throws IOException {
                if (line.contains("<libertySettingsFolder> must be a directory")) {
                    throw new IOException("Caught expected MojoExecutionException - " + line);
                }
            }
        };

        Invoker invoker = new DefaultInvoker();
        invoker.setOutputHandler(outputHandler);

        InvocationResult result = invoker.execute( request );

        assertTrue("Exited successfully, expected non-zero exit code.", result.getExitCode() != 0);
        assertNotNull("Expected MojoExecutionException to be thrown.", result.getExecutionException());
    }
}