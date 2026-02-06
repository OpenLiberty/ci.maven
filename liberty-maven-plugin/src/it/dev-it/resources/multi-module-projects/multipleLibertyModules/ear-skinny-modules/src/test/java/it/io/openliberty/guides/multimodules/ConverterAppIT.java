/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/
package it.io.openliberty.guides.multimodules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.jupiter.api.Test;

public class ConverterAppIT {
    String port = System.getProperty("default.http.port");
    String war = "converter";
    String urlBase = "http://localhost:" + port + "/" + war + "/";

    public final String LOOSE_APP = "target/liberty/wlp/usr/servers/defaultServer/apps/guide-maven-multimodules-ear-skinny-modules.ear.xml";

    @Test
    public void testLooseApplicationFileExist() throws Exception {
        File f = new File(LOOSE_APP);
        assertTrue(f.exists(),f.getCanonicalFile() + " doesn't exist");
    }

    @Test
    public void testLooseApplicationFileContent() throws Exception {
        File f = new File(LOOSE_APP);
        try (FileInputStream input = new FileInputStream(f);) {

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
            String expression = "/archive/file";
            NodeList nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
            assertEquals(2, nodes.getLength(),"Number of <file/> element expected 2 including ear application.xml and manifest.mf");
            assertEquals( "/META-INF/application.xml",
                    nodes.item(0).getAttributes().getNamedItem("targetInArchive").getNodeValue(),"file targetInArchive attribute value");
            assertEquals( "/META-INF/MANIFEST.MF",
                    nodes.item(1).getAttributes().getNamedItem("targetInArchive").getNodeValue(),"file targetInArchive attribute value");

            expression = "/archive/archive";
            nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
            assertEquals(5, nodes.getLength(),"Number of <archive/> elements expected 5 including jar, ejb, war, war2, rar");
            assertEquals( "/lib/io.openliberty.guides-guide-maven-multimodules-jar-1.0-SNAPSHOT.jar",
                    nodes.item(0).getAttributes().getNamedItem("targetInArchive").getNodeValue(),"archive targetInArchive attribute value expected with jar path");
            assertEquals( "/io.openliberty.guides-guide-maven-multimodules-ejb-1.0-SNAPSHOT.jar",
                nodes.item(1).getAttributes().getNamedItem("targetInArchive").getNodeValue(),"archive targetInArchive attribute value expected with ejb path");
            assertEquals("/guide-maven-multimodules-war-1.0-SNAPSHOT.war",
                    nodes.item(2).getAttributes().getNamedItem("targetInArchive").getNodeValue(),"archive targetInArchive attribute value expected with war path");

            assertEquals("/guide-maven-multimodules-war2-1.0-SNAPSHOT.war",
                    nodes.item(3).getAttributes().getNamedItem("targetInArchive").getNodeValue(),"archive targetInArchive attribute value expected with war2 path");

            assertEquals("/io.openliberty.guides-guide-maven-multimodules-rar-1.0-SNAPSHOT.rar",
                    nodes.item(4).getAttributes().getNamedItem("targetInArchive").getNodeValue(),"archive targetInArchive attribute value expected with rar path");

            expression = "/archive/archive/dir";
            nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
            assertEquals(5, nodes.getLength(),"Number of <dir/> element expected 5");

            expression = "/archive/archive/file";
            nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
            assertEquals(6, nodes.getLength(),"Number of <file/> element expected 6 including all manifest.mf and library jars");
            expression = "/archive/archive/archive";
            nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
            assertEquals(0, nodes.getLength(),"Number of <archive/> element inside any other <archive/> is expected to be zero, since we are using skinnymodules");

        }
    }

    @Test
    public void testIndexPage() throws Exception {
        String url = this.urlBase;
        HttpURLConnection con = testRequestHelper(url, "GET");
        assertEquals(200, con.getResponseCode(), "Incorrect response code from " + url);
        assertTrue(testBufferHelper(con).contains("Enter the height in centimeters"),
                        "Incorrect response from " + url);
    }

    @Test
    public void testHeightsPage() throws Exception {
        String url = this.urlBase + "heights.jsp?heightCm=10";
        HttpURLConnection con = testRequestHelper(url, "POST");
        assertTrue(testBufferHelper(con).contains("3        inches"),
                        "Incorrect response from " + url);
    }

    private HttpURLConnection testRequestHelper(String url, String method)
                    throws Exception {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        // optional default is GET
        con.setRequestMethod(method);
        return con;
    }

    private String testBufferHelper(HttpURLConnection con) throws Exception {
        BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        return response.toString();
    }

}
