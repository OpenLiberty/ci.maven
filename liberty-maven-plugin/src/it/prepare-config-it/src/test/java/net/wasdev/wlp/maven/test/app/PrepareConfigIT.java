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

import io.openliberty.tools.common.plugins.util.PrepareConfigUtil;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Integration test for prepare-config goal
 */
public class PrepareConfigIT {

    private static final String PLUGIN_CONFIG_XML = "target/liberty-plugin-config.xml";
    private static final String TEMP_DIR_NAME = PrepareConfigUtil.DEFAULT_TEMP_DIR_NAME;
    public static final String TARGET_TMP_WLP_USR_SERVERS_TEST_SERVER = "target/" + TEMP_DIR_NAME + "/wlp/usr/servers/testServer";
    public static final String BOOTSTRAP_PROPERTIES = "bootstrap.properties";
    public static final String SERVER_XML = "server.xml";
    public static final String LIBERTY_PLUGIN_CONFIG_CONFIG_FILE = "/liberty-plugin-config/configFile";

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
    public void testPluginConfigXmlContainsConfigFile() throws Exception {
        String configFile = getXPathValue(LIBERTY_PLUGIN_CONFIG_CONFIG_FILE);
        Assert.assertNotNull("Config file should be present", configFile);
        Assert.assertTrue("Config file should reference server.xml", configFile.contains(SERVER_XML));
    }

    @Test
    public void testPluginConfigXmlContainsBootstrapPropertiesFile() throws Exception {
        String bootstrapFile = getXPathValue("/liberty-plugin-config/bootstrapPropertiesFile");
        Assert.assertNotNull("Bootstrap properties file should be present", bootstrapFile);
        Assert.assertTrue("Bootstrap file should reference bootstrap.properties", 
            bootstrapFile.contains(BOOTSTRAP_PROPERTIES));
    }

    @Test
    public void testPluginConfigXmlContainsServerDirectory() throws Exception {
        String serverDirectory = getXPathValue("/liberty-plugin-config/serverDirectory");
        Assert.assertNotNull("Server directory should be present", serverDirectory);
    }

    @Test
    public void testPluginConfigXmlContainsInstallDirectory() throws Exception {
        String installDirectory = getXPathValue("/liberty-plugin-config/installDirectory");
        Assert.assertNotNull("Install directory should be present", installDirectory);
    }

    @Test
    public void testPluginConfigXmlContainsLooseApplication() throws Exception {
        String looseApp = getXPathValue("/liberty-plugin-config/looseApplication");
        Assert.assertNotNull("Loose application setting should be present", looseApp);
    }

    @Test
    public void testPluginConfigXmlContainsAppsDirectory() throws Exception {
        String appsDir = getXPathValue("/liberty-plugin-config/appsDirectory");
        Assert.assertNotNull("Apps directory should be present", appsDir);
        // Should be "apps" since we have application configured in server.xml
        Assert.assertEquals("Apps directory should be 'apps'", "apps", appsDir);
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
        Assert.assertTrue("Application filename should be prepare-config-it.war.xml (loose app)", 
            appFilename.equals("prepare-config-it.war.xml"));
    }

    @Test
    public void testNoTempDirectoryLeftBehind() throws Exception {
        // Check that no temporary Liberty installation directories are left behind
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File[] tempLibertyDirs = tempDir.listFiles((dir, name) ->
            name.startsWith("liberty-temp-") && name.endsWith("-install"));
        
        if (tempLibertyDirs != null) {
            Assert.assertEquals("No temporary Liberty directories should remain",
                0, tempLibertyDirs.length);
        }
    }

