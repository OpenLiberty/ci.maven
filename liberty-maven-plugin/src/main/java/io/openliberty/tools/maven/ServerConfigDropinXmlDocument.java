/**
 * (C) Copyright IBM Corporation 2019.
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
package io.openliberty.tools.maven;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.model.Profile;
import org.codehaus.mojo.pluginsupport.util.ArtifactItem;
import org.w3c.dom.Element;
import org.w3c.dom.Comment;

import io.openliberty.tools.common.plugins.config.XmlDocument;

public class ServerConfigDropinXmlDocument extends XmlDocument {
    
    private ServerConfigDropinXmlDocument() {    
    }
    
    public static ServerConfigDropinXmlDocument newInstance() throws ParserConfigurationException {
        ServerConfigDropinXmlDocument configDocument = new ServerConfigDropinXmlDocument();
        configDocument.createDocument("server");
        return configDocument;
    }

    public void createComment(String comment) {
        createComment(doc.getDocumentElement(), comment);
    }

    public void createComment(Element elem, String comment) {
        Comment commentElement = doc.createComment(comment);
        elem.appendChild(commentElement);
    }

    public void createVariableWithValue(String varName, String varValue, boolean isDefaultValue) {
        createVariableWithValue(doc.getDocumentElement(), varName, varValue, isDefaultValue);
    }
    
    public void createVariableWithValue(Element elem, String varName, String varValue, boolean isDefaultValue) {
        if (varValue == null) {
            return;
        }
        Element child = doc.createElement("variable");
        child.setAttribute("name", varName);
        String valueAttr = isDefaultValue ? "defaultValue" : "value";
        child.setAttribute(valueAttr, varValue);
        elem.appendChild(child);
    }
        
}
