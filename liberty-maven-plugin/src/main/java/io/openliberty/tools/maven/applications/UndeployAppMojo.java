/**
 * (C) Copyright IBM Corporation 2014, 2019.
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

import io.openliberty.tools.ant.UndeployTask;

/**
 * Undeploy application from liberty server. If no parameters have been defined
 * the mojo will undeploy all applications from the server.
 */
@Mojo(name = "undeploy", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)

public class UndeployAppMojo extends DeployMojoSupport {

    private UndeployTask undeployTask;
    
    /*
     * (non-Javadoc)
     * @see org.codehaus.mojo.pluginsupport.MojoSupport#doExecute()
     */
    @Override
    protected void doExecute() throws Exception {
        if (skip) {
            return;
        }
        
        checkServerHomeExists();
        checkServerDirectoryExists();
        
        undeployTask = (UndeployTask) ant
                .createTask("antlib:io/openliberty/tools/ant:undeploy");
        
        if (undeployTask == null) {
            throw new IllegalStateException(MessageFormat.format(messages.getString("error.dependencies.not.found"), "undeploy"));
        }

        undeployTask.setInstallDir(installDirectory);
        undeployTask.setServerName(serverName);
        undeployTask.setUserDir(userDirectory);
        undeployTask.setOutputDir(outputDirectory);
        
        // Convert from seconds to milliseconds
        undeployTask.setTimeout(Long.toString(timeout * 1000));

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

    private void undeployApp(File appArchive) throws MojoExecutionException {
        try {
            log.info(MessageFormat.format(
                        messages.getString("info.undeploy.app"),
                        appArchive.getCanonicalPath()));
            undeployTask.setFile(appArchive.toString());
            undeployTask.execute();
        } catch (IOException ioe) {
            throw new MojoExecutionException(MessageFormat.format(
                messages.getString("error.undeploy.app.noexist"),
                appArchive.toString()));
        }
    }
    
    private void undeployDependencies() throws MojoExecutionException {
        Set<Artifact> artifacts = project.getArtifacts();

        System.out.println(project.getArtifacts().size());
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
                        undeployApp(depArchive);
                    }
                } else {
                    log.warn(MessageFormat.format(messages.getString("error.application.not.supported"),
                            project.getId()));
                }
            }
        }
    }

    private void undeployProject() throws MojoExecutionException {
        if (looseApplication) {
            undeployApp(new File(new File(serverDirectory, getAppsDirectory()), getLooseConfigFileName(project)));
        } else {
            undeployApp(project.getArtifact().getFile());
        }
    }
}
