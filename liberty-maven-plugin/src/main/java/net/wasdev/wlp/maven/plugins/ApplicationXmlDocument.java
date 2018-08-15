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
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Element;

import net.wasdev.wlp.common.plugins.util.XmlDocument;

public class ApplicationXmlDocument extends XmlDocument {
    
    public static final String APP_XML_FILENAME = "install_apps_configuration_1491924271.xml";
    
    public ApplicationXmlDocument() {
        try {
            createDocument("server");
        } catch (ParserConfigurationException e) {
            // it should never occur
            e.printStackTrace();
        }
    }
    
    public void createApplicationElement(String appFileName, String artifactId) {
        createApplicationElement(appFileName, artifactId, false);
    }
    
    public void createApplicationElement(String appFileName, String artifactId, boolean isSpringBootApp) {
        File app = new File(appFileName);
        
        if(isSpringBootApp) {
            createElement("springBootApplication", app, artifactId);
            return;
        }
        
        switch (appFileName.substring(appFileName.lastIndexOf(".")+1)) {
            case "war":
                createElement("webApplication", app, artifactId);
                break;
            case "ear":
                createElement("enterpriseApplication", app, artifactId);
                break;
            case "rar":
                createElement("resourceAdapter", app, artifactId);
                break;
            default:
                break;
        }
    }    
 
    public void createElement(String element, File appFile, String artifactId) {
        Element child = doc.createElement(element);
        child.setAttribute("id", artifactId);
        child.setAttribute("location", appFile.getName());
        child.setAttribute("name", artifactId);
        doc.getDocumentElement().appendChild(child);
    }
    
    public void writeApplicationXmlDocument(File serverDirectory) throws IOException, TransformerException {
        File applicationXml = getApplicationXmlFile(serverDirectory);
        if (!applicationXml.getParentFile().exists()) {
            applicationXml.getParentFile().mkdirs();
        }
        writeXMLDocument(applicationXml);
    }
    
    public static File getApplicationXmlFile(File serverDirectory) {
        File f = new File(serverDirectory, "configDropins/defaults/" + APP_XML_FILENAME); 
        return f;
    }
        
    public boolean hasChildElements() {
        return doc.getDocumentElement().getChildNodes().getLength() > 0;
    }
}
