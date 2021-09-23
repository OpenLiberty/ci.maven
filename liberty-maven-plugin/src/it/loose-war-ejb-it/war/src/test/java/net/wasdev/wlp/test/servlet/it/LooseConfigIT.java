/*******************************************************************************
 * (c) Copyright IBM Corporation 2021.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package net.wasdev.wlp.test.servlet.it;

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
 * 
 * Web application with EJB test case
 * 
 */
public class LooseConfigIT {
    
    @Test
    public void testXmlElements() throws Exception {
        File in = new File("liberty/wlp/usr/servers/test/apps/ejb-war.war.xml");
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
        String expression = "/archive/dir";
        NodeList nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
        Assert.assertEquals("Number of <dir> element ==>", 1, nodes.getLength());
        
        expression = "/archive/file";
        nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
        Assert.assertEquals("Number of <file> element ==>", 5, nodes.getLength());
        
        //Check correct EJB jar location exists
        expression = "/archive/archive";
        nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
        Assert.assertEquals("archive targetInArchive attribute value", "/WEB-INF/lib/test-ejb.jar", 
                nodes.item(0).getAttributes().getNamedItem("targetInArchive").getNodeValue());

        expression = "/archive/archive/file";
        nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
        Assert.assertEquals("archive targetInArchive attribute value", new File("../../ejb/target/tmp/META-INF/MANIFEST.MF").getCanonicalPath(), 
                nodes.item(0).getAttributes().getNamedItem("sourceOnDisk").getNodeValue());
    }
    
    @Test
    public void testApplicationFileExist() throws Exception {
        File f = new File("liberty/wlp/usr/servers/test/apps/ejb-war.war.xml");
        Assert.assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
    }
}
