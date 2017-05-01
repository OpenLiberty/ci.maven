package net.wasdev.wlp.maven.test.app;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.Assert;
import org.junit.Test;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * 
 * Web application test case
 * 
 */

public class DefaultAppDirectoryTest {

    public final String CONFIG_XML = "liberty-plugin-config.xml";
    public final String CONFIG_DROPINS_XML="liberty/usr/servers/test/configDropins/defaults/install_apps_configuration_1491924271.xml";

    @Test
    public void testConfigPropFileExist() throws Exception {
        File f = new File(CONFIG_XML);
        Assert.assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
    }
    
    @Test
    public void testXmlElements() throws Exception {
        File in = new File(CONFIG_XML);
        FileInputStream input = new FileInputStream(in);
        
        // get input XML Document 
        DocumentBuilderFactory inputBuilderFactory = DocumentBuilderFactory.newInstance();
        inputBuilderFactory.setIgnoringComments(true);
        inputBuilderFactory.setCoalescing(true);
        inputBuilderFactory.setIgnoringElementContentWhitespace(true);
        inputBuilderFactory.setValidating(false);
        DocumentBuilder inputBuilder = inputBuilderFactory.newDocumentBuilder();
        Document inputDoc=inputBuilder.parse(input);
        
        // parse input XML Document
        XPath xPath = XPathFactory.newInstance().newXPath();
        String expression = "/liberty-plugin-config/serverDirectory";
        NodeList nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
        Assert.assertEquals("Number of <serverDirectory/> element ==>", 1, nodes.getLength());
        
        xPath = XPathFactory.newInstance().newXPath();
        expression = "/liberty-plugin-config/configFile";
        nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
        Assert.assertEquals("Number of <configFile/> element ==>", 1, nodes.getLength());

        xPath = XPathFactory.newInstance().newXPath();
        expression = "/liberty-plugin-config/serverName/text()";
        String value = (String) xPath.compile(expression).evaluate(inputDoc, XPathConstants.STRING);
        Assert.assertEquals("Value of <serverName/> ==>", "test", value);

        expression = "/liberty-plugin-config/appsDirectory/text()";
        value = (String) xPath.compile(expression).evaluate(inputDoc, XPathConstants.STRING);
        Assert.assertEquals("Value of <appsDirectory/> ==>", "apps", value);
        
        expression = "/liberty-plugin-config/installAppPackages/text()";
        value = (String) xPath.compile(expression).evaluate(inputDoc, XPathConstants.STRING);
        Assert.assertEquals("Value of <installAppPackages/> ==>", "all", value);
        
        expression = "/liberty-plugin-config/applicationFilename/text()";
        value = (String) xPath.compile(expression).evaluate(inputDoc, XPathConstants.STRING);
        Assert.assertEquals("Value of <applicationFilename/> ==>", "appsdirectory-apps-notconfigured-dependency-configured-it.war", value);
    }
    
    @Test
    public void testApplicationFileExist() throws Exception {
        File f = new File("liberty/usr/servers/test/apps/appsdirectory-apps-notconfigured-dependency-configured-it.war");
        Assert.assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
        
        File f1 = new File("liberty/usr/servers/test/apps/test-war.war");
        Assert.assertTrue(f1.getCanonicalFile() + " doesn't exist", f1.exists());
    }
    
    @Test
    public void testConfigDropinsFileExist() throws Exception {
        File f = new File(CONFIG_DROPINS_XML);
        Assert.assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
    }
    
    @Test
    public void testApplicationConfiguredInConfigDropins() throws Exception {
        File in = new File(CONFIG_DROPINS_XML);
        FileInputStream input = new FileInputStream(in);
        
        // get configDropins XML Document 
        DocumentBuilderFactory inputBuilderFactory = DocumentBuilderFactory.newInstance();
        inputBuilderFactory.setIgnoringComments(true);
        inputBuilderFactory.setCoalescing(true);
        inputBuilderFactory.setIgnoringElementContentWhitespace(true);
        inputBuilderFactory.setValidating(false);
        DocumentBuilder inputBuilder = inputBuilderFactory.newDocumentBuilder();
        Document inputDoc=inputBuilder.parse(input);
        
        // parse configDropins XML Document
        XPath xPath = XPathFactory.newInstance().newXPath();
        String expression = "/server/webApplication";
        NodeList nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
        Assert.assertEquals("Number of <webApplication/> element ==>", 1, nodes.getLength());

        Node node = nodes.item(0);
        Element element = (Element)node;      
        Assert.assertEquals("Value of the 1st <webApplication/> ==>", 
                "appsdirectory-apps-notconfigured-dependency-configured-it.war", element.getAttribute("location"));
     }
}
