/**
 * (C) Copyright IBM Corporation 2017.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.wasdev.wlp.maven.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class ServerConfigDocument {
    
    private static ServerConfigDocument instance;
    
    private static DocumentBuilder docBuilder;
    
    private static File configDirectory;
    private static File serverFile;
    
    private static Set<String> locations;
    
    public Set<String> getLocations() {
        return locations;
    }
    
    private static File getServerFile() {
        return serverFile;
    }
    
    public ServerConfigDocument(File serverXML, File configDir) {
        initializeAppsLocation(serverXML, configDir);
    }
    
    private static DocumentBuilder getDocumentBuilder() throws Exception {
        if (docBuilder == null) {
            // get input XML Document 
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilderFactory.setIgnoringComments(true);
            docBuilderFactory.setCoalescing(true);
            docBuilderFactory.setIgnoringElementContentWhitespace(true);
            docBuilderFactory.setValidating(false);
            docBuilder = docBuilderFactory.newDocumentBuilder();
        }
        return docBuilder;
    }
    
    public static ServerConfigDocument getInstance(File serverXML, File configDir) throws IOException {
        // Initialize if instance is not created yet, or source server xml file location has been changed.
        if (instance == null || !serverXML.getCanonicalPath().equals(getServerFile().getCanonicalPath())) {
           instance = new ServerConfigDocument(serverXML, configDir);
        }
        return instance;
     }
     
    private static void initializeAppsLocation(File serverXML, File configDir) {
        try {
            serverFile = serverXML;
            configDirectory = configDir;
            
            locations = new HashSet<String>();
            
            Document doc = parseDocument(new FileInputStream(serverFile));
            
            parseApplication(doc, "/server/application");
            parseApplication(doc, "/server/webApplication");
            parseApplication(doc, "/server/enterpriseApplication");
            parseInclude(doc, "/server/include");
            parseConfigDropinsDir();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void parseApplication(Document doc, String expression) throws Exception {
        // parse input document
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
        
        for (int i = 0; i < nodeList.getLength(); i++) {
            String nodeValue = nodeList.item(i).getAttributes().getNamedItem("location").getNodeValue();
            
            // add unique values only
            if (!nodeValue.isEmpty() && !locations.contains(nodeValue)) {
                locations.add(nodeValue);
            }
        }
    }
   
    private static void parseInclude(Document doc, String expression) throws Exception {
        // parse include document in source server xml
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
       
        for (int i = 0; i < nodeList.getLength(); i++) {
            String nodeValue = nodeList.item(i).getAttributes().getNamedItem("location").getNodeValue();
            
            if (!nodeValue.isEmpty()) {
                Document docIncl = getIncludeDoc(nodeValue);
                
                if (docIncl != null) {
                    parseApplication(docIncl, "/server/application");
                    parseApplication(docIncl, "/server/webApplication");
                    parseApplication(docIncl, "/server/enterpriseApplication");
                    // handle nested include elements
                    parseInclude(docIncl, "/server/include");
                }
            }
        }
    }
    
    private static void parseConfigDropinsDir() throws Exception {
        File configDropins = null;
        
        // if configDirectory exists and contains configDropins directory, 
        // its configDropins has higher precedence.
        if (configDirectory != null && configDirectory.exists()) {
            configDropins = new File(configDirectory, "configDropins");
        }
        
        if (configDropins == null || !configDropins.exists()) {
            configDropins = new File(getServerFile().getParent(), "configDropins");
        }
        
        if (configDropins != null && configDropins.exists()) {
            File overrides = new File(configDropins, "overrides");
            
            if (overrides.exists()) {
                File[] cfgFiles = overrides.listFiles();
                
                for (int i = 0; i < cfgFiles.length; i++) {
                    if (cfgFiles[i].isFile()) {
                        parseDropinsFiles(cfgFiles[i]);
                    }
                }
            }
            
            File defaults = new File(configDropins, "defaults");
            if (defaults.exists()) {
                File[] cfgFiles = defaults.listFiles();
                
                for (int i = 0; i < cfgFiles.length; i++) {
                    if (cfgFiles[i].isFile()) {
                        parseDropinsFiles(cfgFiles[i]);
                    }
                }
            }
        }
    }
    
    private static void parseDropinsFiles(File file) throws Exception {
        // get input XML Document 
        Document doc = parseDocument(new FileInputStream(file));
        
        parseApplication(doc, "/server/application");
        parseApplication(doc, "/server/webApplication");
        parseApplication(doc, "/server/enterpriseApplication");
        parseInclude(doc, "/server/include");
    }
    
    private static Document getIncludeDoc(String loc) throws Exception {
    
        Document doc = null;
        File locFile = null;
        
        if (loc.startsWith("http:") || loc.startsWith("https:")) {
            if (isValidURL(loc)) {
                URL url = new URL(loc);
                URLConnection connection = url.openConnection();
                doc = parseDocument(connection.getInputStream());
            }
        }
        else if (loc.startsWith("file:")) {
           if (isValidURL(loc)) {
               locFile = new File(loc);
               if (locFile.exists()) {
                   InputStream inputStream = new FileInputStream(locFile.getCanonicalPath());
                   doc = parseDocument(inputStream);    
               }
           }
       }
       else if (loc.startsWith("ftp:")) {
           // TODO handle ftp protocol
       }
       else {
           locFile = new File(loc);
           
           // check if absolute file
           if (locFile.isAbsolute()) {
               InputStream inputStream = new FileInputStream(locFile.getCanonicalPath());
               doc = parseDocument(inputStream);    
           }
           else {
               // check configDirectory first if exists
               if (configDirectory != null && configDirectory.exists()) {
                   locFile = new File(configDirectory, loc);
               }               
               
               if (locFile == null || !locFile.exists()) {
                   locFile = new File(getServerFile().getParentFile(), loc);
               }
               
               if (locFile != null && locFile.exists()) {
                   InputStream inputStream = new FileInputStream(locFile.getCanonicalPath());
                   doc = parseDocument(inputStream);    
               }
           }
        }
        return doc;
    }
    
    private static Document parseDocument(InputStream ins) throws Exception {
        Document doc = null;
        try {
            doc = getDocumentBuilder().parse(ins);
        } catch (Exception e) {
            throw e;
        } finally {
            if (ins != null) {
                ins.close();
            }
        }
        return doc;
    }
    
    private static boolean isValidURL(String url) {
        try {
            URL testURL = new URL(url);
            testURL.toURI();
            return true;
        } 
        catch (Exception exception) {
            return false;
        }
    }
}
 
