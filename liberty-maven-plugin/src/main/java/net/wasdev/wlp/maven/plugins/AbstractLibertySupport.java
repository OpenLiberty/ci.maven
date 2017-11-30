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

import java.io.File;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.settings.Settings;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.mojo.pluginsupport.MojoSupport;
import org.codehaus.mojo.pluginsupport.ant.AntHelper;
import org.codehaus.mojo.pluginsupport.util.ArtifactItem;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;

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
    private RepositorySystem repositorySystem;

    @Parameter( defaultValue = "${repositorySystemSession}", readonly = true, required = true )
    private RepositorySystemSession repoSession;

    @Parameter( defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true )
    private List<RemoteRepository> repositories;
    
    @Component
    protected ProjectBuilder mavenProjectBuilder;
    
    @Parameter(defaultValue = "${session}", readonly = true)
    protected MavenSession session;
    
    @Parameter(property = "reactorProjects", required = true, readonly = true)
    protected List<MavenProject> reactorProjects;
    
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
    
    protected boolean isReactorMavenProject(Artifact artifact) {
        for (MavenProject p : reactorProjects) {
            if (p.getGroupId().equals(artifact.getGroupId()) && p.getArtifactId().equals(artifact.getArtifactId())
                    && p.getVersion().equals(artifact.getVersion())) {
                return true;
            }
        }
        return false;
    }
    
    protected MavenProject getReactorMavenProject(Artifact artifact) {
        for (MavenProject p : reactorProjects) {
            // Support loose configuration to all sub-module projects in the reactorProjects object. 
            // Need to be able to retrieve all transitive dependencies in these projects.
            if (p.getGroupId().equals(artifact.getGroupId()) && p.getArtifactId().equals(artifact.getArtifactId())
                    && p.getVersion().equals(artifact.getVersion())) {
                p.setArtifactFilter(new ArtifactFilter() {
                    @Override
                    public boolean include(Artifact artifact) {
                        if ("compile".equals(artifact.getScope()) || "runtime".equals(artifact.getScope())) {
                            return true;
                        }
                        return false;
                    }
                });
                return p;
            }
        }
        
        return null;
    }
    
    //
    // Override methods in org.codehaus.mojo.pluginsupport.MojoSupport to resolve/create Artifact 
    // from ArtifactItem with Maven3 APIs.
    //
    
    /**
     * Resolves the Artifact from the remote repository if necessary. If no version is specified, it will
     * be retrieved from the dependency list or from the DependencyManagement section of the pom.
     *
     *
     * @param item  The item to create an artifact for; must not be null
     * @return      The artifact for the given item
     *
     * @throws MojoExecutionException   Failed to create artifact
     */
    @Override
    protected Artifact getArtifact(final ArtifactItem item) throws MojoExecutionException {
        assert item != null;
        
        // resolve version in case it is a range
        if (item.getVersion() != null) {
            try {
                resolveVersionRange(item);
            } catch (VersionRangeResolutionException e) {
                throw new MojoExecutionException(e.getLocalizedMessage(), e);
            }
        }
        
        // Return the artifact from the project dependency if it is available and the mojo 
        // should have requiresDependencyResolution=ResolutionScope.COMPILE_PLUS_RUNTIME set 
        Artifact artifact = resolveFromProjectDependencies(item);
        
        if (artifact != null) {
            if (item.getVersion() == null) {
                item.setVersion(artifact.getVersion());
            }
            // To maintain existing behavior, when the version is set in ArtifactItem which is
            // different from dependencies configuration, the version in ArtifactItem takes 
            // precedence.  
            if (artifact.isResolved() && artifact.getVersion().equals(item.getVersion())) {
                return artifact;
            } else {
                // create artifact from item
                return createArtifact(item);
            }
        } else {
            // if item has no version set, try to get it from the project dependencyManagement section
            if (item.getVersion() == null && resolveFromProjectDepMgmt(item) != null) {
                // get version from dependencyManagement
                item.setVersion(resolveFromProjectDepMgmt(item).getVersion());
            }
            // create artifact from item
            return createArtifact(item);
        }
    }

    /**
     * Create a new artifact.
     *
     * @param item  The item to create an artifact for
     * @return      A resolved artifact for the given item.
     *
     * @throws MojoExecutionException   Failed to create artifact
     */
    @Override
    protected Artifact createArtifact(final ArtifactItem item) throws MojoExecutionException {
        assert item != null;
        
        if (item.getVersion() == null) {
            throw new MojoExecutionException("Unable to find artifact version of " + item.getGroupId()
                + ":" + item.getArtifactId() + " in either project dependencies or in project dependencyManagement.");
        }
        
        // if version is a range get the highest available version
        if (item.getVersion().trim().startsWith("[") || item.getVersion().trim().startsWith("(") ) {
            try {
                item.setVersion(resolveVersionRange(item.getGroupId(), item.getArtifactId(), item.getType(), item.getVersion()));
            } catch (VersionRangeResolutionException e) {
                throw new MojoExecutionException("Could not get the highest version from the range: " + item.getVersion(), e);
            }
        }
        
        return resolveArtifactItem(item);
    }
    
    private Artifact resolveFromProjectDependencies(ArtifactItem item) {
        Set<Artifact> actifacts = getProject().getArtifacts();
        
        for (Artifact artifact : actifacts) {
            if (artifact.getGroupId().equals(item.getGroupId()) && artifact.getArtifactId().equals(item.getArtifactId())
                    && artifact.getType().equals(item.getType())) {
                log.debug("Found ArtifactItem from project dependencies: " + artifact.getGroupId() + ":"
                        + artifact.getArtifactId() + ":" + artifact.getVersion());
                // if (!artifact.getVersion().equals(item.getVersion())) {
                // item.setVersion(artifact.getVersion());
                // }
                return artifact;
            }
        }
        
        log.debug(item.getGroupId() + ":" + item.getArtifactId() + ":" + item.getVersion()
                + " is not found from project dependencies.");
        return null;
    }
    
    private Dependency resolveFromProjectDepMgmt(ArtifactItem item) {
        List<Dependency> list = getProject().getDependencyManagement().getDependencies();
        
        for (Dependency dependency : list) {
            if (dependency.getGroupId().equals(item.getGroupId())
                    && dependency.getArtifactId().equals(item.getArtifactId())
                    && dependency.getType().equals(item.getType())) {
                log.debug("Found ArtifactItem from project dependencyManagement " + dependency.getGroupId() + ":"
                        + dependency.getArtifactId() + ":" + dependency.getVersion());
                return dependency;
            }
        }
        
        log.debug(item.getGroupId() + ":" + item.getArtifactId() + ":" + item.getVersion()
                + " is not found from project dependencyManagement.");
        return null;
    }
    
    private Artifact resolveArtifactItem(final ArtifactItem item) throws MojoExecutionException {
        org.eclipse.aether.artifact.Artifact aetherArtifact = new org.eclipse.aether.artifact.DefaultArtifact(
                item.getGroupId(), item.getArtifactId(), item.getType(), item.getVersion());
        
        File artifactFile = resolveArtifactFile(aetherArtifact);
        
        Artifact artifact = new DefaultArtifact(item.getGroupId(), item.getArtifactId(), item.getVersion(),
                Artifact.SCOPE_PROVIDED, item.getType(), null, new DefaultArtifactHandler("jar"));
        
        if (artifactFile != null && artifactFile.exists()) {
            artifact.setFile(artifactFile);
            artifact.setResolved(true);
            log.debug(item.getGroupId() + ":" + item.getArtifactId() + ":" + item.getVersion()
                    + " is resolved from project repositories.");
        } else {
            getLog().warn("Artifact " + item.getGroupId() + ":" + item.getArtifactId() + ":" + item.getVersion()
                    + " has no attached file.");
            artifact.setResolved(false);
        }
        return artifact;
    }
    
    private File resolveArtifactFile(org.eclipse.aether.artifact.Artifact aetherArtifact) throws MojoExecutionException {
        ArtifactRequest req = new ArtifactRequest().setRepositories(this.repositories).setArtifact(aetherArtifact);
        ArtifactResult resolutionResult = null;
        
        try {
            resolutionResult = this.repositorySystem.resolveArtifact(this.repoSession, req);
            if (!resolutionResult.isResolved()) {
                throw new MojoExecutionException("Unable to resolve artifact: " + aetherArtifact.getGroupId() + ":"
                        + aetherArtifact.getArtifactId() + ":" + aetherArtifact.getVersion());
            }
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException("Unable to resolve artifact: " + aetherArtifact.getGroupId() + ":"
                    + aetherArtifact.getArtifactId() + ":" + aetherArtifact.getVersion(), e);
        }
        
        File artifactFile = resolutionResult.getArtifact().getFile();
        
        return artifactFile;
    }
    
    private String resolveVersionRange(ArtifactItem item) throws VersionRangeResolutionException {
        return resolveVersionRange(item.getGroupId(), item.getArtifactId(), item.getType(), item.getVersion());
    }
    
    private String resolveVersionRange(String groupId, String artifactId, String extension, String version)
            throws VersionRangeResolutionException {
        org.eclipse.aether.artifact.Artifact aetherArtifact = new org.eclipse.aether.artifact.DefaultArtifact(groupId,
                artifactId, extension, version);
        
        VersionRangeRequest rangeRequest = new VersionRangeRequest();
        rangeRequest.setArtifact(aetherArtifact);
        rangeRequest.setRepositories(repositories);
        
        VersionRangeResult rangeResult = this.repositorySystem.resolveVersionRange(this.repoSession, rangeRequest);
        
        if (rangeResult == null || rangeResult.getHighestVersion() == null) {
            throw new VersionRangeResolutionException(rangeResult, "Unable to resolve version range fram " + groupId
                    + ":" + artifactId + ":" + extension + ":" + version);
        }
        getLog().debug("Available versions: " + rangeResult.getVersions());
        return rangeResult.getHighestVersion().toString();
    }
}
