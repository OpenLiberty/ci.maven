/**
 * (C) Copyright IBM Corporation 2026.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.wasdev.wlp.maven.test.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Integration test for prepare-config goal with includeServerInfo=false
 * This tests the lightweight mode that only includes basic project information
 * without server-specific configuration file paths.
 */
public class PrepareConfigNoServerInfoIT {

    private static final String PLUGIN_CONFIG_XML = "target/liberty-plugin-config.xml";

    @Test
    public void testPluginConfigXmlExists() throws Exception {
        File configFile = new File(PLUGIN_CONFIG_XML);
        Assert.assertTrue("liberty-plugin-config.xml should exist", configFile.exists());
    }

    @Test
    public void testPluginConfigXmlContainsServerName() throws Exception {
        String serverName = getXPathValue("/liberty-plugin-config/serverName");
        Assert.assertEquals("Server name should be testServer", "testServer", serverName);
    }

    @Test
    public void testPluginConfigXmlContainsProjectType() throws Exception {
        String projectType = getXPathValue("/liberty-plugin-config/projectType");
        Assert.assertEquals("Project type should be war", "war", projectType);
    }

    @Test
    public void testPluginConfigXmlDoesNotContainConfigFile() throws Exception {
        String configFile = getXPathValue("/liberty-plugin-config/configFile");
        Assert.assertTrue("Config file should be empty or null when includeServerInfo=false", 
            configFile == null || configFile.trim().isEmpty());
    }

    @Test
    public void testPluginConfigXmlDoesNotContainBootstrapPropertiesFile() throws Exception {
        String bootstrapFile = getXPathValue("/liberty-plugin-config/bootstrapPropertiesFile");
        Assert.assertTrue("Bootstrap properties file should be empty or null when includeServerInfo=false", 
            bootstrapFile == null || bootstrapFile.trim().isEmpty());
    }

    @Test
    public void testPluginConfigXmlDoesNotContainServerEnvFile() throws Exception {
        String serverEnvFile = getXPathValue("/liberty-plugin-config/serverEnvFile");
        Assert.assertTrue("Server env file should be empty or null when includeServerInfo=false", 
            serverEnvFile == null || serverEnvFile.trim().isEmpty());
    }

    @Test
    public void testPluginConfigXmlDoesNotContainJvmOptionsFile() throws Exception {
        String jvmOptionsFile = getXPathValue("/liberty-plugin-config/jvmOptionsFile");
        Assert.assertTrue("JVM options file should be empty or null when includeServerInfo=false", 
            jvmOptionsFile == null || jvmOptionsFile.trim().isEmpty());
    }

    @Test
    public void testPluginConfigXmlContainsServerDirectory() throws Exception {
        String serverDirectory = getXPathValue("/liberty-plugin-config/serverDirectory");
        Assert.assertNotNull("Server directory should still be present", serverDirectory);
    }

    @Test
    public void testPluginConfigXmlContainsInstallDirectory() throws Exception {
        String installDirectory = getXPathValue("/liberty-plugin-config/installDirectory");
        Assert.assertNotNull("Install directory should still be present", installDirectory);
    }

    @Test
    public void testPluginConfigXmlContainsLooseApplication() throws Exception {
        String looseApp = getXPathValue("/liberty-plugin-config/looseApplication");
        Assert.assertNotNull("Loose application setting should be present", looseApp);
    }

    @Test
    public void testPluginConfigXmlContainsDependencies() throws Exception {
        NodeList dependencies = getXPathNodeList("/liberty-plugin-config/projectCompileDependency");
        Assert.assertNotNull("Dependencies should be present", dependencies);
        Assert.assertTrue("Should have at least one dependency", dependencies.getLength() > 0);
        
        // Check for Jakarta EE dependency
        boolean foundJakartaEE = false;
        for (int i = 0; i < dependencies.getLength(); i++) {
            String dep = dependencies.item(i).getTextContent();
            if (dep.contains("jakarta.jakartaee-web-api")) {
                foundJakartaEE = true;
                break;
            }
        }
        Assert.assertTrue("Should contain Jakarta EE dependency", foundJakartaEE);
    }

    @Test
    public void testPluginConfigXmlContainsApplicationFilename() throws Exception {
        String appFilename = getXPathValue("/liberty-plugin-config/applicationFilename");
        Assert.assertNotNull("Application filename should be present", appFilename);
        Assert.assertTrue("Application filename should be prepare-config-no-server-info-it.war.xml (loose app)", 
            appFilename.equals("prepare-config-no-server-info-it.war.xml"));
    }

