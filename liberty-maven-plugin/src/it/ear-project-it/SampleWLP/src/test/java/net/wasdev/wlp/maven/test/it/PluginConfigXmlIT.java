package net.wasdev.wlp.maven.test.it;

import java.io.File;
import java.io.FileInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.Test;
import org.w3c.dom.Document;

import static org.junit.Assert.*;

public class PluginConfigXmlIT {
    
    public final String CONFIG_XML = "target/liberty-plugin-config.xml";
    
    @Test
    public void testConfigPropFileExist() throws Exception {
        File f = new File(CONFIG_XML);
        assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
    }
    
    @Test
    public void testXmlElements() throws Exception {
    	File in = new File(CONFIG_XML);
        try (FileInputStream input = new FileInputStream(in);) {
        
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
            String expression = "/liberty-plugin-config/installAppPackages/text()";
            String value = (String) xPath.compile(expression).evaluate(inputDoc, XPathConstants.STRING);
            assertEquals("Value of <installAppPackages/> ==>", "dependencies", value);
            
            expression = "/liberty-plugin-config/projectType/text()";
            value = (String) xPath.compile(expression).evaluate(inputDoc, XPathConstants.STRING);
            assertEquals("Value of <projectType/> ==>", "liberty-assembly", value);
            
            expression = "/liberty-plugin-config/aggregatorParentId/text()";
            value = (String) xPath.compile(expression).evaluate(inputDoc, XPathConstants.STRING);
            assertEquals("Value of <aggregatorParentId/> ==>", "ear-project-it", value);
            
            expression = "/liberty-plugin-config/aggregatorParentBasedir/text()";
            value = (String) xPath.compile(expression).evaluate(inputDoc, XPathConstants.STRING);
            File parentProj = new File("..");
            assertEquals("Value of <aggregatorParentBasedir/> ==>", parentProj.getCanonicalPath(), value);
        }
    }
}
