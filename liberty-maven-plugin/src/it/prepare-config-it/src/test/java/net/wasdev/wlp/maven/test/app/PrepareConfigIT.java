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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Integration test for prepare-config goal
 */
public class PrepareConfigIT {

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
    public void testPluginConfigXmlContainsConfigFile() throws Exception {
        String configFile = getXPathValue("/liberty-plugin-config/configFile");
        Assert.assertNotNull("Config file should be present", configFile);
        Assert.assertTrue("Config file should reference server.xml", configFile.contains("server.xml"));
    }

    @Test
    public void testPluginConfigXmlContainsBootstrapPropertiesFile() throws Exception {
        String bootstrapFile = getXPathValue("/liberty-plugin-config/bootstrapPropertiesFile");
        Assert.assertNotNull("Bootstrap properties file should be present", bootstrapFile);
        Assert.assertTrue("Bootstrap file should reference bootstrap.properties", 
            bootstrapFile.contains("bootstrap.properties"));
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
        // Verify that tmp directory structure was created
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
    public void testConfigFilesCopiedToMockServer() throws Exception {
        // Verify that config files were copied to mock server structure
        File mockServerDir = new File("target/tmp/wlp/usr/servers/testServer");
        
        File serverXml = new File(mockServerDir, "server.xml");
        Assert.assertTrue("server.xml should be copied to mock server", serverXml.exists());
        
        File bootstrapProps = new File(mockServerDir, "bootstrap.properties");
        Assert.assertTrue("bootstrap.properties should be copied to mock server", bootstrapProps.exists());
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
        
        // configFile should point to mock server
        String configFile = getXPathValue("/liberty-plugin-config/configFile");
        Assert.assertNotNull("Config file should be present", configFile);
        Assert.assertTrue("Config file should point to mock server in tmp",
            configFile.contains("tmp") && configFile.contains("testServer"));
    }

    /**
     * Helper method to get XPath value from the plugin config XML
     */
    private String getXPathValue(String expression) throws Exception {
        File configFile = new File(PLUGIN_CONFIG_XML);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new FileInputStream(configFile));
        
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        
        return xpath.evaluate(expression, doc);
    }

    /**
     * Helper method to get XPath NodeList from the plugin config XML
     */
    private NodeList getXPathNodeList(String expression) throws Exception {
        File configFile = new File(PLUGIN_CONFIG_XML);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new FileInputStream(configFile));
        
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        
        return (NodeList) xpath.evaluate(expression, doc, XPathConstants.NODESET);
    }
}