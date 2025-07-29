/**
 * (C) Copyright IBM Corporation 2021, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.openliberty.tools.maven.server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingResult;

import io.openliberty.tools.common.plugins.util.BinaryScannerUtil;
import static io.openliberty.tools.common.plugins.util.BinaryScannerUtil.*;
import io.openliberty.tools.common.plugins.util.GenerateFeaturesUtil;
import io.openliberty.tools.common.plugins.util.GenerateFeaturesUtil.GenerateFeaturesException;
import io.openliberty.tools.common.plugins.util.PluginExecutionException;
import io.openliberty.tools.common.plugins.util.ServerFeatureUtil;
import io.openliberty.tools.common.plugins.util.ServerFeatureUtil.FeaturesPlatforms;

/**
 * This mojo generates the features required in the featureManager element in
 * server.xml. It examines the dependencies declared in the pom.xml and the
 * features already declared in the featureManager elements in the XML
 * configuration files. Then it generates any missing feature names and stores
 * them in a new featureManager element in a new XML file in the source
 * config/dropins directory.
 */
@Mojo(name = "generate-features", threadSafe = true)
public class GenerateFeaturesMojo extends PluginConfigSupport {

    // The executable file used to scan binaries for the Liberty features they use.
    private File binaryScanner;
    List<MavenProject> upstreamProjects = new ArrayList<MavenProject>();

    @Parameter(property = "classFiles")
    private List<String> classFiles;

    /**
     * If optimize is true, pass all class files and only user specified features to binary scanner
     * Otherwise, if optimize is false, pass only provided updated class files (if any) and all existing features to binary scanner
     */
    @Parameter(property = "optimize", defaultValue = "true")
    private boolean optimize;

    /**
     * If generateToSrc is true, then create the file containing new features in the src directory
     * Otherwise, place the file in the target directory where the Liberty server is defined.
     */
    @Parameter(property = "generateToSrc", defaultValue = "false")
    private boolean generateToSrc;

    @Override
    protected void init() throws MojoExecutionException {
        // @see io.openliberty.tools.maven.BasicSupport#init()
        // Skip server directories setup when generate features only requires
        // the files in the src config directory.
        // The server directories to be set up: install dir, wlp dir, outputdir, etc.
        if (generateToSrc) {
            this.skipServerConfigSetup = true;
        }

        super.init();
    }
    
    @Override
    public void execute() throws MojoExecutionException {
        init();

        if (skip) {
            getLog().info("\nSkipping generate-features goal.\n");
            return;
        }
        try {
            generateFeatures();
        } catch (PluginExecutionException e) {
            throw new MojoExecutionException("Error during generation of features.", e);
        }
    }

    /**
     * Generates features for the application given the API usage detected by the binary scanner and
     * taking any user specified features into account
     * 
     * @throws MojoExecutionException
     * @throws PluginExecutionException indicates the binary-app-scanner.jar could
     *                                  not be found
     */
    private void generateFeatures() throws MojoExecutionException, PluginExecutionException {
        // If there are downstream projects (e.g. other modules depend on this module in the Maven Reactor build order),
        // then skip generate-features on this module
        upstreamProjects.clear();
        ProjectDependencyGraph graph = session.getProjectDependencyGraph();
        if (graph != null) {
        	
        	// In a multi-module build, generate-features will only be run on one project (the farthest downstream). 
        	// If this current project in the Maven Reactor is not that project or any of its upstream projects, skip it.  
        	boolean skipJars = true;
        	if("spring-boot-project".equals(getDeployPackages())) {
        		skipJars = false;
        	}
        	List<MavenProject> relevantProjects = getRelevantMultiModuleProjects(graph, skipJars);
        	if (!relevantProjects.contains(project)) {
        		getLog().info("\nSkipping module " + project.getArtifactId() + " which is not configured for the generate-features goal.\n");
        		return;
        	}
        	
            List<MavenProject> downstreamProjects = graph.getDownstreamProjects(project, true);
            if (!downstreamProjects.isEmpty()) {
                getLog().debug("Downstream projects: " + downstreamProjects);
                return;
            } else {
                // get all upstream projects
                for (MavenProject upstreamProj : graph.getUpstreamProjects(project, true)) {
                    try {
                        // when GenerateFeaturesMojo is called from dev mode on a multi module project,
                        // the upstream project umbrella dependencies may not be up to date. Call
                        // getMavenProject to rebuild the project with the current Maven session,
                        // ensuring that the latest umbrella dependencies are loaded
                        upstreamProjects.add(getMavenProject(upstreamProj.getFile()));
                    } catch (ProjectBuildingException e) {
                        getLog().debug("Could not resolve the upstream project: " + upstreamProj.getFile()
                                + " using the current Maven session. Falling back to last resolved upstream project.");
                        upstreamProjects.add(upstreamProj); // fail gracefully, use last resolved project
                    }
                }
            }

            if (containsPreviousLibertyModule(graph)) {
                // skip this module
                return;
            }
        }

        binaryScanner = getBinaryScannerJarFromRepository();
        BinaryScannerHandler binaryScannerHandler = new BinaryScannerHandler(binaryScanner);
        getLog().debug("Binary scanner jar: " + binaryScanner.getName());

        GenerateFeaturesHandler generateFeaturesHandler = new GenerateFeaturesHandler(project,
                binaryScannerHandler, configDirectory, serverDirectory, classFiles, GenerateFeaturesUtil.HEADER_M);
        try {
            generateFeaturesHandler.generateFeatures(optimize, generateToSrc);
        } catch (GenerateFeaturesException e) {
            throw new MojoExecutionException(e.getMessage(), e.getCause());
        }
    }

