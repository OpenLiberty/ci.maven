package net.wasdev.wlp.maven.test.app;

import java.io.File;
import java.io.FileInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
// Test that the location variable in server.xml with a default value is not used for the 
// location of the application when overridden by a bootstrapProperties property in the pom.xml.
public class PluginConfigXmlTest {

    public final String CONFIG_DROPINS_XML="liberty/usr/servers/test/configDropins/defaults/install_apps_configuration_1491924271.xml";

    public final String APP_IN_APPS_FOLDER="liberty/usr/servers/test/apps/appsdirectory-apps-configured-bootstraps-it.war";

    @Test
    public void testApplicationConfiguredInConfigDropins() throws Exception {
        File in = new File(CONFIG_DROPINS_XML);
        try (FileInputStream input = new FileInputStream(in)) {        
        
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
            assertEquals("Number of <webApplication/> element ==>", 1, nodes.getLength());

            Node node = nodes.item(0);
            Element element = (Element)node;      
            assertEquals("Value of the 1st <webApplication/> ==>", 
                    "test-war.war", element.getAttribute("location"));
        }
    }

    @Test
    public void testApplicationLocatedInAppsFolder() throws Exception {
        File app = new File(APP_IN_APPS_FOLDER);
        assertTrue("Application not found in apps folder at " + APP_IN_APPS_FOLDER,app.exists());   
    }
}
