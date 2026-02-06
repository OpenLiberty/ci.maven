package net.wasdev.wlp.maven.test.app;


import java.awt.List;
import java.util.ArrayList;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import java.util.Scanner;
import org.junit.Test;
import org.junit.Assert;
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
    public final String LOG_LOCATION = "liberty/wlp/usr/servers/test/logs/messages.log";
    static final String TOOLCHAIN_CONFIGURED_FOR_GOAL = "CWWKM4101I: The %s goal is using the configured toolchain JDK located at";

    @Test
    public void testServerXMLPropFileExist() throws Exception {
        File f = new File(COMPILE_JSP_SEVER_XML);
        Assert.assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
    }
    
    @Test
    public void testXmlElements() throws Exception {
        File in = new File(COMPILE_JSP_SEVER_XML);
        try (FileInputStream input = new FileInputStream(in);) {
        
            // get input XML Document 
            DocumentBuilderFactory inputBuilderFactory = DocumentBuilderFactory.newInstance();
            inputBuilderFactory.setIgnoringComments(true);
            inputBuilderFactory.setCoalescing(true);
            inputBuilderFactory.setIgnoringElementContentWhitespace(true);
            inputBuilderFactory.setValidating(false);
            inputBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false); 
            inputBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);    
            DocumentBuilder inputBuilder = inputBuilderFactory.newDocumentBuilder();
            Document inputDoc=inputBuilder.parse(input);
            
            // parse input XML Document
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = "/server/featureManager/feature[text()]";        
            NodeList nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
            Assert.assertEquals("Number of <feature/> elements ==>", 3, nodes.getLength());
            
            ArrayList<String> features = new ArrayList<String>();

            for(int i = 0; i < nodes.getLength(); i++) {
                features.add(nodes.item(i).getTextContent().trim());
            }

            Assert.assertTrue("ejbLite-3.2 <feature/> found ==>", features.contains("ejbLite-3.2")); 
            Assert.assertTrue("mongodb-2.0 <feature/> found ==>", features.contains("mongodb-2.0"));
            Assert.assertTrue("jsp-2.3 <feature/> found ==>", features.contains("jsp-2.3"));

            // parse input XML Document
            String expression2 = "/server/jspEngine";        
            nodes = (NodeList) xPath.compile(expression2).evaluate(inputDoc, XPathConstants.NODESET);
            Assert.assertEquals("Number of <jspEngine/> elements ==>", 1, nodes.getLength());

            if (nodes.item(0) instanceof Element) {
                Element child = (Element) nodes.item(0);
                String nodeValue = child.getAttribute("javaSourceLevel");
                Assert.assertTrue("Unexpected javaSourceLevel ==>"+nodeValue, nodeValue.equals("17"));
            }
        }
    }

    @Test
    public void testToolchainLogs() throws Exception {
        File buildLog = new File("../build.log");
        Assert.assertTrue(buildLog.exists());

        Assert.assertTrue("Did not find toolchain honored message for create goal in build.log", logContainsMessage(buildLog, String.format(TOOLCHAIN_CONFIGURED_FOR_GOAL, "create")));
        Assert.assertTrue("Did not find toolchain honored message for start goal in build.log", logContainsMessage(buildLog, String.format(TOOLCHAIN_CONFIGURED_FOR_GOAL, "start")));
        Assert.assertTrue("Did not find toolchain honored message for compile-jsp goal in build.log", logContainsMessage(buildLog, String.format(TOOLCHAIN_CONFIGURED_FOR_GOAL, "compile-jsp")));

        File f = new File(LOG_LOCATION);
        Assert.assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
        // should contain java.version = 11 since <jdkToolChain> is defined as Java 11
        Assert.assertTrue("Did not find toolchain version in messages.log", logContainsMessage(f, "java.version = 11"));

    }


    private boolean logContainsMessage( File logFile, String message) throws FileNotFoundException {
        Assert.assertTrue("Log file not found at location: "+ LOG_LOCATION, logFile.exists());
        boolean found = false;

        try (Scanner scanner = new Scanner(logFile);) {
            while (scanner.hasNextLine()) {
                if(scanner.nextLine().contains(message)) {
                    found = true;
                }
            }
        }

        return found;
    }
}