    @Test
    public void testMockLibertyServerStructureCreated() throws Exception {
        // Verify that temp directory structure was created
        File tmpDir = new File("target/" + TEMP_DIR_NAME);
        Assert.assertTrue(TEMP_DIR_NAME + " directory should exist", tmpDir.exists());
        
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
    public void testConfigFilesCopiedToMockServer() throws Exception {
        // Verify that config files were copied to mock server structure
        File mockServerDir = new File(TARGET_TMP_WLP_USR_SERVERS_TEST_SERVER);
        
        File serverXml = new File(mockServerDir, SERVER_XML);
        Assert.assertTrue("server.xml should be copied to mock server", serverXml.exists());
        
        File bootstrapProps = new File(mockServerDir, BOOTSTRAP_PROPERTIES);
        Assert.assertTrue("bootstrap.properties should be copied to mock server", bootstrapProps.exists());
    }

    @Test
    public void testPluginConfigXmlPointsToMockServer() throws Exception {
        // Verify that liberty-plugin-config.xml has correct directory structure
        // All directories should point to mock structure in tmp
        
        // installDirectory should point to mock Liberty in temp dir
        String installDir = getXPathValue("/liberty-plugin-config/installDirectory");
        Assert.assertNotNull("Install directory should be present", installDir);
        Assert.assertTrue("Install directory should point to " + TEMP_DIR_NAME + "/wlp",
            installDir.contains(TEMP_DIR_NAME) && installDir.endsWith("wlp"));
        
        // userDirectory should point to mock user directory in temp dir
        String userDir = getXPathValue("/liberty-plugin-config/userDirectory");
        Assert.assertNotNull("User directory should be present", userDir);
        Assert.assertTrue("User directory should point to " + TEMP_DIR_NAME + "/wlp/usr",
            userDir.contains(TEMP_DIR_NAME) && userDir.contains("wlp") && userDir.endsWith("usr"));
        
        // serverDirectory should point to mock server in temp dir
        String serverDir = getXPathValue("/liberty-plugin-config/serverDirectory");
        Assert.assertNotNull("Server directory should be present", serverDir);
        Assert.assertTrue("Server directory should point to mock server in " + TEMP_DIR_NAME,
            serverDir.contains(TEMP_DIR_NAME) && serverDir.contains("testServer"));
        
        // serverOutputDirectory should point to mock server in temp dir
        String serverOutputDir = getXPathValue("/liberty-plugin-config/serverOutputDirectory");
        Assert.assertNotNull("Server output directory should be present", serverOutputDir);
        Assert.assertTrue("Server output directory should point to mock server in " + TEMP_DIR_NAME,
            serverOutputDir.contains(TEMP_DIR_NAME) && serverOutputDir.contains("testServer"));
        
        // configFile should point to mock server
        String configFile = getXPathValue(LIBERTY_PLUGIN_CONFIG_CONFIG_FILE);
        Assert.assertNotNull("Config file should be present", configFile);
        Assert.assertTrue("Config file should point to mock server in " + TEMP_DIR_NAME,
            configFile.contains(TEMP_DIR_NAME) && configFile.contains("testServer"));
    }

    /**
     * Helper method to get XPath value from the plugin config XML
     */
    private String getXPathValue(String expression) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        Document doc = getDocument(configFile);
        
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        
        return xpath.evaluate(expression, doc);
    }

