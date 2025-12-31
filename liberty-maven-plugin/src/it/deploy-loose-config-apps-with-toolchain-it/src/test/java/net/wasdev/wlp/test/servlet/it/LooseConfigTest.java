/*******************************************************************************
 * (c) Copyright IBM Corporation 2019.
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
import java.io.FileNotFoundException;
import java.util.Scanner;

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

/**
 *
 * Web application test case
 *
 */
public class LooseConfigTest {

    public static final String TOOLCHAIN_CONFIGURED_FOR_GOAL = "CWWKM4101I: The %s goal is using the configured toolchain JDK located at";
    public static final String LOG_LOCATION = "liberty/usr/servers/test/logs/messages.log";

    @Test
    public void testXmlElements() throws Exception {
        File in = new File("liberty/usr/servers/test/apps/deploy-loose-config-apps-with-toolchain-it.war.xml");
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
            String expression = "/archive/dir";
            NodeList nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
            Assert.assertEquals("Number of <dir> element ==>", 4, nodes.getLength());

            expression = "/archive/file";
            nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
            Assert.assertEquals("Number of <file> element ==>", 2, nodes.getLength());

            Node looseConfigFragmentNode = nodes.item(0);
            String sourceOnDisk = looseConfigFragmentNode.getAttributes().getNamedItem("sourceOnDisk").getNodeValue();
            Assert.assertTrue(sourceOnDisk.contains("target" + File.separator + "libs"));

            Assert.assertEquals("archive targetInArchive attribute value", "/WEB-INF/lib/loose-config-fragment-it-1.0-SNAPSHOT.jar",
                    nodes.item(0).getAttributes().getNamedItem("targetInArchive").getNodeValue());
        }
    }

    @Test
    public void testApplicationFileExist() throws Exception {
        File f = new File("liberty/usr/servers/test/apps/deploy-loose-config-apps-with-toolchain-it.war.xml");
        Assert.assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
    }

    @Test
    public void testToolchainLogs() throws Exception {
        File buildLog = new File("../build.log");
        Assert.assertTrue(buildLog.exists());

        Assert.assertTrue("Did not find toolchain honored message for create goal in build.log", logContainsMessage(buildLog, String.format(TOOLCHAIN_CONFIGURED_FOR_GOAL, "create")));
        Assert.assertTrue("Did not find toolchain honored message for start goal in build.log", logContainsMessage(buildLog, String.format(TOOLCHAIN_CONFIGURED_FOR_GOAL, "start")));
        Assert.assertTrue("Did not find toolchain honored message for deploy goal in build.log", logContainsMessage(buildLog, String.format(TOOLCHAIN_CONFIGURED_FOR_GOAL, "deploy")));

        File f = new File(LOG_LOCATION);
        Assert.assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
        // should contain java.version = 11 since <jdkToolChain> is defined as Java 11
        Assert.assertTrue("Did not find toolchain version in messages.log", logContainsMessage(f, "java.version = 11"));

    }


    public static boolean logContainsMessage( File logFile, String message) throws FileNotFoundException {
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
