/**
 * (C) Copyright IBM Corporation 2014,2017.
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
package net.wasdev.wlp.maven.plugins.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.model.Profile;
import org.codehaus.mojo.pluginsupport.util.ArtifactItem;
import org.codehaus.plexus.util.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.wasdev.wlp.ant.ServerTask;
import net.wasdev.wlp.maven.plugins.Install;

/**
 * Create a liberty server
 * 
 * @goal create-server
 * 
 * 
 */
public class CreateServerMojo extends StartDebugMojoSupport {

    /**
     * Name of the template to use when creating a server.
     * 
     * @parameter expression="${template}"
     */
    private String template;
    
    /**
     * Packages to install. One of "all", "dependencies" or "project".
     * 
     * @parameter property="installAppPackages" default-value="dependencies"
     * @readonly
     */
    private String installAppPackages = "dependencies";
    
    /**
     * Application directory.
     * 
     * @parameter property="appsDirectory" default-value="dropins"
     * @readonly
     */
    private String appsDirectory = "dropins";
    
    /**
     * Strip version.
     * 
     * @parameter property="stripVersion" default-value="false"
     * @readyOnly
     */
    private boolean stripVersion = false;
    
    /**
     * Loose configuration. 
     * 
     * @parameter property="looseConfig" default-value="false"
     * @readonly
     */
    private boolean looseConfig=false;
    
    private final String PLUGIN_CONFIG_XML = "liberty-plugin-config.xml";
    
    @Override
    protected void doExecute() throws Exception {
        if (skip) {
            return;
        }
        if (isInstall) {
            installServerAssembly();
        } else {
            log.info(MessageFormat.format(messages.getString("info.install.type.preexisting"), ""));
            checkServerHomeExists();
        }

        boolean createServer = false;

        if (!serverDirectory.exists()) {
            createServer = true;
        } else if (refresh) {
            FileUtils.forceDelete(serverDirectory);
            createServer = true;
        }

        if (createServer) {
            // server does not exist or we are refreshing it - create it
            log.info(MessageFormat.format(messages.getString("info.server.start.create"), serverName));
            ServerTask serverTask = initializeJava();
            serverTask.setOperation("create");
            serverTask.setTemplate(template);
            serverTask.execute();
            // copy files _after_ we create the server
            copyConfigFiles(true);
            exportParametersToXml();
            log.info(MessageFormat.format(messages.getString("info.server.create.created"), serverName, serverDirectory.getCanonicalPath()));
        } else {
            // server exists - copy files over
            copyConfigFiles();
            exportParametersToXml();
        }
    }
    
