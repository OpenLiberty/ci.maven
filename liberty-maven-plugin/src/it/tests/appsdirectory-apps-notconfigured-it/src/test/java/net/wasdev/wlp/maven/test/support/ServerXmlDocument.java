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
package net.wasdev.wlp.maven.test.support;

import java.io.File;
import java.io.FileWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ServerXmlDocument {
    
    private ServerXmlDocument() {    
    }
    
    public static boolean isFoundTagNames(String fileName, String[] tagNames) throws Exception {

        boolean bFoundTag = false; 
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(fileName);
        
        for (int i = 0; i < tagNames.length; ++i) {
            String tag = tagNames[i];
            NodeList appList = doc.getElementsByTagName(tag);
            if (appList.getLength() > 0) {
                bFoundTag = true;
                break;
            }
        }
        return bFoundTag;
    }
    
    public static boolean isFoundTagName(String fileName, String tag) throws Exception {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(fileName);
        
        NodeList appList = doc.getElementsByTagName(tag);
        
        return appList.getLength() > 0;
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
    
    public static boolean isFoundWebApplication(String filePath) throws Exception {
        
        if (ServerXmlDocument.isFoundTagName(filePath, "webApplication"))
            return true;
        
        boolean bFoundTag = false; 
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(filePath);
        
        NodeList appList = doc.getElementsByTagName("application");

        for(int i = 0; i < appList.getLength(); i++) {
            // e.g. <application id="blog" location="blog.war" name="blog" type="war"/>
            String typeValue = appList.item(i).getAttributes().getNamedItem("type").getNodeValue();
        
            if (typeValue != null && typeValue.equals("war")) {
                bFoundTag = true;
                break;
            }
            else {
                String locationValue = appList.item(i).getAttributes().getNamedItem("location").getNodeValue();
                if (locationValue != null && locationValue.endsWith(".war")) {
                    bFoundTag = true;
                    break;
                }
            }
        }
        return bFoundTag;
    }
}
