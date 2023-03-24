/**
 * (C) Copyright IBM Corporation 2014, 2020.
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
package io.openliberty.tools.maven.applications;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import org.apache.tools.ant.taskdefs.Copy;

import io.openliberty.tools.maven.utils.SpringBootUtil;
import io.openliberty.tools.common.plugins.config.ApplicationXmlDocument;
import io.openliberty.tools.common.plugins.config.LooseConfigData;
import io.openliberty.tools.common.plugins.config.ServerConfigDocument;
import io.openliberty.tools.common.plugins.util.DevUtil;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * Copy applications to the specified directory of the Liberty server. 
 * The ResolutionScope.COMPILE_PLUS_RUNTIME includes compile + system + provided + runtime dependencies. 
 * The copyDependencies functionality will include dependencies with all those scopes, and transitive 
 * dependencies with scope compile + system + runtime. 
 * Provided scope transitive dependencies are not included by default (built-in maven behavior).
 */
@Mojo(name = "deploy", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class DeployMojo extends DeployMojoSupport {

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("\nSkipping deploy goal.\n");
            return;
        }

        try {
            doDeploy();
        } catch (IOException | ParserConfigurationException | TransformerException ioException) {
            throw new MojoExecutionException(ioException);
        }
    }

    private void doDeploy() throws IOException, MojoExecutionException, TransformerException, ParserConfigurationException {
        checkServerHomeExists();
        checkServerDirectoryExists();

        // Delete our generated configDropins XML (a new one will be generated if necessary)
        cleanupPreviousExecution();

        // update target server configuration
        copyConfigFiles();
        exportParametersToXml();

        boolean installDependencies = false;
        boolean installProject = false;

        switch (getDeployPackages()) {
            case "all":
                installDependencies = true;
                installProject = true;
                break;
            case "dependencies":
                installDependencies = true;
                break;
            case "project":
                installProject = true;
                break;
            case "spring-boot-project":
                installSpringBootApp();
                break;
            default:
                return;
        }

        if (installDependencies) {
            installDependencies();
        }
        if (installProject) {
            installProject();
        }

        // create application configuration in configDropins if it is not configured
        if (applicationXml.hasChildElements()) {
            getLog().warn(messages.getString("warn.install.app.add.configuration"));
            applicationXml.writeApplicationXmlDocument(serverDirectory);
        }
    }

    private void installSpringBootApp() throws IOException, MojoExecutionException {
        if (!SpringBootUtil.doesSpringBootRepackageGoalExecutionExist(project)) {
            throw new MojoExecutionException("The repackage goal of the spring-boot-maven-plugin must be configured to run first in order to create the required executable archive.");
        }

        File fatArchiveSrc = SpringBootUtil.getSpringBootUberJAR(project, getLog());
        
        // Check if the archiveSrc is executable and then invokeSpringUtilCommand. 
        if (io.openliberty.tools.common.plugins.util.SpringBootUtil.isSpringBootUberJar(fatArchiveSrc)) {
            File thinArchiveTarget = getThinArchiveTarget(fatArchiveSrc);
            File libIndexCacheTarget = getLibIndexCacheTarget();
            
            validateAppConfig(thinArchiveTarget.getName(), project.getArtifactId(), true);
            invokeSpringBootUtilCommand(installDirectory, fatArchiveSrc.getCanonicalPath(), thinArchiveTarget.getCanonicalPath(), libIndexCacheTarget.getCanonicalPath());
        } else {
            throw new MojoExecutionException(fatArchiveSrc.getCanonicalPath() +" file is not an executable archive. "
                    + "The repackage goal of the spring-boot-maven-plugin must be configured to run first in order to create the required executable archive.");
        }
    }
    
    private File getThinArchiveTarget(File archiveSrc) {
        String appsDirName = getAppsDirectory();
        File archiveTarget = null;
        File appsDir = null;
        File rootDirectory = serverDirectory;
        
        if (project.getProperties().containsKey("container")) {
            rootDirectory = new File(project.getBuild().getDirectory(), DevUtil.DEVC_HIDDEN_FOLDER);
        }
        
        if("apps".equals(appsDirName)){
            appsDir = new File(rootDirectory, appsDirName);        
        } else if ("dropins".equals(appsDirName)) {
            appsDir = new File(rootDirectory, appsDirName+"/spring");         
        }       
        archiveTarget = new File(appsDir, "thin-" + archiveSrc.getName());
        return archiveTarget;
    }
    
    private File getLibIndexCacheTarget() {
        // Set shared directory ${installDirectory}/usr/shared/
        File sharedDirectory = new File(userDirectory, "shared");
        
        //Set shared resources directory ${installDirectory}/usr/shared/resources/
        File sharedResourcesDirectory = new File(sharedDirectory, "resources");
        
        if(!sharedResourcesDirectory.exists()) {
            sharedResourcesDirectory.mkdirs();
        }
        File libIndexCacheTarget = new File(sharedResourcesDirectory, "lib.index.cache");
        return libIndexCacheTarget;
    }

    protected void installDependencies() throws MojoExecutionException, IOException {
        Set<Artifact> artifacts = project.getArtifacts();
        getLog().debug("Number of compile dependencies for " + project.getArtifactId() + " : " + artifacts.size());
        
        for (Artifact artifact : artifacts) {
            // skip if not an application type supported by Liberty
            if (!isSupportedType(artifact.getType())) {
                continue;
            }
            // skip assemblyArtifact if specified as a dependency
            if (assemblyArtifact != null && matches(artifact, assemblyArtifact)) {
                continue;
            }
            if (artifact.getScope().equals("compile")) {
                if (isSupportedType(artifact.getType())) {
                    if (looseApplication && isReactorMavenProject(artifact)) {  //Installing the reactor project artifacts
                        MavenProject dependProj = getReactorMavenProject(artifact);
                        installLooseApplication(dependProj);
                    } else {
                        installApp(resolveArtifact(artifact));
                    }
                } else {
                    getLog().warn(MessageFormat.format(messages.getString("error.application.not.supported"),
                            project.getId()));
                }
            }
        }
    }
    
    protected void installProject() throws MojoExecutionException, IOException {
        if (isSupportedType(project.getPackaging())) {
            if (looseApplication) {
                installLooseApplication(project);
            } else {
                installApp(project.getArtifact());
            }
        } else {
            throw new MojoExecutionException(
                    MessageFormat.format(messages.getString("error.application.not.supported"), project.getId()));
        }
    }

    private void installLooseApplication(MavenProject proj) throws IOException, MojoExecutionException {
        String looseConfigFileName = getLooseConfigFileName(proj);
        String application = looseConfigFileName.substring(0, looseConfigFileName.length() - 4);
        File destDir = new File(serverDirectory, getAppsDirectory());
        File looseConfigFile = new File(destDir, looseConfigFileName);

        File devcDestDir = new File(new File(project.getBuild().getDirectory(), DevUtil.DEVC_HIDDEN_FOLDER), getAppsDirectory());
        File devcLooseConfigFile = new File(devcDestDir, looseConfigFileName);

        LooseConfigData config = createLooseConfigData();

        switch (proj.getPackaging()) {
            case "war":
                validateAppConfig(application, proj.getArtifactId());
                getLog().info(MessageFormat.format(messages.getString("info.install.app"), looseConfigFileName));
                installLooseConfigWar(proj, config, false);
                installAndVerifyApp(config, looseConfigFile, application);
                if (proj.getProperties().containsKey("container")) {
                    // install another copy that is container specific
                    config = createLooseConfigData();
                    installLooseConfigWar(proj, config, true);
                    try {
                        config.toXmlFile(devcLooseConfigFile);
                    } catch (Exception xmlException) {
                        throw new MojoExecutionException("Error writing XML file " + devcLooseConfigFile + " from config " + config, xmlException);
                    }
                }
                break;
            case "ear":
                validateAppConfig(application, proj.getArtifactId());
                getLog().info(MessageFormat.format(messages.getString("info.install.app"), looseConfigFileName));
                installLooseConfigEar(proj, config, false);
                installAndVerifyApp(config, looseConfigFile, application);
                if (proj.getProperties().containsKey("container")) {
                    // install another copy that is container specific
                    config = createLooseConfigData();
                    installLooseConfigEar(proj, config, true);
                    try {
                        config.toXmlFile(devcLooseConfigFile);
                    } catch (Exception xmlException) {
                        throw new MojoExecutionException("Error writing XML file " + devcLooseConfigFile + " from config " + config, xmlException);
                    }
                }
                break;
            case "liberty-assembly":
                if (mavenWarPluginExists(proj) || new File(proj.getBasedir(), "src/main/webapp").exists()) {
                    validateAppConfig(application, proj.getArtifactId());
                    getLog().info(MessageFormat.format(messages.getString("info.install.app"), looseConfigFileName));
                    installLooseConfigWar(proj, config, false);
                    installAndVerifyApp(config, looseConfigFile, application);
                    if (proj.getProperties().containsKey("container")) {
                        // install another copy that is container specific
                        config = createLooseConfigData();
                        installLooseConfigWar(proj, config, true);
                        try {
                            config.toXmlFile(devcLooseConfigFile);
                        } catch (Exception xmlException) {
                            throw new MojoExecutionException("Error writing XML file " + devcLooseConfigFile + " from config " + config, xmlException);
                        }
                    }
                } else {
                    getLog().debug("The liberty-assembly project does not contain the maven-war-plugin or src/main/webapp does not exist.");
                }
                break;
            default:
                getLog().info(MessageFormat.format(messages.getString("info.loose.application.not.supported"),
                        proj.getPackaging()));
                installApp(proj.getArtifact());
                break;
        }
    }

    private static LooseConfigData createLooseConfigData() throws MojoExecutionException {
        try {
            return new LooseConfigData();
        } catch (ParserConfigurationException parserConfigurationException) {
            throw new MojoExecutionException("unable to create new LooseConfigData", parserConfigurationException);
        }
    }

    private void installAndVerifyApp(LooseConfigData config, File looseConfigFile, String applicationName) throws IOException, MojoExecutionException {
        deleteApplication(new File(serverDirectory, "apps"), looseConfigFile);
        deleteApplication(new File(serverDirectory, "dropins"), looseConfigFile);

        try {
            config.toXmlFile(looseConfigFile);
        } catch (Exception xmlException) {
            throw new MojoExecutionException("error writing xml file " + looseConfigFile + " from config " + config, xmlException);
        }

        //Only checks if server is running
        verifyAppStarted(applicationName);
    }

    private void cleanupPreviousExecution() {
        if (ApplicationXmlDocument.getApplicationXmlFile(serverDirectory).exists()) {
            ApplicationXmlDocument.getApplicationXmlFile(serverDirectory).delete();
            ServerConfigDocument.markInstanceStale();
        }
    }

    private boolean mavenWarPluginExists(MavenProject proj) {
        MavenProject currentProject = proj;
        while(currentProject != null) {
            List<Object> plugins = new ArrayList<Object>(currentProject.getBuildPlugins());
            plugins.addAll(currentProject.getPluginManagement().getPlugins());
            for(Object o : plugins) {
                if(o instanceof Plugin) {
                    Plugin plugin = (Plugin) o;
                    if(plugin.getGroupId().equals("org.apache.maven.plugins") && plugin.getArtifactId().equals("maven-war-plugin")) {
                        return true;
                    }
                }
            }
            currentProject = currentProject.getParent();
        }
        return false;
    }
   
}
