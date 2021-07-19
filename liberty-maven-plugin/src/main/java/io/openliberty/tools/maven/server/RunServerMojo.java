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
package io.openliberty.tools.maven.server;

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
@Mojo(name = "run", requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
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
    protected void doExecute() throws Exception {
        if (skip) {
            getLog().info("\nSkipping run goal.\n");
            return;
        }
        String projectPackaging = project.getPackaging();

        // If there are downstream projects (e.g. other modules depend on this module in the Maven Reactor build order),
        // then skip running Liberty on this module but only build it.
        boolean hasDownstreamProjects = false;
        ProjectDependencyGraph graph = session.getProjectDependencyGraph();
        if (graph != null) {
            checkMultiModuleConflicts(graph);

            List<MavenProject> downstreamProjects = graph.getDownstreamProjects(project, true);
            if (!downstreamProjects.isEmpty()) {
                log.debug("Downstream projects: " + downstreamProjects);
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
                installEmptyEarIfNotFound(project);
            }
        } else if (projectPackaging.equals("pom")) {
            log.debug("Skipping compile/resources on module with pom packaging type");
        } else {
            if (hasDownstreamProjects && looseApplication) {
                purgeLocalRepositoryArtifact();
            }
            
            runMojo("org.apache.maven.plugins", "maven-resources-plugin", "resources");
            runMojo("org.apache.maven.plugins", "maven-compiler-plugin", "compile");
        }

        if (!looseApplication) {
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
                    case "jar":
                        runMojo("org.apache.maven.plugins", "maven-jar-plugin", "jar");
                        break;
                }
            } catch (MojoExecutionException e) {
                if (graph != null && !graph.getUpstreamProjects(project, true).isEmpty()) {
                    // this module is a non-loose app, so warn that any upstream modules must also be set to non-loose
                    log.warn("The looseApplication parameter was set to false for the module with artifactId " + project.getArtifactId() + ". Ensure that all modules use the same value for the looseApplication parameter by including -DlooseApplication=false in the Maven command for your multi module project.");
                    throw e;
                }
            }
        }

        // Return if Liberty should not be run on this module
        if (hasDownstreamProjects) {
            return;
        }
        
        runLibertyMojoCreate();
        runLibertyMojoInstallFeature(null, null);
        runLibertyMojoDeploy(false);

        ServerTask serverTask = initializeJava();
        copyConfigFiles();
        serverTask.setUseEmbeddedServer(embedded);
        serverTask.setClean(clean);
        serverTask.setOperation("run");       
        serverTask.execute();
    }

}
