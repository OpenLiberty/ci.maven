/**
 * (C) Copyright IBM Corporation 2017, 2019.
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
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Profile;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.sonatype.plexus.build.incremental.BuildContext;

import io.openliberty.tools.maven.PluginConfigXmlDocument;
import io.openliberty.tools.common.plugins.config.ApplicationXmlDocument;
import net.wasdev.wlp.maven.plugins.utils.CommonLogger;
import io.openliberty.tools.common.plugins.config.ServerConfigDocument;

/**
 * Basic Liberty Mojo Support
 * 
 * 
 */
public class PluginConfigSupport extends StartDebugMojoSupport {

    /**
     * Application directory.
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
    @Parameter(property = "looseApplication", defaultValue = "false")
    protected boolean looseApplication;

    /**
     * Packages to install. One of "all", "dependencies" or "project".
     */
    @Parameter(property = "installAppPackages", defaultValue = "dependencies")
    private String installAppPackages;

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

    protected String getInstallAppPackages() {
        if ("ear".equals(project.getPackaging())) {
            installAppPackages = "project";
        }
        return installAppPackages;
    }

    /*
     * Export plugin configuration parameters to
     * target/liberty-plugin-config.xml
     */
    protected File exportParametersToXml() throws Exception {
        PluginConfigXmlDocument configDocument = PluginConfigXmlDocument.newInstance("liberty-plugin-config");

        List<Profile> profiles = project.getActiveProfiles();
        configDocument.createActiveBuildProfilesElement("activeBuildProfiles", profiles);

        configDocument.createElement("installDirectory", installDirectory);
        configDocument.createElement("serverDirectory", serverDirectory);
        configDocument.createElement("userDirectory", userDirectory);
        configDocument.createElement("serverOutputDirectory", new File(outputDirectory, serverName));
        configDocument.createElement("serverName", serverName);
        configDocument.createElement("configDirectory", configDirectory);

        File configFile = findConfigFile("server.xml", serverXmlFile);
        if (configFile != null) {
            configDocument.createElement("configFile", configFile);
        }

        if (bootstrapProperties != null) {
            configDocument.createElement("bootstrapProperties", bootstrapProperties);
        } else {
            configFile = findConfigFile("bootstrap.properties", bootstrapPropertiesFile);
            if (configFile != null) {
                configDocument.createElement("bootstrapPropertiesFile", configFile);
            }
        }

        if (jvmOptions != null) {
            configDocument.createElement("jvmOptions", jvmOptions);
        } else {
            configFile = findConfigFile("jvm.options", jvmOptionsFile);
            if (configFile != null) {
                configDocument.createElement("jvmOptionsFile", configFile);
            }
        }

        configFile = findConfigFile("server.env", serverEnvFile);
        if (configFile != null) {
            configDocument.createElement("serverEnv", configFile);
        }

        configDocument.createElement("appsDirectory", getAppsDirectory());
        configDocument.createElement("looseApplication", looseApplication);
        configDocument.createElement("stripVersion", stripVersion);
        configDocument.createElement("installAppPackages", getInstallAppPackages());
        configDocument.createElement("applicationFilename", getApplicationFilename());
        configDocument.createElement("assemblyArtifact", assemblyArtifact);
        configDocument.createElement("assemblyArchive", assemblyArchive);
        configDocument.createElement("assemblyInstallDirectory", assemblyInstallDirectory);
        configDocument.createElement("refresh", refresh);
        configDocument.createElement("install", install);

        configDocument.createElement("installAppsConfigDropins",
                ApplicationXmlDocument.getApplicationXmlFile(serverDirectory));
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
        if ("dependencies".equals(getInstallAppPackages())) {
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
            log.debug("The project artifact cannot be installed to a Liberty server because " + project.getPackaging()
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

    protected boolean isAppConfiguredInSourceServerXml(String fileName) {

        Set<String> locations = getAppConfigLocationsFromSourceServerXml();

        if (locations.contains(fileName)) {
            log.debug("Application configuration is found in server.xml : " + fileName);
            return true;
        } else {
            return false;
        }
    }

    protected boolean isAnyAppConfiguredInSourceServerXml() {

        Set<String> locations = getAppConfigLocationsFromSourceServerXml();

        if (locations.size() > 0) {
            log.debug("Application configuration is found in server.xml.");
            return true;
        } else {
            return false;
        }
    }
    
    protected Set<String> getAppConfigLocationsFromSourceServerXml() {

        ServerConfigDocument scd = null;

        File serverXML = new File(serverDirectory, "server.xml");

        if (serverXML != null && serverXML.exists()) {
            try {
                scd = ServerConfigDocument.getInstance(CommonLogger.getInstance(), serverXML, configDirectory,
                        bootstrapPropertiesFile, bootstrapProperties, serverEnvFile);
            } catch (Exception e) {
                log.warn(e.getLocalizedMessage());
                log.debug(e);
            }
        }
        return scd != null ? scd.getLocations() : new HashSet<String>();
    }

    protected String getAppsDirectory() {
        if (appsDirectory != null && !appsDirectory.isEmpty()) {
            if ("dropins".equals(appsDirectory) || "apps".equals(appsDirectory)) {
                return appsDirectory;
            } else {
                log.warn(MessageFormat.format(messages.getString("warn.invalid.app.directory"), appsDirectory));
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
        log.info(MessageFormat.format(messages.getString("info.default.app.directory"), appsDirectory));
        return appsDirectory;
    }

    protected File getLibertyAssemblyWarSourceDirectory(MavenProject proj) {
        if ("liberty-assembly".equals(project.getPackaging())
                && (looseApplication
                        && (getInstallAppPackages().equals("all") || getInstallAppPackages().equals("project")))
                || project.getGoalConfiguration("org.apache.maven.plugins", "maven-war-plugin", null, null) != null) {
            return getWarSourceDirectory(project);
        }
        return null;
    }

    protected File getWarSourceDirectory(MavenProject proj) {
        String dir = getPluginConfiguration(proj, "org.apache.maven.plugins", "maven-war-plugin", "warSourceDirectory");
        if (dir != null) {
            return new File(proj.getBasedir(), dir);
        } else {
            return new File(proj.getBasedir(), "src/main/webapp");
        }
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