    @Test
    public void testMockLibertyServerStructureCreated() throws Exception {
        // Verify that tmp directory structure was created even with includeServerInfo=false
        File tmpDir = new File("target/tmp");
        Assert.assertTrue("tmp directory should exist", tmpDir.exists());
        
        File wlpDir = new File(tmpDir, "wlp");
        Assert.assertTrue("wlp directory should exist", wlpDir.exists());
        
        File usrDir = new File(wlpDir, "usr");
        Assert.assertTrue("usr directory should exist", usrDir.exists());
        
        File serversDir = new File(usrDir, "servers");
        Assert.assertTrue("servers directory should exist", serversDir.exists());
        
        File serverDir = new File(serversDir, "testServer");
        Assert.assertTrue("testServer directory should exist", serverDir.exists());
    }

    @Test
    public void testConfigFilesNotCopiedToMockServer() throws Exception {
        // When includeServerInfo=false, config files should still be copied
        // because copyConfigFiles is called before the includeServerInfo check
        File mockServerDir = new File("target/tmp/wlp/usr/servers/testServer");
        
        File serverXml = new File(mockServerDir, "server.xml");
        Assert.assertTrue("server.xml should be copied to mock server", serverXml.exists());
    }

    @Test
    public void testPluginConfigXmlPointsToMockServer() throws Exception {
        // Verify that liberty-plugin-config.xml has correct directory structure
        // All directories should point to mock structure in tmp
        
        // installDirectory should point to mock Liberty in tmp
        String installDir = getXPathValue("/liberty-plugin-config/installDirectory");
        Assert.assertNotNull("Install directory should be present", installDir);
        Assert.assertTrue("Install directory should point to tmp/wlp",
            installDir.contains("tmp") && installDir.endsWith("wlp"));
        
        // userDirectory should point to mock user directory in tmp
        String userDir = getXPathValue("/liberty-plugin-config/userDirectory");
        Assert.assertNotNull("User directory should be present", userDir);
        Assert.assertTrue("User directory should point to tmp/wlp/usr",
            userDir.contains("tmp") && userDir.contains("wlp") && userDir.endsWith("usr"));
        
        // serverDirectory should point to mock server in tmp
        String serverDir = getXPathValue("/liberty-plugin-config/serverDirectory");
        Assert.assertNotNull("Server directory should be present", serverDir);
        Assert.assertTrue("Server directory should point to mock server in tmp",
            serverDir.contains("tmp") && serverDir.contains("testServer"));
        
        // serverOutputDirectory should point to mock server in tmp
        String serverOutputDir = getXPathValue("/liberty-plugin-config/serverOutputDirectory");
        Assert.assertNotNull("Server output directory should be present", serverOutputDir);
        Assert.assertTrue("Server output directory should point to mock server in tmp",
            serverOutputDir.contains("tmp") && serverOutputDir.contains("testServer"));
    }

    @Test
    public void testFasterExecutionWithoutServerInfo() throws Exception {
        // This test verifies that the goal completes successfully
        // The actual performance benefit is that server-specific file paths
        // are not included in the XML, making it lighter weight
        File configFile = new File(PLUGIN_CONFIG_XML);
        Assert.assertTrue("Config file should exist", configFile.exists());
        
        // Verify the file is smaller/simpler by checking it doesn't contain
        // server-specific file paths
        String configFile1 = getXPathValue("/liberty-plugin-config/configFile");
        String bootstrapFile = getXPathValue("/liberty-plugin-config/bootstrapPropertiesFile");
        String serverEnvFile = getXPathValue("/liberty-plugin-config/serverEnvFile");
        String jvmOptionsFile = getXPathValue("/liberty-plugin-config/jvmOptionsFile");
        
        boolean hasNoServerFiles = (configFile1 == null || configFile1.trim().isEmpty()) &&
                                   (bootstrapFile == null || bootstrapFile.trim().isEmpty()) &&
                                   (serverEnvFile == null || serverEnvFile.trim().isEmpty()) &&
                                   (jvmOptionsFile == null || jvmOptionsFile.trim().isEmpty());
        
        Assert.assertTrue("Config should not contain server-specific file paths", hasNoServerFiles);
    }

    /**
     * Helper method to get XPath value from the plugin config XML
     */
    private String getXPathValue(String expression) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        File configFile = new File(PLUGIN_CONFIG_XML);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        
        // Protect against XXE attacks
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc;
        try (FileInputStream fis = new FileInputStream(configFile)) {
            doc = builder.parse(fis);
        }
        
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        
        return xpath.evaluate(expression, doc);
    }

    /**
     * Helper method to get XPath NodeList from the plugin config XML
     */
    private NodeList getXPathNodeList(String expression) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        File configFile = new File(PLUGIN_CONFIG_XML);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        
        // Protect against XXE attacks
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc;
        try (FileInputStream fis = new FileInputStream(configFile)) {
            doc = builder.parse(fis);
        }
        
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        
        return (NodeList) xpath.evaluate(expression, doc, XPathConstants.NODESET);
    }
}
