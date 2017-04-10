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
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ServerXmlDocument {
    
    private ServerXmlDocument() {    
    }
    
    public static boolean isFoundTagName(String fileName, String tag) throws Exception {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(fileName);
        
        NodeList appList = doc.getElementsByTagName(tag);
        
        return appList != null && appList.getLength() > 0;
    }
    
    public static boolean isFoundWebApplication(String filePath, boolean bTarget) throws Exception {
        
        if (isFoundTagName(filePath, "webApplication"))
            return true;
         
        boolean bFound = false;
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(filePath);
        NodeList appList = doc.getElementsByTagName("application");

        if (isFoundWebAppElement(appList))
            return true;

        // check include elements against target server. 
        if (bTarget) {
            // check if fragments in 'configDropins' folders contain webApplication configuration. 
            if (isFoundInConfigDropinsDir(filePath))
                return true;
            
            NodeList incList = doc.getElementsByTagName("include");
        
            if (incList == null)
                return false;
         
            for (int i = 0; i < incList.getLength(); i++) {
                Node locNode = incList.item(i).getAttributes().getNamedItem("location");
                Document incDoc = null;
                if (locNode != null) {
                    String locValue = locNode.getNodeValue();
                    if (locValue != null && locValue.endsWith(".xml")) {
                        if (locValue.startsWith("http")) {
                            if (isValidURL(locValue)) {
                                URL url = new URL(locValue);
                                URLConnection connection = url.openConnection();
                                incDoc = builder.parse(connection.getInputStream());
                            }
                        }
                        else if (locValue.startsWith("file:")) {
                            if (isValidURL(locValue)) {
                                File locFile = new File(locValue);
                                if (locFile.exists()) {
                                    InputStream inputStream = new FileInputStream(locFile.getCanonicalPath());
                                    incDoc = builder.parse(inputStream);    
                                }
                            }
                        }
                        // relative file path
                        else if (!locValue.startsWith("ftp:")){
                            File serverFile = new File(filePath);                      
                            File locFile = new File(serverFile.getParent(), locValue);
                            if (locFile.exists()) {
                                InputStream inputStream = new FileInputStream(locFile.getCanonicalPath());
                                incDoc = builder.parse(inputStream);    
                            }
                        }
                    }
                }
                if (incDoc != null) {
                    NodeList webappNodeList = incDoc.getElementsByTagName("webApplication");
                    if (webappNodeList != null && webappNodeList.getLength() > 0) 
                        return true;
                    else {
                        NodeList apps = incDoc.getElementsByTagName("application");
                        if (isFoundWebAppElement(apps))
                            return true;
                    }
                }
            }
        }
        return bFound;
    }
    
    private static boolean isFoundInConfigDropinsDir(String serverFilePath) {
        boolean bFound = false; 
        
        File serverFile = new File (serverFilePath);
        File configDropins = new File(serverFile.getParent(), "configDropins");
        
        if (configDropins.exists()) {
            File overrides = new File(configDropins, "overrides");
            // <server_name>/configDropins/overrides
            if (overrides.exists() && isFoundConfigInFragmentDir(overrides)) {
                return true;
            }
            
            // <server_name>/configDropins/defaults
            File defaults = new File(configDropins, "defaults");
            if (defaults.exists() && isFoundConfigInFragmentDir(defaults)) {
                return true;
            }
        }
        return bFound;
    }
    
    private static boolean isFoundConfigInFragmentDir(File dir) {
        // FileFilter to get ".xml" files
        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File file) {
               return file.isFile() && file.getName().endsWith(".xml");
            }
        };
        // Get matching files.
        File[] xmlFiles = dir.listFiles(filter);
        for (int i = 0; i < xmlFiles.length; i++) {
            if (isFoundConfigInFragmentFile(xmlFiles[i].getPath())) {
               return true;
            }
        }
        return false;
    }
    
    private static boolean isFoundConfigInFragmentFile(String filePath) {
        try {
            if (isFoundTagName(filePath, "webApplication")) 
                return true;
        
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(filePath);
            NodeList appList = doc.getElementsByTagName("application");
        
            if (isFoundWebAppElement(appList))
                return true;

        } catch (Exception e) {
            return false;
        }   
        return false; 
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
   
    public static boolean isFoundWebAppElement(NodeList nodeList) {
         
        boolean bFound = false; 
         
        for(int i = 0; i < nodeList.getLength(); i++) {
            Node typeNode = nodeList.item(i).getAttributes().getNamedItem("type");
            if (typeNode != null) {
                String typeValue = typeNode.getNodeValue();
                
                if (typeValue != null) {
                    bFound = typeValue.equals("war");
                    break;
                }
            }
    
            Node locNode = nodeList.item(i).getAttributes().getNamedItem("location");
            if (locNode != null) {
                String locValue = locNode.getNodeValue();
                
                if (locValue != null && !locValue.isEmpty()) {
                    if (locValue.endsWith(".war")) {
                        bFound = true;
                        break;
                    }
                    else {
                        Pattern substitue = Pattern.compile("[${}]");
                        Matcher matcher = substitue.matcher(locValue);
                        // TODO : resolution of the variable in the 'location' element should be handled 
                        //        with a separate issue. Unable to check - skip it for now.
                        //        e.g. location="${wlp.install.dir}/../../${appLocation}"
                        if (matcher.find()) {
                            continue;
                        }
                    }
                }
            }
        }
        return bFound;
    }

    public static void addAppElment(File serverXML, String name, String extension) throws Exception {
    
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance(); 
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(serverXML); 
        
        Element root = doc.getDocumentElement(); 
        Element child = doc.createElement("webApplication"); 

        child.setAttribute("id", name);
        child.setAttribute("name", name);
        child.setAttribute("location", name + "." + extension);
 
        root.appendChild(child); 

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer(); 
        transformer.setOutputProperty(OutputKeys.INDENT, "yes"); 
        StreamResult result = new StreamResult(new FileWriter(serverXML)); 
        DOMSource source = new DOMSource(doc); 
        transformer.transform(source, result); 
    }    
}
