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
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ServerXmlDocument {
    
    private ServerXmlDocument() {    
    }

    /**
     * @param filePath
     * @param fileName application file name
     * @return true if the application with the file name is configured.
     * @throws Exception
     */
    private static boolean isFoundWebAppWithFileName(String filePath, String fileName) throws Exception {
        return isFoundAppConfigByTagAndFileName(filePath, "webApplication", fileName);
    }
    
    /**
     * @param filePath 
     * @param tag specify element tag to parse
     * @param fileName application file name
     * @return true if the application with the file name within the specified element tag is configured. 
     * @throws Exception
     */
    private static boolean isFoundAppConfigByTagAndFileName(String filePath, String tag, String fileName) throws Exception {

        NodeList appList = getNodeList(filePath, "/server/" + tag);
        
        if (appList == null || appList.getLength() == 0) {
            return false; 
        }
        else {
          if (fileName == null) {
            return true;
          }
        } 
        
        for (int i = 0; i < appList.getLength(); i++) {
            Node locNode = appList.item(i).getAttributes().getNamedItem("location");
            
            if (locNode != null) {
                String locValue = locNode.getNodeValue();
                if (locValue.equals(fileName)) {
                    return true;
                }
            }
        }
        
        return false; 
    }
    
    public static boolean isFoundAppConfig(String filePath, File configDirectory, String fileName) throws Exception {
        
        boolean bFound = false;
        
        if (isFoundWebAppWithFileName(filePath, fileName)) {
            return true;
        }
        
        File in = new File(filePath);
        FileInputStream input = new FileInputStream(in);
        
        // get input XML Document 
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setIgnoringComments(true);
        docBuilderFactory.setCoalescing(true);
        docBuilderFactory.setIgnoringElementContentWhitespace(true);
        docBuilderFactory.setValidating(false);
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(input);
        
        // parse input XML Document
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList appList = (NodeList) xPath.compile("/server/application").evaluate(doc, XPathConstants.NODESET);
        
        if (isFoundAppConfigFromAppListByFileName(appList, fileName)) {
            return true;
        }
        
        // check if fragments in 'configDropins' folder contains webApplication configuration. 
        if (isFoundAppConfigFromDropinsDir(filePath, configDirectory, fileName))
            return true;
        
        NodeList incList = (NodeList) xPath.compile("/server/include").evaluate(doc, XPathConstants.NODESET);
    
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
                            incDoc = docBuilder.parse(connection.getInputStream());
                        }
                    }
                    else if (locValue.startsWith("file:")) {
                        if (isValidURL(locValue)) {
                            File locFile = new File(locValue);
                            if (locFile.exists()) {
                                InputStream inputStream = new FileInputStream(locFile.getCanonicalPath());
                                incDoc = docBuilder.parse(inputStream);    
                            }
                        }
                    }
                    else if (locValue.startsWith("ftp:")) {
                        // TODO handle ftp protocol
                    }
                    // relative file path
                    else {
                        File serverFile = new File(filePath);   
                        File locFile = new File(serverFile.getParentFile(), locValue);
      
                        if (locFile != null && locFile.exists()) {
                            InputStream inputStream = new FileInputStream(locFile.getCanonicalPath());
                            incDoc = docBuilder.parse(inputStream);    
                        }
                        else {
                            if (configDirectory != null && configDirectory.exists()) {
                                locFile = new File(configDirectory, locValue);
                                InputStream inputStream = new FileInputStream(locFile.getCanonicalPath());
                                incDoc = docBuilder.parse(inputStream);    
                            }
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
                    if (isFoundAppConfigFromAppList(apps))
                        return true;
                }
            }
        }
        return bFound;
    }
    
    private static boolean isFoundAppConfigFromDropinsDir(String serverFilePath, File configDirectory, String fileName) {
        boolean bFound = false; 
        
        File serverFile = new File (serverFilePath);
        File configDropins = null;
        
        if (configDirectory != null && configDirectory.exists()) {
            configDropins = new File(configDirectory, "configDropins");
        }
        else {
            configDropins = new File(serverFile.getParent(), "configDropins");
        }
        
        if (configDropins.exists()) {
            File overrides = new File(configDropins, "overrides");
            // <server_name>/configDropins/overrides
            if (overrides.exists() && isFoundAppConfigFromFragmentDir(overrides, fileName)) {
                return true;
            }
            
            // <server_name>/configDropins/defaults
            File defaults = new File(configDropins, "defaults");
            if (defaults.exists() && isFoundAppConfigFromFragmentDir(defaults, fileName)) {
                return true;
            }
        }
        return bFound;
    }
    
    private static boolean isFoundAppConfigFromFragmentDir(File dir, String fileName) {
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
            if (isFoundAppConfigInFragmentFile(xmlFiles[i].getPath(), fileName)) {
               return true;
            }
        }
        return false;
    }
    
    private static boolean isFoundAppConfigInFragmentFile(String filePath, String fileName) {
        try {
            if (isFoundWebAppWithFileName(filePath, fileName)) {
                return true;
            }
        
            NodeList appList = getNodeList(filePath, "/server/application");
        
            if (isFoundAppConfigFromAppListByFileName(appList, fileName)) {
                return true;
            }

        } catch (Exception e) {
            return false;
        }   
        return false; 
    }
    
    private static NodeList getNodeList(String filePath, String expression) throws Exception {
        File in = new File(filePath);
        FileInputStream input = new FileInputStream(in);
        
        // get input XML Document 
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setIgnoringComments(true);
        docBuilderFactory.setCoalescing(true);
        docBuilderFactory.setIgnoringElementContentWhitespace(true);
        docBuilderFactory.setValidating(false);
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(input);
        
        // parse input XML Document
        XPath xPath = XPathFactory.newInstance().newXPath();
        return (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
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
   
    private static boolean isFoundAppConfigFromAppList(NodeList nodeList) {
        return isFoundAppConfigFromAppListByFileName(nodeList, null);
    }
    
    /**
     * @param nodeList application node list to parse.
     * @param fileName  application file name to search, 'null' otherwise. 
     * @return true if the application with the file name is configured.
     */
    private static boolean isFoundAppConfigFromAppListByFileName(NodeList nodeList, String fileName) {
        
        boolean bFound = false; 
         
        for(int i = 0; i < nodeList.getLength(); i++) {
            if (fileName ==  null) {
                Node typeNode = nodeList.item(i).getAttributes().getNamedItem("type");
                
                if (typeNode != null) {
                    String typeValue = typeNode.getNodeValue();
                    
                    if (typeValue != null) {
                        bFound = typeValue.equals("war");
                        break;
                    }
                }
            }
    
            Node locNode = nodeList.item(i).getAttributes().getNamedItem("location");
            if (locNode != null) {
                String locValue = locNode.getNodeValue();
                
                if (locValue != null && !locValue.isEmpty()) {
                    if (locValue.endsWith(".war")) {
                        if (fileName != null) {
                            bFound = locValue.equals(fileName);
                            if (bFound) {
                                break;
                            }
                        }
                        else {
                            bFound = true; 
                            break;
                        }
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
