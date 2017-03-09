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
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import net.wasdev.wlp.maven.plugins.XmlDocument;

public class LooseConfigData extends XmlDocument {
    
    private Map<String, String> dirs;
    
    private Map<String, String> files;
    
    private Map<String, String> archives;
    
    public LooseConfigData() {
        dirs = new HashMap<String, String>();
        files = new HashMap<String, String>();
        archives = new HashMap<String, String>();
    }
    
    public void addDir(String src, String target) {
        dirs.put(src, target);
    }
    
    public void addFile(String src, String target) {
        files.put(src, target);
    }
    
    public void addArchive(String src, String target) {
        archives.put(src, target);
    }
    
    public void toXmlFile(File xmlFile) throws Exception {
        createDocument("archive");
        
        if (!dirs.isEmpty()) {
            for(Map.Entry<String, String> entry : dirs.entrySet()){
                Element child = doc.createElement("dir");
                child.setAttribute("sourceOnDisk", entry.getKey());
                child.setAttribute("targetInArchive", entry.getValue());
                doc.getDocumentElement().appendChild(child);
            }
        }
        
        if (!files.isEmpty()) {
            for(Map.Entry<String, String> entry : files.entrySet()){
                Element child = doc.createElement("file");
                child.setAttribute("sourceOnDisk", entry.getKey());
                child.setAttribute("targetInArchive", entry.getValue());
                doc.getDocumentElement().appendChild(child);
            }
        }
        
        if (!archives.isEmpty()) {
            for(Map.Entry<String, String> entry : archives.entrySet()){
                Element child = doc.createElement("archive");
                child.setAttribute("targetInArchive", entry.getValue());
                doc.getDocumentElement().appendChild(child);
                Element grandChild = doc.createElement("dir");
                child.setAttribute("sourceOnDisk", entry.getKey());
                child.setAttribute("targetInArchive", "/");
                doc.getDocumentElement().appendChild(grandChild);
            }
        }
        
        writeXMLDocument(xmlFile);
    }
    
}
