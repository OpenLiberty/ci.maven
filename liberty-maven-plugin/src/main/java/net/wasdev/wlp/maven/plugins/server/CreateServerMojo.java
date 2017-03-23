/**
 * (C) Copyright IBM Corporation 2014, 2017.
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

import org.apache.maven.model.Profile;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;

import net.wasdev.wlp.ant.ServerTask;
import net.wasdev.wlp.maven.plugins.PluginConfigXmlDocument;

/**
 * Create a liberty server
  */
@Mojo(name = "create-server") 
public class CreateServerMojo extends StartDebugMojoSupport {

    /**
     * Name of the template to use when creating a server.
     */
    @Parameter(property = "template")
    private String template;
    
    /**
     * Packages to install. One of "all", "dependencies" or "project".
     */
    @Parameter(property = "installAppPackages", defaultValue = "dependencies", readonly = true)
    private String installAppPackages = "dependencies";
    
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
            log.info(MessageFormat.format(messages.getString("info.server.create.created"), serverName, serverDirectory.getCanonicalPath()));
        }
        
        // copy files _after_ we create the server
        copyConfigFiles();
        exportParametersToXml();
    }
    
    /*
     * Export plugin configuration parameters to target/liberty-plugin-config.xml
     */
    private void exportParametersToXml() throws Exception {
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
        
        setAppsDirectory();
       
        configDocument.createElement("appsDirectory", appsDirectory);
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
        configDocument.writeXMLDocument(project.getBuild().getDirectory() + File.separator + PLUGIN_CONFIG_XML);
    }
    
    /*
     * return the filename of the project artifact to be installed by install-apps goal
     */
    private String getApplicationFilename() {
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
}
