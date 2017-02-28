/**
 * (C) Copyright IBM Corporation 2016, 2017.
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
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.tools.ant.taskdefs.Copy;

import net.wasdev.wlp.maven.plugins.BasicSupport;

/**
 * Install artifact into Liberty server support.
 */
public class InstallAppMojoSupport extends BasicSupport {

    /**
     * Application directory. 
     * 
     * @Parameter( property="appsDirectory", defaultValue="dropins" )
     */
    @Parameter( property="appsDirectory", defaultValue="dropins" )
    protected String appsDirectory = null;
    
    /**
     * Strip version. 
     * 
     * @Parameter( property="stripVersion", defaultValue="false" )
     */
    @Parameter( property="stripVersion", defaultValue="false" )
    protected boolean stripVersion;
    
    protected void installApp(Artifact artifact) throws Exception {
        
        if (artifact.getFile() == null) {
            throw new MojoExecutionException(messages.getString("error.install.app.missing"));
        }
        
        File destDir = new File(serverDirectory, appsDirectory);
        log.info(MessageFormat.format(messages.getString("info.install.app"), artifact.getFile().getCanonicalPath()));
        
        Copy copyFile = (Copy) ant.createTask("copy");
        copyFile.setFile(artifact.getFile());
        if (stripVersion) {
            String extension = null;
            String path = artifact.getFile().getCanonicalPath();
            if (path.lastIndexOf('.')>0) {
                extension = path.substring(path.lastIndexOf('.')+1);
            } else {
                extension = artifact.getType();
            }
            copyFile.setTofile(new File(destDir, artifact.getArtifactId() + "." + extension));        	
        } else {
            copyFile.setTodir(destDir);
        }
        copyFile.execute();
    }

}