    /*
     * Export configuration parameters to
     * ${project.build.directory}/liberty-plugin-config.xml.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
	private void exportParametersToXml() throws Exception {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        doc.setXmlStandalone(true);
        
        // set output XML root element
        Element rootElement = doc.createElement("liberty-plugin-config");
        doc.appendChild(rootElement);
        
        createActiveBuildProfilesElement(doc, rootElement, "activeBuildProfiles", project.getActiveProfiles());
        createElement(doc, rootElement, "installDirectory", installDirectory);
        createElement(doc, rootElement, "serverDirectory", serverDirectory);
        createElement(doc, rootElement, "userDirectory", userDirectory);
        // resolved to actual server output direcoty 
        createElement(doc, rootElement, "serverOutputDirectory", new File(outputDirectory, serverName));
        createElement(doc, rootElement, "serverName", serverName);
        
        if (!containsFile(configDirectory, "server.xml")) {
        	createElement(doc, rootElement, "configFile", configFile);
        }	
        if (!containsFile(configDirectory, "bootstrapPropertiesFile")) {
        	if (bootstrapPropertiesFile.exists()) {
        		createElement(doc, rootElement, "bootstrapPropertiesFile", bootstrapPropertiesFile);
        	} else {
        		createElement(doc, rootElement, "bootstrapProperties", bootstrapProperties);
        	}
        }
        if (!containsFile(configDirectory, "jvm.options")) {
        	if (jvmOptionsFile.exists()) {
        		createElement(doc, rootElement, "jvmOptionsFile", jvmOptionsFile);
        	} else {
        		createElement(doc, rootElement, "jvmOptions", jvmOptions);
        	}
        }
        if (containsFile(configDirectory, "server.env")) {
        	 createElement(doc, rootElement, "serverEnv", serverEnv);
        }
        
        createElement(doc, rootElement, "appsDirectory", appsDirectory);
        createElement(doc, rootElement, "looseConfig", looseConfig);
        createElement(doc, rootElement, "stripVersion", stripVersion);
        createElement(doc, rootElement, "installAppPackages", installAppPackages);

        createElement(doc, rootElement, "assemblyArtifact", assemblyArtifact);   
        createElement(doc, rootElement, "assemblyArchive", assemblyArchive);
        createElement(doc, rootElement, "assemblyInstallDirectory", assemblyInstallDirectory);
        createElement(doc, rootElement, "refresh", refresh);
        createElement(doc, rootElement, "install", install);
        
        // write XML document to file
        writeXMLDocument(doc, project.getBuild().getDirectory() + File.separator + PLUGIN_CONFIG_XML);
    }
    
    private void createActiveBuildProfilesElement(Document doc, Element root, String name, List<Profile> value) {
    	if (value == null || value.isEmpty()) {
    		return;
    	}
    	Element child = doc.createElement(name);
    	for (int i = 0; i < value.size(); i++) {
    	    createElement(doc, child, "profileId", value.get(i).getId());
    	}
    	root.appendChild(child);
    }
    
    private void createElement(Document doc, Element root, String key, boolean value) {
		createElement(doc, root, key, Boolean.toString(value));
    }
    
    private void createElement(Document doc, Element root, String key, File value) throws IOException {
		if (value == null) {
			return;
		} else {
			createElement(doc, root, key, value.getCanonicalPath());
		}
    }
    
    private void createElement(Document doc, Element root, String name, Map<String, String> value) {
    	if (value == null || value.isEmpty()) {
    		return;
    	}
    	Element child = doc.createElement(name);
    	for (Map.Entry<String, String> entry : value.entrySet()) {
    	    createElement(doc, child, entry.getKey(), entry.getValue());
    	}
    	root.appendChild(child);
    }
    
    private void createElement(Document doc, Element root, String name, List<String> value) {
    	if (value == null || value.isEmpty()) {
    		return;
    	}
    	Element child = doc.createElement(name);
    	for (int i = 0; i < value.size(); i++) {
    	    createElement(doc, child, "param", value.get(i));
    	}
    	root.appendChild(child);
    }
    
    private void createElement(Document doc, Element root, String name, ArtifactItem value) {
    	if (value == null) {
    		return;
    	}
    	Element child = doc.createElement(name);
    	createElement(doc, child, "groupId", value.getGroupId());
    	createElement(doc, child, "artifactId", value.getArtifactId());
    	createElement(doc, child, "version", value.getVersion());
    	createElement(doc, child, "version", value.getType());
    	root.appendChild(child);
    }
    
    private void createElement(Document doc, Element root, String name, Install value) {
    	if (value == null) {
    		return;
    	}
    	Element child = doc.createElement(name);
    	
        if (value.getCacheDirectory() != null) {
        	createElement(doc, child, "cacheDirectory", value.getCacheDirectory());
        }
        if (value.getLicenseCode() != null) {
        	createElement(doc, child, "licenseCode", value.getLicenseCode());
        }
        if (value.getType() != null) {
        	createElement(doc, child, "type", value.getType());
        }
        if (value.getType() != null) {
        	createElement(doc, child, "type", value.getType());
        }
        if (value.getRuntimeUrl() != null) {
        	createElement(doc, child, "runtimeUrl", value.getRuntimeUrl());
        }
        if (value.getUsername() != null) {
        	createElement(doc, child, "username", value.getUsername());
        }
        if (value.getPassword() != null) {
        	createElement(doc, child, "password", "*********");
        }
        createElement(doc, child, "maxDownloadTime", Long.toString(value.getMaxDownloadTime()));
        if (value.getRuntimeUrl() != null) {
        	createElement(doc, child, "runtimeUrl", value.getRuntimeUrl());
        }
        createElement(doc, child, "type", value.isVerbose());
        root.appendChild(child);
    }
    
    private void createElement(Document doc, Element root, String key, String value) {
		Element child = doc.createElement(key);
		child.appendChild(doc.createTextNode(value));
		root.appendChild(child);
    }
    
    private boolean containsFile(File configDir, String file) {
    	if (configDir == null) {
    		return false;
    	} else {
    		File f = new File(configDir, file);
    		if (!f.exists()) {
    			return false;
    		}
    	}
    	return true;
    }
    
    private void writeXMLDocument(Document doc, String fileName) throws IOException, TransformerException {
    	File f = new File(fileName);
    	if (!f.getParentFile().exists()) {
    		f.getParentFile().mkdirs();
    	}
        FileOutputStream outFile = new FileOutputStream(f);
        
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(outFile);
        
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "yes");
        transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
        transformer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        
        transformer.transform(source, result);
        outFile.close();
    }
}
