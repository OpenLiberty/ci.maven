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
import java.util.Set;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import io.openliberty.tools.ant.ServerTask;
import io.openliberty.tools.common.plugins.config.ServerConfigDocument;
import io.openliberty.tools.maven.utils.CommonLogger;

/**
 * Undeploy application from liberty server. If no parameters have been defined
 * the mojo will undeploy all applications from the server.
 */
@Mojo(name = "undeploy", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)

public class UndeployAppMojo extends DeployMojoSupport {

    private static final String STOP_APP_MESSAGE_CODE_REG = "CWWKZ0009I.*";
    private static final long APP_STOP_TIMEOUT_DEFAULT = 30 * 1000;

    private ServerConfigDocument scd;
    
    /*
     * (non-Javadoc)
     * @see org.codehaus.mojo.pluginsupport.MojoSupport#doExecute()
     */
    @Override
    protected void doExecute() throws Exception {
        if (skip) {
            getLog().info("\nSkipping undeploy goal.\n");
            return;
        }
        
        checkServerHomeExists();
        checkServerDirectoryExists();

        boolean uninstallDependencies = false;
        boolean uninstallProject = false;

        switch (getDeployPackages()) {
            case "all":
                uninstallDependencies = true;
                uninstallProject = true;
                break;
            case "dependencies":
                uninstallDependencies = true;
                break;
            case "project":
                uninstallProject = true;
                break; 
            default:
                return;
        }

        if (uninstallDependencies) {
            undeployDependencies();
        }
        if (uninstallProject) {
            undeployProject();
        }
    }
    
    private void undeployDependencies() throws MojoExecutionException {
        Set<Artifact> artifacts = project.getArtifacts();

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
                    if (looseApplication && isReactorMavenProject(artifact)) {
                        MavenProject dependProj = getReactorMavenProject(artifact);
                        undeployApp(new File(new File(serverDirectory, getAppsDirectory()), getLooseConfigFileName(dependProj)));
                    } else {
                        Artifact depArtifact = resolveArtifact(artifact);
                        File depArchive = depArtifact.getFile();
                        if (stripVersion) {
                            depArchive = new File(stripVersionFromName(depArtifact.getFile().getName(), depArtifact.getBaseVersion()));
                        }
                        File installDir = new File(serverDirectory, getAppsDirectory());
                        undeployApp(new File(installDir, depArchive.getName()));
                    }
                } else {
                    log.warn(MessageFormat.format(messages.getString("error.application.not.supported"),
                            project.getId()));
                }
            }
        }
    }

    private void undeployProject() throws MojoExecutionException {
        File installDir = new File(serverDirectory, getAppsDirectory());
        if (looseApplication) {
            undeployApp(new File(installDir, getLooseConfigFileName(project)));
        } else {
            undeployApp(new File(installDir, getAppFileName(project)));
        }
    }

    protected void undeployApp(File file) throws MojoExecutionException {
        String appName = file.getName().substring(0, file.getName().lastIndexOf('.'));

        if (getAppsDirectory().equals("apps")) {
            scd = null;

            try {
                File serverXML = new File(serverDirectory.getCanonicalPath(), "server.xml");
            
                scd = ServerConfigDocument.getInstance(CommonLogger.getInstance(), serverXML, configDirectory,
                bootstrapPropertiesFile, bootstrapProperties, serverEnvFile, false);

                //appName will be set to a name derived from file if no name can be found.
                appName = scd.findNameForLocation(appName);
            } catch (Exception e) {
                log.warn(e.getLocalizedMessage());
            } 
        }

        try {
            if (!file.delete()) {
                throw new MojoExecutionException(file.toString() + " could not be deleted from the server during undeploy.");
            }    
        } catch (SecurityException se) {
            throw new MojoExecutionException(file.toString() + " could not be deleted because access was denied.", se);
        }

        //check stop message code
        String stopMessage = STOP_APP_MESSAGE_CODE_REG + appName;
        ServerTask serverTask = initializeJava();
        if (serverTask.waitForStringInLog(stopMessage, APP_STOP_TIMEOUT_DEFAULT, new File(serverDirectory, "logs/messages.log")) == null) {
            throw new MojoExecutionException("CWWKM2022E: Failed to undeploy application " + file.getPath() + ". The Stop application message cannot be found in console.log.");
        }
    }
}
