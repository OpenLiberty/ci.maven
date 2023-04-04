/**
 * (C) Copyright IBM Corporation 2021, 2023.
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import io.openliberty.tools.common.plugins.util.ServerFeatureUtil;

public class ServerFeatureSupport extends BasicSupport {

    private ServerFeatureUtil servUtil;

    protected class ServerFeatureMojoUtil extends ServerFeatureUtil {

        @Override
        public void debug(String msg) {
            if (isDebugEnabled()) {
                log.debug(msg);
            }
        }

        @Override
        public void debug(String msg, Throwable e) {
            if (isDebugEnabled()) {
                log.debug(msg, e);
            }
        }

        @Override
        public void debug(Throwable e) {
            if (isDebugEnabled()) {
                log.debug(e);
            }
        }

        @Override
        public void warn(String msg) {
            if (!suppressLogs) {
                log.warn(msg);
            } else {
                debug(msg);
            }
        }

        @Override
        public void info(String msg) {
            if (!suppressLogs) {
                log.info(msg);
            } else {
                debug(msg);
            }
        }

        @Override
        public void error(String msg, Throwable e) {
            log.error(msg, e);
        }

        @Override
        public void error(String msg) {
            log.error(msg);
        }

        @Override
        public boolean isDebugEnabled() {
            return log.isDebugEnabled();
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
    protected ServerFeatureUtil getServerFeatureUtil(boolean suppressLogs) {
        if (servUtil == null) {
            createNewServerFeatureUtil();
        }
        if (suppressLogs) {
            servUtil.setSuppressLogs(true);
        } else {
            servUtil.setSuppressLogs(false);
        }
        return servUtil;
    }

    /**
     * Check the Reactor build order for multi module conflicts. Multi module logic
     * is utilized by DevMojo, RunMojo and GenerateFeaturesMojo.
     * 
     * A conflict is multiple modules that do not have downstream modules depending
     * on them, and are not submodules of each other.
     * For example, if the Reactor tree looks like:
     * - ear
     * - war
     * - jar
     * - jar2
     * - pom (top level multi module pom.xml containing ear, war, jar, jar2 as
     * modules)
     * Then ear and jar2 conflict with each other, but pom does not conflict because
     * ear and jar2 are sub-modules of it.
     * 
     * @param graph The project dependency graph
     * 
     * @throws MojoExecutionException If there are multiple modules that conflict
     */
    protected void checkMultiModuleConflicts(ProjectDependencyGraph graph) throws MojoExecutionException {
        List<MavenProject> sortedReactorProjects = graph.getSortedProjects();
        Set<MavenProject> conflicts = new LinkedHashSet<MavenProject>(); // keeps the order of items added in

        // a leaf here is a module without any downstream modules depending on it
        List<MavenProject> leaves = new ArrayList<MavenProject>();
        for (MavenProject reactorProject : sortedReactorProjects) {
            if (graph.getDownstreamProjects(reactorProject, true).isEmpty()) {
                leaves.add(reactorProject);
            }
        }

        for (MavenProject leaf1 : leaves) {
            for (MavenProject leaf2 : leaves) {
                if (leaf1 != leaf2 && !(isSubModule(leaf2, leaf1) || isSubModule(leaf1, leaf2))) {
                    conflicts.add(leaf1);
                    conflicts.add(leaf2);
                }
            }
        }

        List<String> conflictModuleRelativeDirs = new ArrayList<String>();
        for (MavenProject conflict : conflicts) {
            // make the module path relative to the multi module project directory
            conflictModuleRelativeDirs.add(getModuleRelativePath(conflict));
        }

        boolean hasMultipleLibertyModules = !conflicts.isEmpty();

        if (hasMultipleLibertyModules) {
            throw new MojoExecutionException("Found multiple independent modules in the Reactor build order: "
                    + conflictModuleRelativeDirs
                    + ". Specify the module containing the Liberty configuration that you want to use for the server by including the following parameters in the Maven command: -pl <module-with-liberty-config> -am");
        }
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
            log.debug("Found a previous module in the Reactor build order that does not have downstream dependencies: "
                    + mostDownstreamModule);
            if (isSubModule(project, mostDownstreamModule)) {
                log.debug(
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

}
