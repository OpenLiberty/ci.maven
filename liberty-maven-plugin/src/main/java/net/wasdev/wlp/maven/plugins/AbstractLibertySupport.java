package net.wasdev.wlp.maven.plugins;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.artifact.repository.ArtifactRepository;

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
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project = null;

    /**
     * @parameter expression="${localRepository}"
     * @readonly
     * @required
     */
    protected ArtifactRepository artifactRepository = null;
    
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
