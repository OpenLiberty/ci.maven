/**
 * (C) Copyright IBM Corporation 2014.
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
import java.io.IOException;
import java.text.MessageFormat;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.mojo.pluginsupport.util.ArtifactItem;

import net.wasdev.wlp.ant.UndeployTask;
import net.wasdev.wlp.maven.plugins.BasicSupport;

/**
 * Undeploy application from liberty server
 * 
 * @goal undeploy
 * 
 * 
 */
public class UndeployAppMojo extends BasicSupport {
    /**
     * A file name which points to a specific module's jar | war | ear | eba | zip
     * archive.
     * 
     * @parameter expression="${appArchive}"
     * @optional
     */
    protected String appArchive = null;

    /**
     * Maven coordinates of an application to undeploy. This is best listed as a dependency,
     * in which case the version can be omitted.
     * 
     * @parameter
     */
    protected ArtifactItem appArtifact;

    /**
     * Timeout to verify undeploy successfully, in seconds.
     * 
     * @parameter expression="${timeout}" default-value="40"
     */
    protected int timeout = 40;

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
            appArchive = artifact.getFile().getName();
            log.info(MessageFormat.format(messages.getString("info.variable.set"), "artifact based application", appArtifact));
        } else if (appArchive != null) {
            File file = new File(appArchive);
            if (file.exists()) {
                appArchive = file.getName();
            }
            log.info(MessageFormat.format(messages.getString("info.variable.set"), "non-artifact based application", appArchive));
        } else {
            throw new MojoExecutionException(MessageFormat.format(messages.getString("error.undeploy.app.set.validate"), ""));
        }

        File destFile = new File(serverDirectory, "dropins/" + appArchive);
        if (destFile == null || !destFile.exists() || destFile.isDirectory()) {
            throw new IOException(MessageFormat.format(messages.getString("error.undeploy.app.noexist"), destFile.getCanonicalPath()));
        }

        log.info(MessageFormat.format(messages.getString("info.undeploy.app"), destFile.getCanonicalPath()));
        UndeployTask undeployTask = (UndeployTask) ant.createTask("antlib:net/wasdev/wlp/ant:undeploy");
        if (undeployTask == null)
            throw new NullPointerException("server task not found");

        undeployTask.setInstallDir(installDirectory);
        undeployTask.setServerName(serverName);
        undeployTask.setUserDir(userDirectory);
        undeployTask.setOutputDir(outputDirectory);
        undeployTask.setFile(appArchive);
        // Convert from seconds to milliseconds
        undeployTask.setTimeout(Long.toString(timeout*1000));
        undeployTask.execute();

    }
}