    /**
     * Helper method to get XPath NodeList from the plugin config XML
     */
    private NodeList getXPathNodeList(String expression) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        File configFile = new File(PLUGIN_CONFIG_XML);
        Document doc = getDocument(configFile);

        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        
        return (NodeList) xpath.evaluate(expression, doc, XPathConstants.NODESET);
    }

    private static Document getDocument(File configFile) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        // Protect against XXE attacks
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        factory.setIgnoringComments(true);
        factory.setCoalescing(true);
        factory.setIgnoringElementContentWhitespace(true);
        factory.setValidating(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc;
        try (FileInputStream fis = new FileInputStream(configFile)) {
            doc = builder.parse(fis);
        }
        return doc;
    }

    @Test
    public void testPluginConfigXmlContainsUserDirectory() throws Exception {
        String userDirectory = getXPathValue("/liberty-plugin-config/userDirectory");
        Assert.assertNotNull("User directory should be present", userDirectory);
        Assert.assertTrue("User directory should point to mock user directory",
            userDirectory.contains(TEMP_DIR_NAME) && userDirectory.contains("usr"));
    }

    @Test
    public void testPluginConfigXmlContainsServerOutputDirectory() throws Exception {
        String serverOutputDirectory = getXPathValue("/liberty-plugin-config/serverOutputDirectory");
        Assert.assertNotNull("Server output directory should be present", serverOutputDirectory);
        Assert.assertTrue("Server output directory should point to mock server",
            serverOutputDirectory.contains(TEMP_DIR_NAME) && serverOutputDirectory.contains("testServer"));
    }

    @Test
    public void testBootstrapPropertiesContentCopied() throws Exception {
        // Verify that bootstrap.properties was copied with correct content
        File mockServerDir = new File(TARGET_TMP_WLP_USR_SERVERS_TEST_SERVER);
        File bootstrapProps = new File(mockServerDir, BOOTSTRAP_PROPERTIES);
        
        Assert.assertTrue("bootstrap.properties should exist", bootstrapProps.exists());
        
        // Read and verify content
        String content = new String(Files.readAllBytes(bootstrapProps.toPath()), StandardCharsets.UTF_8);
        
        Assert.assertTrue("Bootstrap properties should contain default.http.port",
            content.contains("default.http.port"));
        Assert.assertTrue("Bootstrap properties should contain default.https.port",
            content.contains("default.https.port"));
    }

    @Test
    public void testServerXmlContentCopied() throws Exception {
        // Verify that server.xml was copied with correct content
        File mockServerDir = new File(TARGET_TMP_WLP_USR_SERVERS_TEST_SERVER);
        File serverXml = new File(mockServerDir, SERVER_XML);
        
        Assert.assertTrue("server.xml should exist", serverXml.exists());
        
        // Read and verify content
        String content = new String(Files.readAllBytes(serverXml.toPath()), StandardCharsets.UTF_8);
        
        Assert.assertTrue("server.xml should contain featureManager",
            content.contains("featureManager"));
        Assert.assertTrue("server.xml should contain jakartaee-9.1 feature",
            content.contains("jakartaee-9.1"));
        Assert.assertTrue("server.xml should contain microProfile-5.0 feature",
            content.contains("microProfile-5.0"));
    }

    @Test
    public void testPluginConfigXmlContainsProjectDirectory() throws Exception {
        String projectDirectory = getXPathValue("/liberty-plugin-config/projectDirectory");
        Assert.assertNotNull("Project directory should be present", projectDirectory);
        Assert.assertFalse("Project directory should not be empty", projectDirectory.trim().isEmpty());
    }

    @Test
    public void testPluginConfigXmlContainsBuildDirectory() throws Exception {
        String buildDirectory = getXPathValue("/liberty-plugin-config/buildDirectory");
        Assert.assertNotNull("Build directory should be present", buildDirectory);
        Assert.assertTrue("Build directory should contain 'target'", buildDirectory.contains("target"));
    }

    @Test
    public void testMockServerStructureIsComplete() throws Exception {
        // Comprehensive check of the entire mock server structure
        File tmpDir = new File("target/" + TEMP_DIR_NAME);
        File wlpDir = new File(tmpDir, "wlp");
        File usrDir = new File(wlpDir, "usr");
        File serversDir = new File(usrDir, "servers");
        File serverDir = new File(serversDir, "testServer");
        
        Assert.assertTrue(TEMP_DIR_NAME + " directory should be a directory", tmpDir.isDirectory());
        Assert.assertTrue("wlp directory should be a directory", wlpDir.isDirectory());
        Assert.assertTrue("usr directory should be a directory", usrDir.isDirectory());
        Assert.assertTrue("servers directory should be a directory", serversDir.isDirectory());
        Assert.assertTrue("testServer directory should be a directory", serverDir.isDirectory());
        
        // Verify the structure matches Liberty's expected layout
        Assert.assertEquals("wlp should be child of " + TEMP_DIR_NAME, tmpDir, wlpDir.getParentFile());
        Assert.assertEquals("usr should be child of wlp", wlpDir, usrDir.getParentFile());
        Assert.assertEquals("servers should be child of usr", usrDir, serversDir.getParentFile());
        Assert.assertEquals("testServer should be child of servers", serversDir, serverDir.getParentFile());
    }

    @Test
    public void testIncludeServerInfoIsTrue() throws Exception {
        // Verify that when includeServerInfo=true (default), server files are included
        String configFile = getXPathValue(LIBERTY_PLUGIN_CONFIG_CONFIG_FILE);
        String bootstrapFile = getXPathValue("/liberty-plugin-config/bootstrapPropertiesFile");
        
        Assert.assertNotNull("Config file should be present when includeServerInfo=true", configFile);
        Assert.assertFalse("Config file should not be empty", configFile.trim().isEmpty());
        
        Assert.assertNotNull("Bootstrap file should be present when includeServerInfo=true", bootstrapFile);
        Assert.assertFalse("Bootstrap file should not be empty", bootstrapFile.trim().isEmpty());
    }
}