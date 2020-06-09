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
import org.w3c.dom.Node;
import org.w3c.dom.Element;


/**
 * 
 * Web application test case
 * 
 */

public class CompileJSPTest {

    public final String COMPILE_JSP_SEVER_XML = "compileJsp/servers/defaultServer/server.xml";

    @Test
    public void testServerXMLPropFileExist() throws Exception {
        File f = new File(COMPILE_JSP_SEVER_XML);
        Assert.assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
    }
    
    @Test
    public void testXmlElements() throws Exception {
        File in = new File(COMPILE_JSP_SEVER_XML);
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
        String expression = "/server/featureManager/feature[text()]";        
        NodeList nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
        Assert.assertEquals("Number of <feature/> elements ==>", 3, nodes.getLength());

        Assert.assertEquals("Value of first <feature/> ==>", "ejbLite-3.2", nodes.item(0).getTextContent()); 
        Assert.assertEquals("Value of second <feature/> ==>", "mongodb-2.0", nodes.item(1).getTextContent());
        Assert.assertEquals("Value of third <feature/> ==>", "jsp-2.3", nodes.item(2).getTextContent());
    
    }
    
}
