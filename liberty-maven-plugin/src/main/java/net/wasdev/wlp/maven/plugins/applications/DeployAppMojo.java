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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.mojo.pluginsupport.util.ArtifactItem;

import net.wasdev.wlp.ant.DeployTask;
import net.wasdev.wlp.maven.plugins.BasicSupport;

/**
 * Deploy application to liberty server
 */
@Mojo(name = "deploy", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class DeployAppMojo extends BasicSupport {
    /**
     * A file which points to a specific module's war | ear | eba | zip archive location
     */
    @Parameter(property = "appArchive")
    protected File appArchive;

    /**
     * Maven coordinates of an application to deploy. This is best listed as a dependency,
     * in which case the version can be omitted.
     */
    @Parameter
    protected ArtifactItem appArtifact;

    /**
     * Timeout to verify deploy successfully, in seconds.
     */
    @Parameter(property = "timeout", defaultValue = "40")
    protected int timeout = 40;
    
    /**
     *  The file name of the deployed application in the `dropins` directory.
     */
    @Parameter(property = "appDeployName")
    protected String appDeployName;

    @Override
    protected void doExecute() throws Exception {
        if (skip) {
            return;
        }
        checkServerHomeExists();
        checkServerDirectoryExists();

        if (appArchive != null && appArtifact != null) {
            throw new MojoExecutionException(messages.getString("error.app.set.twice"));
        }

        if (appArtifact != null) {
            Artifact artifact = getArtifact(appArtifact);
            appArchive = artifact.getFile();
            log.info(MessageFormat.format(messages.getString("info.variable.set"), "artifact based application", appArtifact));
        } else if (appArchive != null) {
            log.info(MessageFormat.format(messages.getString("info.variable.set"), "non-artifact based application", appArchive));
        } else {
            throw new MojoExecutionException("Nothing to deploy - appArchive or appArtifact must be set.");
        }

        if (!appArchive.exists() || appArchive.isDirectory()) {
            throw new MojoExecutionException("Application file does not exist or is a directory: " + appArchive);
        }

        log.info(MessageFormat.format(messages.getString("info.deploy.app"), appArchive.getCanonicalPath()));
        DeployTask deployTask = (DeployTask) ant.createTask("antlib:net/wasdev/wlp/ant:deploy");
        if (deployTask == null) {
            throw new IllegalStateException(MessageFormat.format(messages.getString("error.dependencies.not.found"), "deploy"));
        }

        deployTask.setInstallDir(installDirectory);
        deployTask.setServerName(serverName);
        deployTask.setUserDir(userDirectory);
        deployTask.setOutputDir(outputDirectory);
        deployTask.setFile(appArchive);
        deployTask.setDeployName(appDeployName);
        // Convert from seconds to milliseconds
        deployTask.setTimeout(Long.toString(timeout*1000));
        deployTask.execute();
    }
}
