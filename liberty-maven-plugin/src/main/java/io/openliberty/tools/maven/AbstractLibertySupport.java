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
package io.openliberty.tools.maven;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.settings.Settings;
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
    protected RepositorySystem repositorySystem;

    @Parameter( defaultValue = "${repositorySystemSession}", readonly = true, required = true )
    protected RepositorySystemSession repoSession;

    @Parameter( defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true )
    protected List<RemoteRepository> repositories;
    
    @Component
    protected ProjectBuilder mavenProjectBuilder;
    
    @Parameter(defaultValue = "${session}", readonly = true)
    protected MavenSession session;
    
    @Parameter(property = "reactorProjects", required = true, readonly = true)
    protected List<MavenProject> reactorProjects;
    
    @Parameter(defaultValue = "${maven.multiModuleProjectDirectory}", required = false, readonly = true)
    protected File multiModuleProjectDirectory = null;

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
                    && p.getVersion().equals(artifact.getBaseVersion())) {
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
                    && p.getVersion().equals(artifact.getBaseVersion())) {
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
        Artifact artifact = null;
        
        if (item.getVersion() != null) {
            // if version is set in ArtifactItem, it will always override the one in project dependency
            artifact = createArtifact(item);
        } else {
            // Return the artifact from the project dependency if it is available and the mojo
            // should have requiresDependencyResolution=ResolutionScope.COMPILE_PLUS_RUNTIME set
            artifact = resolveFromProjectDependencies(item);
            
            if (artifact != null) {
                // in case it is not resolved yet
                if (!artifact.isResolved()) {
                    item.setVersion(artifact.getVersion());
                    artifact = createArtifact(item);
                }
            } else if (resolveFromProjectDepMgmt(item) != null) {
                // if item has no version set, try to get it from the project dependencyManagement section
                // get version from dependencyManagement
                item.setVersion(resolveFromProjectDepMgmt(item).getVersion());
                artifact = createArtifact(item);
            } else {
                    throw new MojoExecutionException(
                            "Unable to find artifact version of " + item.getGroupId() + ":" + item.getArtifactId()
                                    + " in either project dependencies or in project dependencyManagement.");
            }
        }
        
        return artifact;
    }
    
    /**
     * Equivalent to {@link #getArtifact(ArtifactItem)} with an ArtifactItem
     * defined by the given the coordinates.
     * 
     * @param groupId
     *            The group ID
     * @param artifactId
     *            The artifact ID
     * @param type
     *            The type (e.g. jar)
     * @param version
     *            The version, or null to retrieve it from the dependency list
     *            or from the DependencyManagement section of the pom.
     * @return Artifact The artifact for the given item
     * @throws MojoExecutionException
     *             Failed to create artifact
     */
    protected Artifact getArtifact(String groupId, String artifactId, String type, String version) throws MojoExecutionException {
        ArtifactItem item = new ArtifactItem();
        item.setGroupId(groupId);
        item.setArtifactId(artifactId);
        item.setType(type);
        item.setVersion(version);

        return getArtifact(item);
    }

    protected ArtifactItem createArtifactItem(String groupId, String artifactId, String type, String version) {
        ArtifactItem item = new ArtifactItem();
        item.setGroupId(groupId);
        item.setArtifactId(artifactId);
        item.setType(type);
        item.setVersion(version);

        return item;
    }

    /**
     * Find resolved dependencies with matching groupId:artifactId:version. Also collect transitive dependencies for those
     * resolved dependencies. The groupId is required. The artifactId and version are optional. 
     * The artifactId can also end with a '*' to indicate a wildcard match.
     *
     * @param groupId String specifying the groupId of the Maven artifact to copy.
     * @param artifactId String specifying the artifactId of the Maven artifact to copy.
     * @param version String specifying the version of the Maven artifact to copy.
     * @param type String specifying the type of the Maven artifact to copy.
     *
     * @return Set<Artifact> A collection of Artifact objects for the resolved dependencies and transitive dependencies
     * @throws MojoExecutionException
     */
    protected Set<Artifact> getResolvedDependencyWithTransitiveDependencies(String groupId, String artifactId, String version, String type) throws MojoExecutionException {
        Set<Artifact> resolvedDependencies = new HashSet<Artifact> ();

        if (version != null) {
            // if version is set, it will always override the one in project dependency
            Artifact artifact = getArtifact(groupId, artifactId, type, version);
            if (artifact != null) {
                resolvedDependencies.add(artifact);
                findTransitiveDependencies(artifact, getProject().getArtifacts(), resolvedDependencies);
            } else {
                log.warn("Unable to find artifact matching groupId "+ groupId +", artifactId "+artifactId+", version "+version+", and type "+type+" in configured repositories.");
            }
        } else {
            Set<Artifact> artifacts = getProject().getArtifacts();
            boolean isWildcard = artifactId != null && artifactId.endsWith("*") ? true : false;
            String compareArtifactId = artifactId;

            if (isWildcard) {
                // if the artifactId is "*", just match on groupId
                if (artifactId.length() == 1) {
                    compareArtifactId = null;
                    isWildcard = false;
                } else {
                    compareArtifactId = artifactId.substring(0,artifactId.length() -1);
                }
            }
        
            for (Artifact artifact : artifacts) {
                if (artifact.getGroupId().equals(groupId) && 
                    ((compareArtifactId == null) ||
                     (isWildcard && artifact.getArtifactId().startsWith(compareArtifactId)) ||
                     (artifact.getArtifactId().equals(compareArtifactId)))) {
                    if (!artifact.isResolved()) {
                        ArtifactItem item = createArtifactItem(artifact.getGroupId(), artifact.getArtifactId(), artifact.getType(), artifact.getVersion()); 
                        artifact = getArtifact(item);
                    }
                    log.debug("Found resolved dependency from project dependencies: " + artifact.getGroupId() + ":"
                            + artifact.getArtifactId() + ":" + artifact.getVersion());
                    resolvedDependencies.add(artifact);
                    findTransitiveDependencies(artifact, getProject().getArtifacts(), resolvedDependencies);
                }
            }

            if (resolvedDependencies.isEmpty() && getProject().getDependencyManagement() != null) {
                // if project has dependencyManagement section
                List<Dependency> list = getProject().getDependencyManagement().getDependencies();
            
                for (Dependency dependency : list) {
                    if (dependency.getGroupId().equals(groupId) && 
                        ((compareArtifactId == null) ||
                         (isWildcard && dependency.getArtifactId().startsWith(compareArtifactId)) ||
                         (dependency.getArtifactId().equals(compareArtifactId)))) {
                        ArtifactItem item = createArtifactItem(dependency.getGroupId(), dependency.getArtifactId(), dependency.getType(), dependency.getVersion()); 
                        Artifact artifact = getArtifact(item);
                        log.debug("Found resolved dependency from project dependencyManagement " + dependency.getGroupId() + ":"
                            + dependency.getArtifactId() + ":" + dependency.getVersion());
                        resolvedDependencies.add(artifact);
                        findTransitiveDependencies(artifact, getProject().getArtifacts(), resolvedDependencies);
                    }
                }
            }

            if (resolvedDependencies.isEmpty()) {
                // No matching artifacts were found in the resolved dependencies. Send warning.
                log.warn("Unable to find artifact matching groupId "+ groupId +", artifactId "+artifactId+", and version "+version+" in either project dependencies or in project dependencyManagement.");
            }
        }

        return resolvedDependencies;
     }

     protected void findTransitiveDependencies(Artifact resolvedArtifact, Set<Artifact> resolvedArtifacts, Set<Artifact> resolvedDependencies) {
        boolean isProvidedScopeAllowed = resolvedArtifact.getScope().equals(Artifact.SCOPE_PROVIDED);
        String coords = resolvedArtifact.getGroupId() + ":" + resolvedArtifact.getArtifactId() + ":";
        for (Artifact artifact : resolvedArtifacts) {
            // Do not copy transitive dependencies with SCOPE_PROVIDED unless the resolvedArtifact is SCOPE_PROVIDED.
            boolean isProvidedScope = artifact.getScope().equals(Artifact.SCOPE_PROVIDED);
            if (!artifact.equals(resolvedArtifact) && (!isProvidedScope || isProvidedScopeAllowed)) {
                List<String> depTrail = artifact.getDependencyTrail();
                if (dependencyTrailContainsArtifact(coords, resolvedArtifact.getVersion(), depTrail)) {
                    log.info("Adding transitive dependency with scope: "+artifact.getScope()+" and GAV: "+artifact.getGroupId()+":"+artifact.getArtifactId()+":"+artifact.getVersion());
                    resolvedDependencies.add(artifact);
                }
            }
        }
     }

     protected boolean dependencyTrailContainsArtifact(String gaCoords, String version, List<String> depTrail) {
         for (String nextFullArtifactId : depTrail) {
             if (nextFullArtifactId.startsWith(gaCoords) && 
                 ((version == null) || (version != null && nextFullArtifactId.endsWith(":"+version))) ) {
                 return true;
             }
         }
         return false;
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
            throw new MojoExecutionException("Unable to find artifact without version specified: " + item.getGroupId()
                + ":" + item.getArtifactId() + ":" + item.getVersion() + " in either project dependencies or in project dependencyManagement.");
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
            if (artifact.getGroupId().equals(item.getGroupId()) && 
                artifact.getArtifactId().equals(item.getArtifactId()) && 
                artifact.getType().equals(item.getType())) {
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
        // if project has dependencyManagement section
        if (getProject().getDependencyManagement() != null) {
            List<Dependency> list = getProject().getDependencyManagement().getDependencies();
            
            for (Dependency dependency : list) {
                if (dependency.getGroupId().equals(item.getGroupId()) && 
                    dependency.getArtifactId().equals(item.getArtifactId()) && 
                    dependency.getType().equals(item.getType())) {
                    log.debug("Found ArtifactItem from project dependencyManagement " + dependency.getGroupId() + ":"
                            + dependency.getArtifactId() + ":" + dependency.getVersion());
                    return dependency;
                }
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
