/**
 * (C) Copyright IBM Corporation 2021, 2024.
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

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import io.openliberty.tools.common.plugins.util.ServerFeatureUtil;
import io.openliberty.tools.maven.utils.DevHelper;
import io.openliberty.tools.maven.utils.ExecuteMojoUtil;

public abstract class ServerFeatureSupport extends BasicSupport {
	
    private static final String LIBERTY_MAVEN_PLUGIN_GROUP_ID = "io.openliberty.tools";
    private static final String LIBERTY_MAVEN_PLUGIN_ARTIFACT_ID = "liberty-maven-plugin";

    private ServerFeatureUtil servUtil;
    
    /**
     * The current plugin's descriptor. This is auto-filled by Maven 3.
     */
    @Parameter( defaultValue = "${plugin}", readonly = true )
    private PluginDescriptor plugin;

    protected class ServerFeatureMojoUtil extends ServerFeatureUtil {

        @Override
        public void debug(String msg) {
            if (isDebugEnabled()) {
                getLog().debug(msg);
            }
        }

        @Override
        public void debug(String msg, Throwable e) {
            if (isDebugEnabled()) {
                getLog().debug(msg, e);
            }
        }

        @Override
        public void debug(Throwable e) {
            if (isDebugEnabled()) {
                getLog().debug(e);
            }
        }

        @Override
        public void warn(String msg) {
            if (!suppressLogs) {
                getLog().warn(msg);
            } else {
                debug(msg);
            }
        }

        @Override
        public void info(String msg) {
            if (!suppressLogs) {
                getLog().info(msg);
            } else {
                debug(msg);
            }
        }

        @Override
        public void error(String msg, Throwable e) {
            getLog().error(msg, e);
        }

        @Override
        public void error(String msg) {
            getLog().error(msg);
        }

        @Override
        public boolean isDebugEnabled() {
            return getLog().isDebugEnabled();
        }
    }

    private void createNewServerFeatureUtil() {
        servUtil = new ServerFeatureMojoUtil();
    }

    /**
     * Get a new instance of ServerFeatureUtil
     * 
     * @param suppressLogs if true info and warning will be logged as debug
     * @return instance of ServerFeatureUtil
     */
    protected ServerFeatureUtil getServerFeatureUtil(boolean suppressLogs, Map<String, File> libDirPropFiles) {
        if (servUtil == null) {
            createNewServerFeatureUtil();
            servUtil.setLibertyDirectoryPropertyFiles(libDirPropFiles);
        }
        if (suppressLogs) {
            servUtil.setSuppressLogs(true);
        } else {
            servUtil.setSuppressLogs(false);
        }
        return servUtil;
    }
    
    /**
     * Returns whether potentialTopModule is a multi module project that has
     * potentialSubModule as one of its sub-modules.
     */
    private static boolean isSubModule(MavenProject potentialTopModule, MavenProject potentialSubModule) {
        List<String> multiModules = potentialTopModule.getModules();
        if (multiModules != null) {
            for (String module : multiModules) {
                File subModuleDir = new File(potentialTopModule.getBasedir(), module);
                try {
                    if (subModuleDir.getCanonicalFile().equals(potentialSubModule.getBasedir().getCanonicalFile())) {
                        return true;
                    }
                } catch (IOException e) {
                    if (subModuleDir.getAbsoluteFile().equals(potentialSubModule.getBasedir().getAbsoluteFile())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Get the list of projects that are relevant when certain goals (e.g. "dev" and "run") are executed
     * against a multi-module project. Relevant projects are any projects in the same downstream/upstream flow.
     * Jar projects without any downstream modules are implicitly ignored and any projects with skip=true 
     * configured in the plugin configuration are also ignored. If multiple "streams" still exist, a MojoExecutionException
     * is thrown logging the conflict. 
     * 
     * 
     * @param graph
     * @return
     * @throws MojoExecutionException
     */
    protected List<MavenProject> getRelevantMultiModuleProjects(ProjectDependencyGraph graph, boolean skipJars) throws MojoExecutionException {
    	getLog().debug("Resolve relevant multi-module projects");
    	
        List<MavenProject> sortedReactorProjects = graph.getSortedProjects();
        Set<MavenProject> conflicts = new LinkedHashSet<MavenProject>(); // keeps the order of items added in

        // A leaf here is a module without any downstream modules depending on it
        List<MavenProject> leaves = new ArrayList<MavenProject>();
        for (MavenProject reactorProject : sortedReactorProjects) {
            if (graph.getDownstreamProjects(reactorProject, true).isEmpty()) {
            	getLog().debug("Found final downstream project: " + reactorProject.getArtifactId());
                
            	if (skipConfigured(reactorProject)) {
            		getLog().debug("Skip configured on project: " + reactorProject.getArtifactId() + " - Ignoring");
            	} else if (reactorProject.getPackaging().equals("jar") && skipJars) {
            		getLog().debug(reactorProject.getArtifactId() + " is a jar project - Ignoring");
            	} else {
                    leaves.add(reactorProject);
            	}
            }
        }
            
        // At this point, the only leaves we should have is one final downstream project and the parent pom project. 
        // Loop through and find any conflicts.
        for (MavenProject leaf1 : leaves) {
            for (MavenProject leaf2 : leaves) {
            	// Check that the leaves are not the same module and that one of the leaves is not the parent.
                if (leaf1 != leaf2 && !(isSubModule(leaf2, leaf1) || isSubModule(leaf1, leaf2)) ) {
                    conflicts.add(leaf1);
                    conflicts.add(leaf2);
                }
            }
        }
        
        if (conflicts.isEmpty() ) {
        	List<MavenProject> devModeProjects = new ArrayList<MavenProject>();
        	for (MavenProject leaf : leaves) {
        		devModeProjects.addAll(graph.getUpstreamProjects(leaf, true));
        		devModeProjects.add(leaf);
        	}
        	
        	getLog().debug("Resolved multi-module projects: ");
        	for (MavenProject project : devModeProjects) {
        		getLog().debug(project.getArtifactId());
        	}
        	return devModeProjects;
        } else {
        	
        	List<String> conflictModuleRelativeDirs = new ArrayList<String>();
            for (MavenProject conflict : conflicts) {
                // Make the module path relative to the multi-module project directory
                conflictModuleRelativeDirs.add(getModuleRelativePath(conflict));
            }
            
            throw new MojoExecutionException("Found multiple independent modules in the Reactor build order: "
                    + conflictModuleRelativeDirs
                    + ". Skip a conflicting module in the Liberty configuration or specify the module containing the Liberty configuration that you want to use for the server by including the following parameters in the Maven command: -pl <module-with-liberty-config> -am");
        }
    }
    
    private boolean skipConfigured(MavenProject project) {
    	
        // Properties that are set in the pom file
        Properties props = project.getProperties();

        // Properties that are set by user via CLI parameters
        Properties userProps = session.getUserProperties();
    	Plugin libertyPlugin = getLibertyPluginForProject(project);
        Xpp3Dom config = ExecuteMojoUtil.getPluginGoalConfig(libertyPlugin, "dev", getLog());
        
        return DevHelper.getBooleanFlag(config, userProps, props, "skip");
    }

    /**
     * If there was a previous module without downstream projects, assume Liberty
     * already ran. Then if THIS module is a top-level multi module pom that
     * includes that other module, skip THIS module.
     * 
     * @param graph The project dependency graph containing Reactor build order
     * @return Whether this module should be skipped
     */
    protected boolean containsPreviousLibertyModule(ProjectDependencyGraph graph) {
        List<MavenProject> sortedReactorProjects = graph.getSortedProjects();
        MavenProject mostDownstreamModule = null;
        for (MavenProject reactorProject : sortedReactorProjects) {
            // Stop if reached the current module in Reactor build order
            if (reactorProject.equals(project)) {
                break;
            }
            if (graph.getDownstreamProjects(reactorProject, true).isEmpty()) {
                mostDownstreamModule = reactorProject;
                break;
            }
        }
        if (mostDownstreamModule != null && !mostDownstreamModule.equals(project)) {
            getLog().debug("Found a previous module in the Reactor build order that does not have downstream dependencies: "
                    + mostDownstreamModule);
            if (isSubModule(project, mostDownstreamModule)) {
                getLog().debug(
                        "Detected that this multi module pom contains another module that does not have downstream dependencies. Skipping goal on this module.");
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the module's relative path (i.e. the module name) relative to the multi
     * module project directory.
     * 
     * @param module The module for which you want to get the path
     * @return The module path relative to the multi module project directory
     */
    protected String getModuleRelativePath(MavenProject module) {
        return multiModuleProjectDirectory.toPath().relativize(module.getBasedir().toPath()).toString();
    }
    
    protected Plugin getLibertyPluginForProject(MavenProject currentProject) {
        // Try getting the version from Maven 3's plugin descriptor
        String version = null;
        if (plugin != null && plugin.getPlugin() != null) {
            version = plugin.getVersion();
            getLog().debug("Setting plugin version to " + version);
        }
        Plugin projectPlugin = currentProject.getPlugin(LIBERTY_MAVEN_PLUGIN_GROUP_ID + ":" + LIBERTY_MAVEN_PLUGIN_ARTIFACT_ID);
        if (projectPlugin == null) {
            getLog().debug("Did not find liberty-maven-plugin configured in currentProject: "+currentProject.toString());
            projectPlugin = getPluginFromPluginManagement(LIBERTY_MAVEN_PLUGIN_GROUP_ID, LIBERTY_MAVEN_PLUGIN_ARTIFACT_ID, currentProject);
        } else {
            getLog().debug("Found liberty-maven-plugin configured in currentProject: "+currentProject.toString());
        }
        if (projectPlugin == null) {
            getLog().debug("Did not find liberty-maven-plugin in pluginManagement in currentProject: "+currentProject.toString());
            projectPlugin = plugin(LIBERTY_MAVEN_PLUGIN_GROUP_ID, LIBERTY_MAVEN_PLUGIN_ARTIFACT_ID, "LATEST");
        }
        if (version != null) {
            projectPlugin.setVersion(version);
        }
        return projectPlugin;
    }
    
    protected Plugin getPluginFromPluginManagement(String groupId, String artifactId, MavenProject currentProject) {
        Plugin retVal = null;
        PluginManagement pm = currentProject.getPluginManagement();
        if (pm != null) {
            for (Plugin p : pm.getPlugins()) {
                if (groupId.equals(p.getGroupId()) && artifactId.equals(p.getArtifactId())) {
                    retVal = p;
                    break;
                }
            }
        }
        return retVal;
    }
    
    /**
     * Given the groupId and artifactId get the corresponding plugin for the
     * specified project
     * 
     * @param groupId
     * @param artifactId
     * @param currentProject
     * @return Plugin
     */
    protected Plugin getPluginForProject(String groupId, String artifactId, MavenProject currentProject) {
        Plugin plugin = currentProject.getPlugin(groupId + ":" + artifactId);
        if (plugin == null) {
            plugin = getPluginFromPluginManagement(groupId, artifactId, currentProject);
        }
        if (plugin == null) {
            plugin = plugin(groupId(groupId), artifactId(artifactId), version("RELEASE"));
        }
        return plugin;
    }
}
