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
package net.wasdev.wlp.maven.plugins.server;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;

import net.wasdev.wlp.maven.plugins.PluginConfigXmlDocument;
import net.wasdev.wlp.maven.plugins.ServerXmlDocument;

import org.apache.maven.model.Profile;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Basic Liberty Mojo Support
 * 
 * 
 */
public class PluginConfigSupport extends StartDebugMojoSupport {
    
    /**
     * Packages to install. One of "all", "dependencies" or "project".
     */
    @Parameter(property = "installAppPackages", defaultValue = "dependencies", readonly = true)
    private String installAppPackages = "dependencies";
    
    /**
     * Application directory.
     */
    @Parameter(property = "appsDirectory", readonly = false)
    protected String appsDirectory = null;
    
    /**
     * Strip version.
     */
    @Parameter(property = "stripVersion", defaultValue = "false", readonly = true)
    private boolean stripVersion = false;
    
    /**
     * Loose application. 
     */
    @Parameter(property = "looseApplication", defaultValue = "false", readonly = true)
    private boolean looseApplication;
    
    @Component
    private BuildContext buildContext;
    
    protected final String PLUGIN_CONFIG_XML = "liberty-plugin-config.xml";
    
    @Override
    protected void installServerAssembly() throws Exception {
        File f = exportParametersToXml();
        super.installServerAssembly();
        this.buildContext.refresh(f);
        this.buildContext.refresh(installDirectory);
    }
    
    /*
     * Export plugin configuration parameters to target/liberty-plugin-config.xml
     */
    protected File exportParametersToXml() throws Exception {
        PluginConfigXmlDocument configDocument = PluginConfigXmlDocument.newInstance("liberty-plugin-config");
        
        @SuppressWarnings("unchecked")
        List<Profile> profiles = project.getActiveProfiles();
        configDocument.createActiveBuildProfilesElement("activeBuildProfiles", profiles);
        
        configDocument.createElement("installDirectory", installDirectory);
        configDocument.createElement("serverDirectory", serverDirectory);
        configDocument.createElement("userDirectory", userDirectory);
        configDocument.createElement("serverOutputDirectory", new File(outputDirectory, serverName));
        configDocument.createElement("serverName", serverName);
        configDocument.createElement("configDirectory", configDirectory);
        
        if (getFileFromConfigDirectory("server.xml", configFile) != null) {
            configDocument.createElement("configFile", getFileFromConfigDirectory("server.xml", configFile));
        }
        if (getFileFromConfigDirectory("bootstrap.properties", bootstrapPropertiesFile) != null) {
            configDocument.createElement("bootstrapPropertiesFile", getFileFromConfigDirectory("bootstrap.properties", bootstrapPropertiesFile));
        } else {
            configDocument.createElement("bootstrapProperties", bootstrapProperties);
        }
        if (getFileFromConfigDirectory("jvm.option", jvmOptionsFile) != null) {
            configDocument.createElement("jvmOptionsFile", getFileFromConfigDirectory("jvm.option", jvmOptionsFile));
        } else {
            configDocument.createElement("jvmOptions", jvmOptions);
        }
        if (getFileFromConfigDirectory("server.env", serverEnv) != null) {
            configDocument.createElement("serverEnv", getFileFromConfigDirectory("server.env", serverEnv));
        }
        
        configDocument.createElement("appsDirectory", getAppsDirectory());
        configDocument.createElement("looseApplication", looseApplication);
        // TDOD: remove looseConfig when WDT starts to use looseApplication
        configDocument.createElement("looseConfig", looseApplication);
        configDocument.createElement("stripVersion", stripVersion);
        configDocument.createElement("installAppPackages", installAppPackages);
        configDocument.createElement("applicationFilename", getApplicationFilename());
        
        configDocument.createElement("assemblyArtifact", assemblyArtifact);   
        configDocument.createElement("assemblyArchive", assemblyArchive);
        configDocument.createElement("assemblyInstallDirectory", assemblyInstallDirectory);
        configDocument.createElement("refresh", refresh);
        configDocument.createElement("install", install);
        
        // write XML document to file
        File f = new File(project.getBuild().getDirectory() + File.separator + PLUGIN_CONFIG_XML);
        configDocument.writeXMLDocument(f);
        return f;
    }
    
    /* 
     * Get the file from configDrectory if it exists;
     * otherwise return def only if it exists, or null if not
     */
    protected File getFileFromConfigDirectory(String file, File def) {
        File f = new File(configDirectory, file);
        if (configDirectory != null && f.exists()) { 
            return f;
        }
        if (def != null && def.exists()) {
            return def;
        } 
        return null;
    }
    
    /*
     * return the filename of the project artifact to be installed by install-apps goal
     */
    protected String getApplicationFilename() {
        // project artifact has not be created yet when create-server goal is called in pre-package phase
        String name = project.getBuild().getFinalName();
        if (stripVersion) {
            int versionBeginIndex = project.getBuild().getFinalName().lastIndexOf("-" + project.getVersion());
            if ( versionBeginIndex != -1) {
                name = project.getBuild().getFinalName().substring(0, versionBeginIndex);
            }
        }
        
        // liberty only supports these application types: ear, war, eba, esa
        switch (project.getPackaging()) {
        case "ear":
        case "war":
        case "eba":
        case "esa":
            name += "." + project.getPackaging();
            if (looseApplication) {
                name += ".xml";
            }
            break;
        case "liberty-assembly":
            // assuming liberty-assembly project will also have a war file output.
            File dir = new File(project.getBasedir() + "/src/main/webapp");
            if (dir.exists()) {
                name += ".war";
                if (looseApplication) {
                    name += ".xml";
                }
            }
            break;
        default:
            log.debug("The project artifact cannot be installed to a Liberty server because " +
                    project.getPackaging() + " is not a supported packaging type.");
            name = null;
            break;
        }
        
        return name;
    }
    
    // Strip version string from name
    protected String stripVersionFromName(String name, String version) {
        int versionBeginIndex = name.lastIndexOf("-" + version);
        if ( versionBeginIndex != -1) {
            return name.substring(0, versionBeginIndex) + name.substring(versionBeginIndex + version.length() + 1);
        } else {
            return name;
        }
    }
    
    protected void addAppConfiguration(String artifactId) throws Exception {
   
        log.warn(messages.getString("info.install.app.not.configured"));
         
        // Add webApplication configuration into the target server.xml. 
        File serverXML = new File(serverDirectory, "server.xml");
        ServerXmlDocument.addAppElment(serverXML, artifactId);

        log.info(MessageFormat.format(messages.getString("info.install.app.add.configuration"), artifactId));
    }
    
    protected boolean isApplicationConfigured() throws Exception {
        File serverXML = new File(serverDirectory, "server.xml");
        return ServerXmlDocument.isFoundTagNames(serverXML.getCanonicalPath(), 	
                                                 new String[] {"application", "webApplication"});
    }
    
    protected String getAppsDirectory() {
    	if (appsDirectory == null)
    		return "dropins";
    	
    	return appsDirectory;
    }
    
//    /* 
//     * Get the file from configDrectory if it exists;
//     * otherwise return def only if it exists, or null if not
//     */
//    protected File getFileFromConfigDirectory(String file, File def) {
//        File f = new File(configDirectory, file);
//        if (configDirectory != null && f.exists()) { 
//            return f;
//        }
//        if (def != null && def.exists()) {
//            return def;
//        } 
//        return null;
//    }

}
