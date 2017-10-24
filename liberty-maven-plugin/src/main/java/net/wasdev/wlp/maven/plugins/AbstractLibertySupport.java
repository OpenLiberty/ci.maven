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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.*;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Settings;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.mojo.pluginsupport.MojoSupport;
import org.codehaus.mojo.pluginsupport.ant.AntHelper;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Liberty Abstract Mojo Support
 * 
 */
public abstract class AbstractLibertySupport extends MojoSupport {
    /**
     * Maven Project
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject project = null;
    
    @Parameter(defaultValue = "${localRepository}", required = true, readonly = true)
    protected ArtifactRepository artifactRepository = null;
    
    /**
     * The build settings.
     */
    @Parameter(defaultValue = "${settings}", required = true, readonly = true)
    protected Settings settings;
    
    @Component(role = AntHelper.class)
    protected AntHelper ant;
    
    @Component
    protected RepositorySystem repositorySystem;
    
    @Component
    protected ProjectBuilder mavenProjectBuilder;
    
    @Parameter(defaultValue = "${session}", readonly = true)
    protected MavenSession session;
    
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
    
    protected MavenProject getMavenProject(String groupId, String artifactId, String version)
            throws ProjectBuildingException {
        Artifact pomArtifact = repositorySystem.createProjectArtifact(groupId, artifactId, version);
        ProjectBuildingResult build = mavenProjectBuilder.build(pomArtifact, session.getProjectBuildingRequest());
        return build.getProject();
    }

    protected MavenProject getMavenProject(MavenProject proj, final Dependency dependency) {
        try {
            List<MavenProject> collectedProjects = proj.getParent().getCollectedProjects();
            return Iterables.find(collectedProjects, new Predicate<MavenProject>() {
                @Override
                public boolean apply(MavenProject mavenProject) {
                    return mavenProject.getArtifactId().equals(dependency.getArtifactId());
                }
            });
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    protected MavenProject getMavenProject(MavenProject proj, final Artifact artifact) {
        try {
            List<MavenProject> collectedProjects = proj.getParent().getCollectedProjects();
            return Iterables.find(collectedProjects, new Predicate<MavenProject>() {
                @Override
                public boolean apply(MavenProject mavenProject) {
                    return mavenProject.getArtifactId().equals(artifact.getArtifactId());
                }
            });
        } catch (NoSuchElementException e) {
            return null;
        }
    }

}
