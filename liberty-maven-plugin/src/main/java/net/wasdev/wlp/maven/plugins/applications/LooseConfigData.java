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
package net.wasdev.wlp.maven.plugins.applications;

import java.io.File;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import net.wasdev.wlp.maven.plugins.XmlDocument;

public class LooseConfigData extends XmlDocument {
    
    public LooseConfigData() throws ParserConfigurationException {
        createDocument("archive");
    }
    
    public void addDir(String src, String target) {
        if (new File(src).exists()) {
            addDir(doc.getDocumentElement(), src, target);
        }
    }
    
    public void addDir(Element parent, String src, String target) {
        if (new File(src).exists()) {
            Element child = doc.createElement("dir");
            addElement(parent, child, target, src);
        }
    }
    
    public void addFile(String src, String target) {
        if (new File(src).exists()) {
            addFile(doc.getDocumentElement(), src, target);
        }
    }
    
    public void addFile(Element parent, String src, String target) {
        if (new File(src).exists()) {
            Element child = doc.createElement("file");
            addElement(parent, child, target, src);
        }
    }
    
    public Element addArchive(String target) {
        return addArchive(doc.getDocumentElement(), target);
    }
    
    public Element addArchive(Element parent, String target) {
        Element child = doc.createElement("archive");
        addElement(parent, child, target);
        return child;
    }
    
    public void addArchive(String src, String target) {
        Element child = addArchive(target);
        addElement(child, doc.createElement("dir"), "/", src);
    }
    
    public void toXmlFile(File xmlFile) throws Exception {        
        writeXMLDocument(xmlFile);
    }
    
    private void addElement(Element parent, Element child, String targetAttr, String srcAttr) {
        child.setAttribute("sourceOnDisk", srcAttr);
        addElement(parent, child, targetAttr);
    }
    
    private void addElement(Element parent, Element child, String targetAttr) {
        child.setAttribute("targetInArchive", targetAttr);
        parent.appendChild(child);
    }
}
