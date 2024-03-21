/**
 * (C) Copyright IBM Corporation 2017, 2024.
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
package io.openliberty.tools.maven.server;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.sonatype.plexus.build.incremental.BuildContext;

import io.openliberty.tools.maven.PluginConfigXmlDocument;
import io.openliberty.tools.maven.utils.CommonLogger;
import io.openliberty.tools.common.CommonLoggerI;
import io.openliberty.tools.common.plugins.config.ApplicationXmlDocument;
import io.openliberty.tools.common.plugins.config.ServerConfigDocument;

/**
 * Basic Liberty Mojo Support
 * 
 * 
 */
public abstract class PluginConfigSupport extends StartDebugMojoSupport {

    protected ServerConfigDocument scd = null;

    /**
     * Application directory. Either "apps" or "dropins". This defaults to "apps" when there is 
     * some application configured in server XML config and "dropins" when there is not.
     */
    @Parameter(property = "appsDirectory")
    protected String appsDirectory;

    /**
     * Strip version.
     */
    @Parameter(property = "stripVersion", defaultValue = "false")
    protected boolean stripVersion;

    /**
     * Loose application.
     */
    @Parameter(property = "looseApplication", defaultValue = "true")
    protected boolean looseApplication;

    /**
     * Packages to install. One of "all", "dependencies", "project", or "spring-boot-project".
     */
    @Parameter(property = "deployPackages", defaultValue = "project", alias = "installAppPackages")
    private String deployPackages;

    @Component
    private BuildContext buildContext;

    protected final String PLUGIN_CONFIG_XML = "liberty-plugin-config.xml";

    @Override
    protected void installServerAssembly() throws MojoExecutionException {
        try {
            File f = exportParametersToXml(false);
            super.installServerAssembly();
            this.buildContext.refresh(f);
            this.buildContext.refresh(installDirectory);
        } catch (IOException | ParserConfigurationException | TransformerException e) {
            throw new MojoExecutionException("Error installing Liberty server.", e);
        }
     }

    protected String getDeployPackages() {
        if ("ear".equals(project.getPackaging())) {
            deployPackages = "project";
        }
        return deployPackages;
    }

    /**
     * Overriding this method to add the export of the parameters to the xml file. This method will get called for the
     * following goals: create, deploy, run and dev
     * @throws IOException
     * @throws MojoExecutionException
     */
    @Override
    protected void copyConfigFiles() throws IOException, MojoExecutionException {
        try {
            super.copyConfigFiles();
            exportParametersToXml();
        } catch (IOException | ParserConfigurationException |  TransformerException e) {
            throw new MojoExecutionException("Error copying configuration files to Liberty server directory.", e);            
        }
    }

    /*
     * Export plugin configuration parameters to
     * target/liberty-plugin-config.xml
     */
    protected File exportParametersToXml() throws IOException, ParserConfigurationException, TransformerException {
        return exportParametersToXml(true);
    }

