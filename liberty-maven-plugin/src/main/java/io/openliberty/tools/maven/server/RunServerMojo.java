/**
 * (C) Copyright IBM Corporation 2014, 2025.
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
package io.openliberty.tools.maven.server;

import java.io.IOException;
import java.util.List;

import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import io.openliberty.tools.ant.ServerTask;

/**
 * Start a liberty server
 */
@Mojo(name = "run", requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class RunServerMojo extends PluginConfigSupport {

    /**
     * Clean all cached information on server start up.
     */
    @Parameter(property = "clean", defaultValue = "false")
    protected boolean clean;
    
    /**
     * Run the server in embedded mode
     */
    @Parameter(property = "embedded", defaultValue = "false")
    private boolean embedded;
    
    @Override
    public void execute() throws MojoExecutionException {
        init();

        if (skip) {
            getLog().info("\nSkipping run goal.\n");
            return;
        }

        doRunServer();
    }

    private void doRunServer() throws MojoExecutionException {
        String projectPackaging = project.getPackaging();

        // If there are downstream projects (e.g. other modules depend on this module in the Maven Reactor build order),
        // then skip running Liberty on this module but only build it.
        boolean hasDownstreamProjects = false;
        ProjectDependencyGraph graph = session.getProjectDependencyGraph();
        if (graph != null) {
        	
        	// In a multi-module build, the run server goal will only be run on one project (the farthest downstream) and compile will
        	// be run on any relative upstream projects. If this current project in the Maven Reactor is not one of those projects, skip it.  
        	boolean skipJars = true;
        	if("spring-boot-project".equals(getDeployPackages())) {
        		skipJars = false;
        	}
        	List<MavenProject> relevantProjects = getRelevantMultiModuleProjects(graph, skipJars);
        	if (!relevantProjects.contains(project)) {
        		getLog().info("\nSkipping module " + project.getArtifactId() + " which is not included in this invocation of the run goal.\n");
        		return;
        	}

            List<MavenProject> downstreamProjects = graph.getDownstreamProjects(project, true);
            if (!downstreamProjects.isEmpty()) {
                getLog().debug("Downstream projects: " + downstreamProjects);
                hasDownstreamProjects = true;
            }

            if (containsPreviousLibertyModule(graph)) {
                // skip this module
                return;
            }
        }

        // Proceed to build this module (regardless of whether Liberty will run on it afterwards)
        if (projectPackaging.equals("ear")) {
            runMojo("org.apache.maven.plugins", "maven-ear-plugin", "generate-application-xml");
            runMojo("org.apache.maven.plugins", "maven-resources-plugin", "resources");

            if (hasDownstreamProjects && looseApplication) {
                getOrCreateEarArtifact(project);
            }
        } else if (projectPackaging.equals("pom")) {
            getLog().debug("Skipping compile/resources on module with pom packaging type");
        } else {
            runMojo("org.apache.maven.plugins", "maven-resources-plugin", "resources");
            runMojo("org.apache.maven.plugins", "maven-compiler-plugin", "compile");
            updateArtifactPathToOutputDirectory(project);
        }

        if (!looseApplication) {
            // no need to repackage war/jar if deploy package is specified as spring-boot-project
            if ("spring-boot-project".equals(getDeployPackages())) {
                getLog().info("Skipping project repackaging as deploy package is configured as spring-boot-project");
            } else {
                try {
                    switch (projectPackaging) {
                        case "war":
                            runMojo("org.apache.maven.plugins", "maven-war-plugin", "war");
                            break;
                        case "ear":
                            runMojo("org.apache.maven.plugins", "maven-ear-plugin", "ear");
                            break;
                        case "ejb":
                            runMojo("org.apache.maven.plugins", "maven-ejb-plugin", "ejb");
                            break;
                        case "bundle":
                            runMojo("org.apache.felix", "maven-bundle-plugin", "bundle");
                            break;
                        case "jar":
                            runMojo("org.apache.maven.plugins", "maven-jar-plugin", "jar");
                            break;
                    }

                } catch (MojoExecutionException e) {
                    if (graph != null && !graph.getUpstreamProjects(project, true).isEmpty()) {
                        // this module is a non-loose app, so warn that any upstream modules must also be set to non-loose
                        getLog().warn("The looseApplication parameter was set to false for the module with artifactId " + project.getArtifactId() + ". Ensure that all modules use the same value for the looseApplication parameter by including -DlooseApplication=false in the Maven command for your multi module project.");
                        throw e;
                    }
                }
            }
        }
        // Return if Liberty should not be run on this module
        if (hasDownstreamProjects) {
            return;
        }
        
        runLibertyMojoCreate();
        runLibertyMojoInstallFeature(null, null, null);
        runLibertyMojoDeploy(false);

        ServerTask serverTask = initializeJava();
        try {
            copyConfigFiles();
        } catch (IOException e) {
            throw new MojoExecutionException("Error copying configuration files to Liberty server directory.", e);
        }
        serverTask.setUseEmbeddedServer(embedded);
        serverTask.setClean(clean);
        serverTask.setOperation("run");       
        serverTask.execute();
    }

}