    // Get the features from the server config and optionally exclude the specified config files from the search.
    private Set<String> getServerFeatures(ServerFeatureUtil servUtil, File generationContextDir, Set<String> generatedFiles, boolean excludeGenerated) {
        servUtil.setLowerCaseFeatures(false);
        // if optimizing, ignore generated files when passing in existing features to
        // binary scanner
        FeaturesPlatforms fp = servUtil.getServerFeatures(generationContextDir, serverXmlFile,
                new HashMap<String, File>(), excludeGenerated ? generatedFiles : null); // pass generatedFiles to exclude them
        servUtil.setLowerCaseFeatures(true);
        if (fp == null) {
            return new HashSet<String>();
        }
        return fp.getFeatures();
    }

    /**
     * Gets the binary scanner jar file from the local cache.
     * Downloads it first from connected repositories such as Maven Central if a newer release is available than the cached version.
     * Note: Maven updates artifacts daily by default based on the last updated timestamp. Users should use 'mvn -U' to force updates if needed.
     * 
     * @return The File object of the binary scanner jar in the local cache.
     * @throws PluginExecutionException
     */
    private File getBinaryScannerJarFromRepository() throws PluginExecutionException {
        try {
            return getArtifact(BINARY_SCANNER_MAVEN_GROUP_ID, BINARY_SCANNER_MAVEN_ARTIFACT_ID, BINARY_SCANNER_MAVEN_TYPE, BINARY_SCANNER_MAVEN_VERSION).getFile();
        } catch (Exception e) {
            throw new PluginExecutionException("Could not retrieve the artifact " + BINARY_SCANNER_MAVEN_GROUP_ID + "."
                    + BINARY_SCANNER_MAVEN_ARTIFACT_ID
                    + " needed for liberty:generate-features. Ensure you have a connection to Maven Central or another repository that contains the "
                    + BINARY_SCANNER_MAVEN_GROUP_ID + "." + BINARY_SCANNER_MAVEN_ARTIFACT_ID
                    + ".jar configured in your pom.xml.",
                    e);
        }
    }

    // Return a list containing the classes directory of the Maven projects (upstream projects and main project)
    private Set<String> getClassesDirectories(List<MavenProject> mavenProjects) throws MojoExecutionException {
        Set<String> dirs = new HashSet<String>();
        String classesDirName = null;
        getLog().debug("For binary scanner gathering Java build output directories for Maven projects, size="
                + mavenProjects.size());
        for (MavenProject mavenProject : mavenProjects) {
            classesDirName = getClassesDirectory(mavenProject.getBuild().getOutputDirectory());
            if (classesDirName != null) {
                dirs.add(classesDirName);
            }
        }
        for (String s : dirs) {
            getLog().debug("Found dir:" + s);
        }
        return dirs;
    }

