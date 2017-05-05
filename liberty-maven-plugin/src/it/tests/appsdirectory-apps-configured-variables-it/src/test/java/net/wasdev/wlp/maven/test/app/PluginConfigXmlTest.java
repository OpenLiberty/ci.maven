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
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 * Web application test case
 * 
 */

public class PluginConfigXmlTest {

    public final String CONFIG_DROPINS_XML="liberty/usr/servers/test/configDropins/defaults/install_apps_configuration_1491924271.xml";

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
                "test-war.war", element.getAttribute("location"));
     }
}
