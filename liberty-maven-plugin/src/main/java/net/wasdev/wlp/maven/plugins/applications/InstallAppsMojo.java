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
package net.wasdev.wlp.maven.plugins.applications;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.mojo.pluginsupport.util.ArtifactItem;

import net.wasdev.wlp.maven.plugins.ApplicationXmlDocument;

/**
 * Copy applications to the specified directory of the Liberty server.
 */
@Mojo(name = "install-apps", requiresDependencyResolution=ResolutionScope.COMPILE)
public class InstallAppsMojo extends InstallAppMojoSupport {
    
    protected void doExecute() throws Exception {
        if (skip) {
            return;
        }
        checkServerHomeExists();
        checkServerDirectoryExists();
        
        // update target server configuration
        copyConfigFiles();
        exportParametersToXml();
        
        boolean installDependencies = false;
        boolean installProject = false;
        
        switch (getInstallAppPackages()) {
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
            log.warn(messages.getString("warn.install.app.add.configuration"));
            applicationXml.writeApplicationXmlDocument(serverDirectory);
        } else {
            if (ApplicationXmlDocument.getApplicationXmlFile(serverDirectory).exists()) {
                ApplicationXmlDocument.getApplicationXmlFile(serverDirectory).delete();
            }
        }
    }
    
    private void installDependencies() throws Exception {
        List<Dependency> deps = project.getCompileDependencies();
        
        for (Dependency dep : deps) {
            // skip if not an application type supported by Liberty
            if (!isSupportedType(dep.getType())) {
                continue;
            }
            // skip assemblyArtifact if specified as a dependency
            if (assemblyArtifact != null && matches(dep, assemblyArtifact)) {
                continue;
            }
            if (dep.getScope().equals("compile")) {
                MavenProject dependProj = getMavenProject(dep.getGroupId(), dep.getArtifactId(), dep.getVersion());
                if (isSupportedType(dependProj.getPackaging())) {
                    if (looseApplication && dependProj.getBasedir() != null && dependProj.getBasedir().exists()) {
                        installLooseApplication(dependProj);
                    } else {
                        installApp(resolveArtifact(dependProj.getArtifact()));
                    }
                } else {
                    log.warn(MessageFormat.format(messages.getString("error.application.not.supported"),
                            project.getId()));
                }
            }
        }
    }
    
    protected void installProject() throws Exception {
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

    private void installLooseApplication(MavenProject proj) throws Exception {
        String looseConfigFileName = getLooseConfigFileName(proj);
        String application = looseConfigFileName.substring(0, looseConfigFileName.length() - 4);
        File destDir = new File(serverDirectory, getAppsDirectory());
        File looseConfigFile = new File(destDir, looseConfigFileName);
        LooseConfigData config = new LooseConfigData();
        switch (proj.getPackaging()) {
            case "war":
                validateAppConfig(application, proj.getArtifactId());
                log.info(MessageFormat.format(messages.getString("info.install.app"), looseConfigFileName));
                installLooseConfigWar(proj, config);
                deleteApplication(new File(serverDirectory, "apps"), looseConfigFile);
                deleteApplication(new File(serverDirectory, "dropins"), looseConfigFile);
                config.toXmlFile(looseConfigFile);
                break;
            case "ear":
                validateAppConfig(application, proj.getArtifactId());
                log.info(MessageFormat.format(messages.getString("info.install.app"), looseConfigFileName));
                installLooseConfigEar(proj, config);
                deleteApplication(new File(serverDirectory, "apps"), looseConfigFile);
                deleteApplication(new File(serverDirectory, "dropins"), looseConfigFile);
                config.toXmlFile(looseConfigFile);
                break;
            case "liberty-assembly":
                if (mavenWarPluginExists(proj) || new File(proj.getBasedir(), "src/main/webapp").exists()) {
                    validateAppConfig(application, proj.getArtifactId());
                    log.info(MessageFormat.format(messages.getString("info.install.app"), looseConfigFileName));
                    installLooseConfigWar(proj, config);
                    deleteApplication(new File(serverDirectory, "apps"), looseConfigFile);
                    deleteApplication(new File(serverDirectory, "dropins"), looseConfigFile);
                    config.toXmlFile(looseConfigFile);
                } else {
                    log.debug("The liberty-assembly project does not contain the maven-war-plugin or src/main/webapp does not exist.");
                }
                break;
            default:
                log.info(MessageFormat.format(messages.getString("info.loose.application.not.supported"),
                        proj.getPackaging()));
                installApp(proj.getArtifact());
                break;
        }
    }
    
    @SuppressWarnings("unchecked")
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
    
    private boolean matches(Dependency dep, ArtifactItem assemblyArtifact) {
        return dep.getGroupId().equals(assemblyArtifact.getGroupId())
                && dep.getArtifactId().equals(assemblyArtifact.getArtifactId())
                && dep.getType().equals(assemblyArtifact.getType());
    }
    
    private boolean isSupportedType(String type) {
        boolean supported = false;
        switch (type) {
            case "ear":
            case "war":
            case "rar":
            case "eba":
            case "esa":
            case "liberty-assembly":
                supported = true;
                break;
            default:
                break;
        }
        return supported;
    }
   
}