    // Check one directory and if it exists return its canonical path (or absolute path if error).
    private String getClassesDirectory(String outputDir) {
        File classesDir = new File(outputDir);
        try {
            if (classesDir.exists()) {
                return classesDir.getCanonicalPath();
            }
        } catch (IOException x) {
            String classesDirAbsPath = classesDir.getAbsolutePath();
            getLog().debug("IOException obtaining canonical path name for a project's classes directory: " + classesDirAbsPath);
            return classesDirAbsPath;
        }
        return null; // directory does not exist.
    }

    /**
     * Returns the EE major version detected for the given MavenProjects
     * 
     * @param mavenProjects project modules, for single module projects list of size 1
     * @return the latest version of EE detected across multiple project modules,
     *         null if an EE version is not found or the version number is out of range
     */
    public String getEEVersion(List<MavenProject> mavenProjects) {
        String eeVersion = null;
        if (mavenProjects != null) {
            Set<String> eeVersionsDetected = new HashSet<String>();
            for (MavenProject mavenProject : mavenProjects) {
                try {
                    String ver = getEEVersion(mavenProject);
                    getLog().debug("Java and/or Jakarta EE umbrella dependency found in project: " + mavenProject.getName());
                    if (ver != null) {
                        eeVersionsDetected.add(ver);
                    }
                } catch (NoUmbrellaDependencyException e) {
                    // umbrella dependency does not exist, do nothing
                }
            }
            if (!eeVersionsDetected.isEmpty()) {
                eeVersion = eeVersionsDetected.iterator().next();
                // if multiple EE versions are found across multiple modules, return the latest version
                for (String ver : eeVersionsDetected) {
                    if (ver.compareTo(eeVersion) > 0) {
                        eeVersion = ver;
                    }
                }
            }
            if (eeVersionsDetected.size() > 1) {
                getLog().debug(
                        "Multiple Java and/or Jakarta EE versions found across multiple project modules, using the latest version ("
                                + eeVersion + ") found to generate Liberty features.");
            }
        }
        return eeVersion;
    }

    /**
     * Returns the EE major version detected for the given MavenProject.
     * To match the Maven "nearest in the dependency tree" strategy, this method
     * will return the first EE umbrella dependency version detected.
     * 
     * @param project the MavenProject to search
     * @return EE major version corresponding to the EE umbrella dependency
     * @throws NoUmbrellaDependencyException indicates that the umbrella dependency was not found
     */
    private String getEEVersion(MavenProject project) throws NoUmbrellaDependencyException {
        if (project != null) {
            List<Dependency> dependencies = project.getDependencies();
            for (Dependency d : dependencies) {
                if (!d.getScope().equals("provided")) {
                    continue;
                }
                if ((d.getGroupId().equals("javax") && d.getArtifactId().equals("javaee-api")) ||
                    (d.getGroupId().equals("jakarta.platform") && d.getArtifactId().equals("jakarta.jakartaee-api"))) {
                    return d.getVersion();
                }
            }
        }
        throw new NoUmbrellaDependencyException();
    }

    /**
     * Returns the MicroProfile version detected for the given MavenProjects
     * 
     * @param mavenProjects project modules, for single module projects list of size 1
     * @return the latest version of MP detected across multiple project modules,
     *         null if an MP version is not found or the version number is out of range
     */
    public String getMPVersion(List<MavenProject> mavenProjects) {
        String mpVersion = null;
        if (mavenProjects != null) {
            Set<String> mpVersionsDetected = new HashSet<String>();
            for (MavenProject mavenProject : mavenProjects) {
                try {
                    String ver = getMPVersion(mavenProject);
                    getLog().debug("MicroProfile umbrella dependency found in project: " + mavenProject.getName());
                    if (ver != null) {
                        mpVersionsDetected.add(ver);
                    }
                } catch (NoUmbrellaDependencyException e) {
                    // umbrella dependency does not exist, do nothing
                }
            }
            if (!mpVersionsDetected.isEmpty()) {
                mpVersion = mpVersionsDetected.iterator().next();
                // if multiple MP versions are found across multiple modules, return the latest version
                for (String ver : mpVersionsDetected) {
                    if (ver.compareTo(mpVersion) > 0) {
                        mpVersion = ver;
                    }
                }
            }
            if (mpVersionsDetected.size() > 1) {
                getLog().debug(
                        "Multiple MicroProfile versions found across multiple project modules, using the latest version ("
                                + mpVersion + ") found to generate Liberty features.");
            }
        }
        return mpVersion;
    }