    /*
     * Export plugin configuration parameters to
     * target/liberty-plugin-config.xml
     */
    protected File exportParametersToXml(boolean includeServerInfo) throws IOException, ParserConfigurationException, TransformerException {
        PluginConfigXmlDocument configDocument = PluginConfigXmlDocument.newInstance("liberty-plugin-config");

        // install related info (common parameters)
        configDocument.createElement("installDirectory", installDirectory);
        if (installType != InstallType.ALREADY_EXISTS) {
            configDocument.createElement("assemblyArtifact", assemblyArtifact);
            configDocument.createElement("assemblyArchive", assemblyArchive);
            configDocument.createElement("assemblyInstallDirectory", assemblyInstallDirectory);    
        }
        configDocument.createElement("refresh", refresh);
        configDocument.createElement("install", install);

        // even though these are related to the server, they are specified in the common parameters for the install
        // and are initialized in the BasicSupport.init() method.
        configDocument.createElement("serverDirectory", serverDirectory);
        configDocument.createElement("userDirectory", userDirectory);
        configDocument.createElement("serverOutputDirectory", new File(outputDirectory, serverName));
        configDocument.createElement("serverName", serverName);

        // project related info
        List<Profile> profiles = project.getActiveProfiles();
        configDocument.createActiveBuildProfilesElement("activeBuildProfiles", profiles);

        configDocument.createElement("projectType", project.getPackaging());
        if (project.getParent() != null && !project.getParent().getModules().isEmpty()) {
            configDocument.createElement("aggregatorParentId", project.getParent().getArtifactId());
            configDocument.createElement("aggregatorParentBasedir", project.getParent().getBasedir());
        }

        // returns all current project compile dependencies, including
        // transitive dependencies
        // if Mojo required dependencyScope is set to COMPILE
        Set<Artifact> artifacts = project.getArtifacts();
        for (Artifact artifact : artifacts) {
            if ("compile".equals(artifact.getScope()) || "runtime".equals(artifact.getScope())) {
                configDocument.createElement("projectCompileDependency",
                        artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion());
            }
        }

        // include warSourceDirectory for liberty-assembly project with source
        configDocument.createElement("warSourceDirectory", getLibertyAssemblyWarSourceDirectory(project));

        if (includeServerInfo) {
            // include all info related specifically to the server (commmon server parameters)
             configDocument.createElement("configDirectory", configDirectory);

            File configFile = findConfigFile("server.xml", serverXmlFile);
            if (configFile != null) {
                configDocument.createElement("configFile", configFile);
            }

            if (combinedBootstrapProperties != null) {
                configDocument.createElement("bootstrapProperties", combinedBootstrapProperties);
            } else if (bootstrapProperties != null) {
                if (bootstrapPropertiesResolved == null) {
                    bootstrapPropertiesResolved = handleLatePropertyResolution(bootstrapProperties);
                }
                configDocument.createElement("bootstrapProperties", bootstrapPropertiesResolved);
            } else {
                configFile = findConfigFile("bootstrap.properties", bootstrapPropertiesFile);
                if (configFile != null) {
                    configDocument.createElement("bootstrapPropertiesFile", configFile);
                }
            }

            if (combinedJvmOptions != null) {
                configDocument.createElement("jvmOptions", combinedJvmOptions);
            } else if (jvmOptions != null) {
                if (jvmOptionsResolved == null) {
                    jvmOptionsResolved = handleLatePropertyResolution(jvmOptions);
                }
                List<String> uniqueOptions = getUniqueValues(jvmOptionsResolved);
                configDocument.createElement("jvmOptions", uniqueOptions);
            } else {
                configFile = findConfigFile("jvm.options", jvmOptionsFile);
                if (configFile != null) {
                    configDocument.createElement("jvmOptionsFile", configFile);
                }
            }

            // Only write the serverEnvFile path if it was not overridden by liberty.env.{var} Maven properties.
            if (envMavenProps.isEmpty()) {
                configFile = findConfigFile("server.env", serverEnvFile);
                if (configFile != null) {
                    configDocument.createElement("serverEnv", configFile);
                }
            }

            // include info related to apps
            if (isConfigCopied()) {
                configDocument.createElement("appsDirectory", getAppsDirectory());
            }

            configDocument.createElement("looseApplication", looseApplication);
            configDocument.createElement("stripVersion", stripVersion);
            configDocument.createElement("installAppPackages", getDeployPackages());
            configDocument.createElement("applicationFilename", getApplicationFilename());


            configDocument.createElement("installAppsConfigDropins",
                    ApplicationXmlDocument.getApplicationXmlFile(serverDirectory));
        }

        // write XML document to file
        File f = new File(project.getBuild().getDirectory() + File.separator + PLUGIN_CONFIG_XML);
        configDocument.writeXMLDocument(f);
        return f;
    }

    /*
     * Return specificFile if it exists; otherwise return the file with the requested fileName from the 
     * configDirectory, but only if it exists. Null is returned if the file does not exist in either location.
     */
    protected File findConfigFile(String fileName, File specificFile) {
        if (specificFile != null && specificFile.exists()) {
            return specificFile;
        }

        File f = new File(configDirectory, fileName);
        if (configDirectory != null && f.exists()) {
            return f;
        }
        return null;
    }

