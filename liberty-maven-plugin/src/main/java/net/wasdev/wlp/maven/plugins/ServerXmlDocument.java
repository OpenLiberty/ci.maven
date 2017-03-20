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
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
    
    public static void addAppElment(File serverXML, String artifactId) throws Exception {
    	
    	DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance(); 
    	DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(serverXML); 
        
        Element root = doc.getDocumentElement(); 
        Element child = doc.createElement("webApplication"); 

        // e.g.
        // <webApplication contextRoot="helloworld" location="helloworld.war" />
        // <application context-root="helloworld" type="war" id="helloworld"
        //	    location="helloworld.war" name="helloworld"/>

        child.setAttribute("contextRoot", artifactId);
        child.setAttribute("location", artifactId + ".war");
 
        root.appendChild(child); 

        TransformerFactory tf = TransformerFactory.newInstance();
        //tf.setAttribute("indent-number", new Integer(4));
        Transformer transformer = tf.newTransformer(); 
        transformer.setOutputProperty(OutputKeys.INDENT, "yes"); 
        StreamResult result = new StreamResult(new FileWriter(serverXML)); 
        DOMSource source = new DOMSource(doc); 
        transformer.transform(source, result); 
    }
    
}
