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
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.model.Profile;
import org.codehaus.mojo.pluginsupport.util.ArtifactItem;
import org.w3c.dom.Element;

import net.wasdev.wlp.common.plugins.util.XmlDocument;

public class PluginConfigXmlDocument extends XmlDocument {
    
    private PluginConfigXmlDocument() {    
    }
    
    public static PluginConfigXmlDocument newInstance(String rootElement) throws ParserConfigurationException {
        PluginConfigXmlDocument configDocument = new PluginConfigXmlDocument();
        configDocument.createDocument(rootElement);
        return configDocument;
    }
    
    public void createElement(String key, boolean value) {
        createElement(doc.getDocumentElement(), key, Boolean.toString(value));
    }
    
    public void createElement(Element element, String key, boolean value) {
        createElement(element, key, Boolean.toString(value));
    }
    
    public void createElement(String key, File value) throws IOException {
        if (value == null) {
            return;
        } else {
            createElement(doc.getDocumentElement(), key, value.getCanonicalPath());
        }
    }
    
    public void createElement(String name, Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        Element child = doc.createElement(name);
        for (Map.Entry<String, String> entry : values.entrySet()) {
            createElement(child, entry.getKey(), entry.getValue());
        }
        doc.getDocumentElement().appendChild(child);
    }
    
    public void createElement(String name, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        Element child = doc.createElement(name);
        for (int i = 0; i < values.size(); i++) {
            createElement(child, "param", values.get(i));
        }
        doc.getDocumentElement().appendChild(child);
    }
    
    public void createElement(String name, ArtifactItem value) {
        if (value == null) {
            return;
        }
        Element child = doc.createElement(name);
        createElement(child, "groupId", value.getGroupId());
        createElement(child, "artifactId", value.getArtifactId());
        createElement(child, "version", value.getVersion());
        createElement(child, "type", value.getType());
        doc.getDocumentElement().appendChild(child);
    }
    
    public void createElement(String name, Install value) {
        if (value == null) {
            return;
        }
        Element child = doc.createElement(name);
        
        createElement(child, "cacheDirectory", value.getCacheDirectory());
        createElement(child, "licenseCode", value.getLicenseCode());
        createElement(child, "type", value.getType());
        createElement(child, "version", value.getVersion());
        createElement(child, "runtimeUrl", value.getRuntimeUrl());
        createElement(child, "username", value.getUsername());
        createElement(child, "password", "*********");
        createElement(child, "maxDownloadTime", Long.toString(value.getMaxDownloadTime()));
        createElement(child, "runtimeUrl", value.getRuntimeUrl());
        createElement(child, "verbose", value.isVerbose());
        
        doc.getDocumentElement().appendChild(child);
    }
 
    public void createElement(String key, String value) {
        createElement(doc.getDocumentElement(), key, value);
    }
    
    public void createElement(Element elem, String key, String value) {
        if (value == null) {
            return;
        }
        Element child = doc.createElement(key);
        child.appendChild(doc.createTextNode(value));
        elem.appendChild(child);
    }
    
    public void createActiveBuildProfilesElement(String name, List<Profile> value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        Element child = doc.createElement(name);
        for (int i = 0; i < value.size(); i++) {
            createElement(child, "profileId", value.get(i).getId());
        }
        doc.getDocumentElement().appendChild(child);
    }
    
}