    /**
     * Returns the MicroProfile (MP) version detected for the given MavenProject
     * To match the Maven "nearest in the dependency tree" strategy, this method
     * will return the first MP umbrella dependency version detected.
     * 
     * @param project the MavenProject to search
     * @return MP exact version code corresponding to the MP umbrella dependency
     * @throws NoUmbrellaDependencyException indicates that the umbrella dependency was not found
     */
    public String getMPVersion(MavenProject project) throws NoUmbrellaDependencyException { // figure out correct level of MP from declared dependencies
        if (project != null) {
            List<Dependency> dependencies = project.getDependencies();
            for (Dependency d : dependencies) {
                if (!d.getScope().equals("provided")) {
                    continue;
                }
                if (d.getGroupId().equals("org.eclipse.microprofile") &&
                        d.getArtifactId().equals("microprofile")) {
                    return d.getVersion();
                }
            }
        }
        throw new NoUmbrellaDependencyException();
    }

    private class GenerateFeaturesHandler extends GenerateFeaturesUtil {
        public GenerateFeaturesHandler(Object project, BinaryScannerUtil binaryScannerHandler, File configDirectory, File serverDirectory, List<String> classFiles, String header) {
            super(project, binaryScannerHandler, configDirectory, serverDirectory, classFiles, header);
        }
        @Override
        public ServerFeatureUtil getServerFeatureUtil(boolean suppress, Map files) {
            return GenerateFeaturesMojo.this.getServerFeatureUtil(suppress, files);
        }
        @Override
        public Set<String> getServerFeatures(ServerFeatureUtil servUtil, File generationContextDir, Set<String> generatedFiles, boolean excludeGenerated) {
            return GenerateFeaturesMojo.this.getServerFeatures(servUtil, generationContextDir, generatedFiles, excludeGenerated);
        }
        @Override
        public Set<String> getClassesDirectories(List projects) throws GenerateFeaturesException {
            try {
                return GenerateFeaturesMojo.this.getClassesDirectories((List<MavenProject>) projects);
            } catch (MojoExecutionException e) {
                throw new GenerateFeaturesException(e.getMessage(), e.getCause());
            }
        }
        @Override
        public List<Object> getProjectList(Object project) {
            List<Object> mavenProjects = new ArrayList<>();
            mavenProjects.addAll(upstreamProjects);
            mavenProjects.add((MavenProject)project);
            return mavenProjects;
        }
        @Override
        public String getEEVersion(List projects) {
            return GenerateFeaturesMojo.this.getEEVersion(projects);
        }
        @Override
        public String getMPVersion(List projects) {
            return GenerateFeaturesMojo.this.getMPVersion(projects);
        }
        @Override
        public String getLogLocation(Object project) {
            return ((MavenProject)project).getBuild().getDirectory();
        }
        @Override
        public File getServerXmlFile() {
            return serverXmlFile;
        }
        @Override
        public void debug(String msg) {
            getLog().debug(msg);
        }
        @Override
        public void debug(String msg, Throwable t) {
            getLog().debug(msg, t);
        }
        @Override
        public void warn(String msg) {
            getLog().warn(msg);
        }
        @Override
        public void info(String msg) {
            getLog().info(msg);
        }
    }

    // Define the logging functions of the binary scanner handler and make it available in this plugin
    private class BinaryScannerHandler extends BinaryScannerUtil {
        BinaryScannerHandler(File scannerFile) {
            super(scannerFile);
        }
        @Override
        public void debug(String msg) {
            getLog().debug(msg);
        }
        @Override
        public void debug(String msg, Throwable t) {
            getLog().debug(msg, t);
        }
        @Override
        public void error(String msg) {
            getLog().error(msg);
        }
        @Override
        public void warn(String msg) {
            getLog().warn(msg);
        }
        @Override
        public void info(String msg) {
            getLog().info(msg);
        }
        @Override
        public boolean isDebugEnabled() {
            return getLog().isDebugEnabled();
        }
    }

    // using the current MavenSession build the project (resolves dependencies)
    private MavenProject getMavenProject(File buildFile) throws ProjectBuildingException {
        ProjectBuildingResult build = mavenProjectBuilder.build(buildFile,
                session.getProjectBuildingRequest().setResolveDependencies(true));
        return build.getProject();
    }

    /**
     * Class to indicate that an umbrella dependency was not found in the build file
     */
    public class NoUmbrellaDependencyException extends Exception {
        private static final long serialVersionUID = 1L;
    }

}
