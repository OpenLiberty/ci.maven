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
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.tools.ant.taskdefs.Copy;
import org.codehaus.mojo.pluginsupport.util.ArtifactItem;

import net.wasdev.wlp.maven.plugins.BasicSupport;

/**
 * Copy applications to the specified directory of the Liberty server.
 * 
 * @goal install-apps
 * 
 * @requiresDependencyResolution compile
 */
public class InstallAppsMojo extends BasicSupport {

    /**
     * Application directory. 
     * 
     * @parameter property="appsDirectory" default-value="dropins"
     */
    protected String appsDirectory = null;
    
    /**
     * Strip version. 
     * 
     * @parameter property="stripVersion" default-value="false"
     */
    protected boolean stripVersion;
    
    protected void doExecute() throws Exception {
        if (skip) {
            return;
        }
        checkServerHomeExists();
        checkServerDirectoryExists();

        if (!"liberty-assembly".equals(project.getPackaging())) {
            installApp(project.getArtifact());
        }
        
        for (Artifact dep : (Set<Artifact>) project.getDependencyArtifacts()) {
            // skip assemblyArtifact if specified as a dependency
            if (assemblyArtifact != null && matches(dep, assemblyArtifact)) {
                continue;
            }
            if (dep.getScope().equals("compile")) {
                installApp(dep);
            }
        }

    }
    
    private void installApp(Artifact artifact) throws Exception {
        File destDir = new File(serverDirectory, appsDirectory);
        log.info(MessageFormat.format(messages.getString("info.install.app"), artifact.getFile().getCanonicalPath()));

        Copy copyFile = (Copy) ant.createTask("copy");
        copyFile.setFile(artifact.getFile());
        if (stripVersion) {
            copyFile.setTofile(new File(destDir, artifact.getArtifactId() + "." + artifact.getType()));
        } else {
            copyFile.setTodir(destDir);
        }
        copyFile.execute();
    }

    private boolean matches(Artifact dep, ArtifactItem assemblyArtifact) {
        return dep.getGroupId().equals(assemblyArtifact.getGroupId())
                && dep.getArtifactId().equals(assemblyArtifact.getArtifactId())
                && dep.getType().equals(assemblyArtifact.getType());
    }
}
