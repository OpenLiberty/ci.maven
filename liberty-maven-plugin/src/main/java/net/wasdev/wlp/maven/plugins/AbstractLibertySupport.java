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
package net.wasdev.wlp.maven.plugins;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.mojo.pluginsupport.MojoSupport;
import org.codehaus.mojo.pluginsupport.ant.AntHelper;

/**
 * Liberty Abstract Mojo Support
 * 
 */
public abstract class AbstractLibertySupport extends MojoSupport {
    /**
     * Maven Project
     * 
     * @parameter property="project"
     * @required
     * @readonly
     */
    protected MavenProject project = null;

    /**
     * @parameter property="localRepository"
     * @readonly
     * @required
     */
    protected ArtifactRepository artifactRepository = null;
    
    /**
    * The build settings.
    *
    * @parameter property="settings" 
    * @required
    * @readonly
    */
    protected Settings settings;

    /**
     * @component
     */
    protected AntHelper ant;

    protected MavenProject getProject() {
        return project;
    }

    protected ArtifactRepository getArtifactRepository() {
        return artifactRepository;
    }

    protected void init() throws MojoExecutionException, MojoFailureException {
        super.init();
        // Initialize ant helper instance
        ant.setProject(getProject());
    }

}
