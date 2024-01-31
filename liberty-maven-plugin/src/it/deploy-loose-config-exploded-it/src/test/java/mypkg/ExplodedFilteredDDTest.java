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

import static mypkg.utils.LooseConfigUtils.*;

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
public class ExplodedFilteredDDTest {
    
    @Test
    public void testExplodedLooseAppFormat() throws Exception {
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
            Assert.assertEquals("Number of archive elements ==>", 4, nodes.getLength());

            expression = "/archive/dir";
            nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
            Assert.assertEquals("Number of <dir> element ==>", 3, nodes.getLength());

            // 1. validate:
            //    <dir sourceOnDisk="...\src\main\resource1" targetInArchive="/"/>
            validateSrcResourceRoot(nodes.item(0), "src" + File.separator + "main" + File.separator + "resource1");

            // 2. validate: 
            //    <dir sourceOnDisk="...\target\classes" targetInArchive="/WEB-INF/classes"/>
            validateTargetClasses(nodes.item(1));        

            // 3. validate:
            //     <dir sourceOnDisk="...\target\ targetInArchive="/"/>
            validateWebAppDirRoot(nodes.item(2), "deploy-loose-config-exploded-it-1.0-SNAPSHOT");
        }
    }
}
