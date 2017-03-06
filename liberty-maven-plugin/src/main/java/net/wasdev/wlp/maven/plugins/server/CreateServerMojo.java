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
@Mojo( name = "create-server" ) 
public class CreateServerMojo extends StartDebugMojoSupport {

    /**
     * Name of the template to use when creating a server.
     */
    @Parameter
    private String template;
    
    /**
     * Packages to install. One of "all", "dependencies" or "project".
     */
    @Parameter( property="create-server.installAppPackages", defaultValue="dependencies", readonly=true )
    private String installAppPackages = "dependencies";
    
    /**
     * Application directory.
     */
    @Parameter( property="create-server.appsDirectory", defaultValue="dropins", readonly=true )
    private String appsDirectory = "dropins";
    
    /**
     * Strip version.
     */
    @Parameter( property="create-server.stripVersion", defaultValue="false", readonly=true )
    private boolean stripVersion = false;
    
    /**
     * Loose configuration. 
     */
    @Parameter( property="create-server.looseConfig", defaultValue="false", readonly=true )
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
        
        configDocument.createElement("appsDirectory", appsDirectory);
        configDocument.createElement("looseConfig", looseConfig);
        configDocument.createElement("stripVersion", stripVersion);
        configDocument.createElement("installAppPackages", installAppPackages);

        configDocument.createElement("assemblyArtifact", assemblyArtifact);   
        configDocument.createElement("assemblyArchive", assemblyArchive);
        configDocument.createElement("assemblyInstallDirectory", assemblyInstallDirectory);
        configDocument.createElement("refresh", refresh);
        configDocument.createElement("install", install);
        
        // write XML document to file
        configDocument.writeXMLDocument(project.getBuild().getDirectory() + File.separator + PLUGIN_CONFIG_XML);
    }
    
    /* 
     * Get the file from configDrectory if it exists;
     * otherwise return def only if it exists, or null if not
     */
    private File getFileFromConfigDirectory(String file, File def) {
        File f = new File(configDirectory, file);
        if (configDirectory != null && f.exists()) { 
            return f;
        }
        if (def != null && def.exists()) {
            return def;
        } 
        return null;
    }
}