    /*
     * Get the file from configDrectory if it exists; otherwise return def only
     * if it exists, or null if not
     *
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

    protected File getFileFromConfigDirectory(String file) {
        return getFileFromConfigDirectory(file, null);
    }

    *
     * return the filename of the project artifact to be installed by
     * install-apps goal
     */
    protected String getApplicationFilename() {
        // A project doesn't build a web application artifact but getting the
        // application artifacts from dependencies. e.g. liberty-assembly type
        // project.
        if ("dependencies".equals(getDeployPackages())) {
            return null;
        }

        // project artifact has not be created yet when create-server goal is
        // called in pre-package phase
        String name = project.getBuild().getFinalName();
        if (stripVersion) {
            int versionBeginIndex = project.getBuild().getFinalName().lastIndexOf("-" + project.getVersion());
            if (versionBeginIndex != -1) {
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
            // assuming liberty-assembly project will also have a war file
            // output.
            File dir = getWarSourceDirectory(project);
            if (dir.exists()) {
                name += ".war";
                if (looseApplication) {
                    name += ".xml";
                }
            }
            break;
        default:
            getLog().debug("The project artifact cannot be installed to a Liberty server because " + project.getPackaging()
                    + " is not a supported packaging type.");
            name = null;
            break;
        }

        return name;
    }

    // Strip version string from name
    protected String stripVersionFromName(String name, String version) {
        int versionBeginIndex = name.lastIndexOf("-" + version);
        if (versionBeginIndex != -1) {
            return name.substring(0, versionBeginIndex) + name.substring(versionBeginIndex + version.length() + 1);
        } else {
            return name;
        }
    }

    protected boolean isAppConfiguredInSourceServerXml(String fullyQualifiedFileName, String fileName) {

        Set<String> locations = getAppConfigLocationsFromSourceServerXml();

        // For Windows, modify fully qualified file name to use forward slashes since resolved app locations were stored that way
        fullyQualifiedFileName = fullyQualifiedFileName == null ? null : fullyQualifiedFileName.replace("\\","/");

        if (locations.contains(fileName) || ((fullyQualifiedFileName != null) && locations.contains(fullyQualifiedFileName))) {
            getLog().info("Application configuration is found in server.xml : " + fileName);
            return true;
        } else {
            getLog().info("Application configuration is not found in server.xml : " + fileName);
            return false;
        }
    }

    protected boolean isAnyAppConfiguredInSourceServerXml() {

        Set<String> locations = getAppConfigLocationsFromSourceServerXml();

        if (locations.size() > 0) {
            getLog().debug("Application configuration is found in server.xml.");
            return true;
        } else {
            return false;
        }
    }
    
    protected Set<String> getAppConfigLocationsFromSourceServerXml() {

        File serverXML = new File(serverDirectory, "server.xml");

        if (serverXML != null && serverXML.exists()) {
            try {
            Map<String, File> libertyDirPropertyFiles = getLibertyDirectoryPropertyFiles();
            CommonLogger logger = new CommonLogger(getLog());
            setLog(logger.getLog());
            // scd = getServerConfigDocument(logger, serverXML, configDirectory,
            //             bootstrapPropertiesFile, combinedBootstrapProperties, serverEnvFile, false,
            //             libertyDirPropertyFiles);
            scd = getServerConfigDocument(logger, serverXML, libertyDirPropertyFiles);
            } catch (Exception e) {
                getLog().warn(e.getLocalizedMessage());
                getLog().debug(e);
            }
        }
        return scd != null ? scd.getLocations() : new HashSet<String>();
    }

    protected ServerConfigDocument getServerConfigDocument(CommonLoggerI log, File serverXML, File configDir, File bootstrapFile,
            Map<String, String> bootstrapProp, File serverEnvFile, boolean giveConfigDirPrecedence, Map<String, File> libertyDirPropertyFiles) throws IOException {
        if (scd == null || !scd.getServerXML().getCanonicalPath().equals(serverXML.getCanonicalPath())) {
            scd = new ServerConfigDocument(log, serverXML, configDir, bootstrapFile, bootstrapProp, serverEnvFile, giveConfigDirPrecedence, libertyDirPropertyFiles);
        }

        return scd;
    }

    protected ServerConfigDocument getServerConfigDocument(CommonLoggerI log, File serverXML, Map<String, File> libertyDirPropertyFiles) throws IOException {
        if (scd == null || !scd.getServerXML().getCanonicalPath().equals(serverXML.getCanonicalPath())) {
            scd = new ServerConfigDocument(log, libertyDirPropertyFiles);
        }

        return scd;
    }

    protected String getAppsDirectory() {
        return getAppsDirectory(true);
    }

    protected String getAppsDirectory(boolean logDirectory) {
        if (appsDirectory != null && !appsDirectory.isEmpty()) {
            if ("dropins".equals(appsDirectory) || "apps".equals(appsDirectory)) {
                return appsDirectory;
            } else {
                getLog().warn(MessageFormat.format(messages.getString("warn.invalid.app.directory"), appsDirectory));
            }
        }

        // default appsDirectory
        appsDirectory = "dropins";
        File srcServerXML = findConfigFile("server.xml", serverXmlFile);
        if (srcServerXML != null && srcServerXML.exists() && isAnyAppConfiguredInSourceServerXml()) {
            // overwrite default appsDirectory if application configuration is
            // found.
            appsDirectory = "apps";
        }
        if (logDirectory) {
            getLog().info(MessageFormat.format(messages.getString("info.default.app.directory"), appsDirectory));
        }
        return appsDirectory;
    }

    protected File getLibertyAssemblyWarSourceDirectory(MavenProject proj) {
        if ("liberty-assembly".equals(project.getPackaging())
                && (looseApplication
                        && (getDeployPackages().equals("all") || getDeployPackages().equals("project")))
                || project.getGoalConfiguration("org.apache.maven.plugins", "maven-war-plugin", null, null) != null) {
            return getWarSourceDirectory(project);
        }
        return null;
    }

    protected File getWarSourceDirectory(MavenProject proj) {
        String dir = getPluginConfiguration(proj, "org.apache.maven.plugins", "maven-war-plugin", "warSourceDirectory");
        getLog().debug(
        		String.format("WAR source directory from POM: %s", dir));
        File warSourceDir;
        
        if (dir != null) {
        	File dirObj = new File(dir);
        	if (dirObj.isAbsolute()) {
        		warSourceDir = dirObj;
        	} else {
        		warSourceDir = new File(proj.getBasedir(), dir);
        	}
        } else {
        	warSourceDir = new File(proj.getBasedir(), "src/main/webapp");
        }
        getLog().debug(String.format("Final WAR source directory: %s (absolute: %s)", warSourceDir, warSourceDir.getAbsolutePath()));
        return warSourceDir;
    }

    private String getPluginConfiguration(MavenProject proj, String pluginGroupId, String pluginArtifactId,
            String key) {
        Xpp3Dom dom = proj.getGoalConfiguration(pluginGroupId, pluginArtifactId, null, null);
        if (dom != null) {
            Xpp3Dom val = dom.getChild(key);
            if (val != null) {
                return val.getValue();
            }
        }
        return null;
    }
}
