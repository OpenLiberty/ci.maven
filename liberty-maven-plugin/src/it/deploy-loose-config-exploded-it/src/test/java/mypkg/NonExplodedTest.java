/*******************************************************************************
 * (c) Copyright IBM Corporation 2023.
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
package mypkg;

import static mypkg.utils.LooseConfigUtils.validateSrcMainWebAppRoot;
import static mypkg.utils.LooseConfigUtils.validateTargetClasses;

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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Web application test case
 */
public class NonExplodedTest {
    
    @Test
    public void testNonExplodedLooseAppFormat() throws Exception {
        File in = new File("target/liberty/wlp/usr/servers/defaultServer/dropins/deploy-loose-config-exploded-it.war.xml");
        try (FileInputStream input = new FileInputStream(in)) {        
        
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

            String expression = "/archive//*";
            NodeList nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
            Assert.assertEquals("Number of archive elements ==>", 13, nodes.getLength());

            expression = "/archive/dir";
            nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
            Assert.assertEquals("Number of <dir> element ==>", 3, nodes.getLength());

            // validate:
            //    <dir sourceOnDisk="...\src\main\webapp" targetInArchive="/"/>
            validateSrcMainWebAppRoot(nodes.item(0));        
            
            // validate: 
            //    <dir sourceOnDisk="...\target\classes" targetInArchive="/WEB-INF/classes"/>
            validateTargetClasses(nodes.item(1));        

            expression = "/archive/file";
            nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
            
            // validate:
            //    <file sourceOnDisk="../.m2\repository\org\apache\commons\commons-lang3\3.18.0\commons-lang3-3.18.0.jar" 
            //       targetInArchive="/WEB-INF/lib/commons-lang3-3.18.0.jar"/>
            String commonsLangBaseName = "commons-lang3-3.18.0.jar";
            boolean foundCommonsLangJar = false;
            for (int i = 0; i < nodes.getLength() && !foundCommonsLangJar; i++) {
                Node node = nodes.item(i);
                String srcVal = node.getAttributes().getNamedItem("sourceOnDisk").getNodeValue();
                String targetVal = node.getAttributes().getNamedItem("targetInArchive").getNodeValue();
                if (srcVal.endsWith(commonsLangBaseName) && targetVal.endsWith(commonsLangBaseName)) {
                    foundCommonsLangJar = true;
                }
            }
            Assert.assertTrue("Didn't find commons lang JAR in loose app XML ending with: " + commonsLangBaseName, foundCommonsLangJar);
        }
    }
}