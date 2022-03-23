/**
 * (C) Copyright IBM Corporation 2019, 2021.
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

import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.twdata.maven.mojoexecutor.MojoExecutor.Element;

import io.openliberty.tools.ant.ServerTask;
import io.openliberty.tools.common.plugins.util.DevUtil;
import io.openliberty.tools.common.plugins.util.JavaCompilerOptions;
import io.openliberty.tools.common.plugins.util.PluginExecutionException;
import io.openliberty.tools.common.plugins.util.PluginScenarioException;
import io.openliberty.tools.common.plugins.util.ServerFeatureUtil;
import io.openliberty.tools.common.plugins.util.ServerStatusUtil;
import io.openliberty.tools.common.plugins.util.ProjectModule;
import io.openliberty.tools.maven.BasicSupport;
import io.openliberty.tools.maven.applications.DeployMojoSupport;
import io.openliberty.tools.maven.applications.LooseWarApplication;
import io.openliberty.tools.maven.utils.ExecuteMojoUtil;

/**
 * Start a liberty server in dev mode import to set ResolutionScope for TEST as
 * it helps build full transitive dependency classpath
 */
@Mojo(name = "dev", requiresDependencyCollection = ResolutionScope.TEST, requiresDependencyResolution = ResolutionScope.TEST)
public class DevMojo extends LooseAppSupport {

    private static final String TEST_RUN_ID_PROPERTY_NAME = "liberty.dev.test.run.id";
    private static final String LIBERTY_HOSTNAME = "liberty.hostname";
    private static final String LIBERTY_HTTP_PORT = "liberty.http.port";
    private static final String LIBERTY_HTTPS_PORT = "liberty.https.port";
    private static final String MICROSHED_HOSTNAME = "microshed_hostname";
    private static final String MICROSHED_HTTP_PORT = "microshed_http_port";
    private static final String MICROSHED_HTTPS_PORT = "microshed_https_port";
    private static final String WLP_USER_DIR_PROPERTY_NAME = "wlp.user.dir";

    DevMojoUtil util = null;

    @Parameter(property = "hotTests", defaultValue = "false")
    private boolean hotTests;

    @Parameter(property = "skipTests", defaultValue = "false")
    private boolean skipTests;

    @Parameter(property = "skipUTs", defaultValue = "false")
    private boolean skipUTs;

    @Parameter(property = "skipITs", defaultValue = "false")
    private boolean skipITs;

    @Parameter(property = "debug", defaultValue = "true")
    private boolean libertyDebug;

    @Parameter(property = "debugPort", defaultValue = "7777")
    private int libertyDebugPort;

    @Parameter(property = "container", defaultValue = "false")
    private boolean container;

    @Parameter(property = "generateFeatures", defaultValue = "true")
    private boolean generateFeatures;

    /**
     * Whether to recompile dependencies. Defaults to false for single module
     * projects and true for multi module projects. Since the default behavior
     * changes between single module and multi module projects, need to accept param
     * as a string.
     */
    @Parameter(property = "recompileDependencies")
    private String recompileDependencies;

    /**
     * Time in seconds to wait before processing Java changes and deletions.
     */
    @Parameter(property = "compileWait", defaultValue = "0.5")
    private double compileWait;

    private int runId = 0;

    private ServerTask serverTask = null;

    private Plugin boostPlugin = null;

    @Component
    protected ProjectBuilder mavenProjectBuilder;

    @Component
    private RuntimeInformation runtime;

    /**
     * Time in seconds to wait while verifying that the application has started or
     * updated.
     */
    @Parameter(property = "verifyTimeout", defaultValue = "30")
    private int verifyTimeout;

    /**
     * Time in seconds to wait while verifying that the server has started.
     */
    @Parameter(property = "serverStartTimeout", defaultValue = "90")
    private int serverStartTimeout;

    /**
     * comma separated list of app names to wait for
     */
    @Parameter(property = "applications")
    private String applications;

    /**
     * Clean all cached information on server start up.
     */
    @Parameter(property = "clean", defaultValue = "false")
    protected boolean clean;

    /**
     * Poll for file changes instead of using file system notifications (test only).
     */
    @Parameter(property = "pollingTest", defaultValue = "false")
    protected boolean pollingTest;

    /**
     * Dockerfile used to build a Docker image to then start a container with
     */
    @Parameter(property = "dockerfile")
    private File dockerfile;

    /**
     * Context (directory) to use for the Docker build when building the container image
     */
    @Parameter(property = "dockerBuildContext")
    private File dockerBuildContext;

    /**
     * The directory for source files.
     */
    @Parameter(readonly = true, required = true, defaultValue = " ${project.build.sourceDirectory}")
    private String sourceDirectoryString;
    private File sourceDirectory;

    /**
     * The directory for test source files.
     */
    @Parameter(readonly = true, required = true, defaultValue = " ${project.build.testSourceDirectory}")
    private String testSourceDirectoryString;
    private File testSourceDirectory;

    /**
     * The directory for compiled classes.
     */
    @Parameter(readonly = true, required = true, defaultValue = "${project.build.outputDirectory}")
    private File outputDirectory;

    /**
     * The directory for compiled test classes.
     */
    @Parameter(readonly = true, required = true, defaultValue = "${project.build.testOutputDirectory}")
    private File testOutputDirectory;

    /**
     * Additional options for the docker run command when dev mode starts a
     * container.
     */
    @Parameter(property = "dockerRunOpts")
    private String dockerRunOpts;

    /**
     * Specify the amount of time in seconds that dev mode waits for the docker
     * build command to run to completion. Default to 600 seconds.
     */
    @Parameter(property = "dockerBuildTimeout", defaultValue = "600")
    private int dockerBuildTimeout;

    /**
     * If true, the default Docker port mappings are skipped in the docker run
     * command
     */
    @Parameter(property = "skipDefaultPorts", defaultValue = "false")
    private boolean skipDefaultPorts;

    /**
     * If true, preserve the temporary Dockerfile used in the docker build command
     */
    @Parameter(property = "keepTempDockerfile", defaultValue = "false")
    private boolean keepTempDockerfile;
    
    private boolean isExplodedLooseWarApp = false;

    /**
     * Set the container option.
     * 
     * @param container whether dev mode should use a container
     */
    protected void setContainer(boolean container) {
        // set container variable for DevMojo
        this.container = container;

        // set project property for use in DeployMojoSupport
        project.getProperties().setProperty("container", Boolean.toString(container));
    }

    protected List<File> getResourceDirectories(MavenProject project, File outputDir) {
    	// Let's just add resources directories unconditionally, the dev util already checks the directories actually exist
        // before adding them to the watch list.   If we avoid checking here we allow for creating them later on.
        List<File> resourceDirs = new ArrayList<File>();
        for (Resource resource : project.getResources()) {
            File resourceFile = new File(resource.getDirectory());
            resourceDirs.add(resourceFile);
        }
        if (resourceDirs.isEmpty()) {
            File defaultResourceDir = new File(project.getBasedir(), "src/main/resources");
            log.debug("No resource directory detected, using default directory: " + defaultResourceDir);
            resourceDirs.add(defaultResourceDir);
        }
        return resourceDirs;
    }

    private class DevMojoUtil extends DevUtil {

        Set<String> existingFeatures;
        Map<String, File> libertyDirPropertyFiles = new HashMap<String, File>();
        List<MavenProject> upstreamMavenProjects;

        public DevMojoUtil(File installDir, File userDir, File serverDirectory, File sourceDirectory,
                File testSourceDirectory, File configDirectory, File projectDirectory, File multiModuleProjectDirectory,
                List<File> resourceDirs, JavaCompilerOptions compilerOptions, String mavenCacheLocation,
                List<ProjectModule> upstreamProjects, List<MavenProject> upstreamMavenProjects, boolean recompileDeps,
                File pom, Map<String, List<String>> parentPoms, boolean generateFeatures, Set<String> compileArtifactPaths, Set<String> testArtifactPaths, 
                List<Path> webResourceDirs) throws IOException {
            super(new File(project.getBuild().getDirectory()), serverDirectory, sourceDirectory, testSourceDirectory,
                    configDirectory, projectDirectory, multiModuleProjectDirectory, resourceDirs, hotTests, skipTests,
                    skipUTs, skipITs, project.getArtifactId(), serverStartTimeout, verifyTimeout, verifyTimeout,
                    ((long) (compileWait * 1000L)), libertyDebug, false, false, pollingTest, container, dockerfile,
                    dockerBuildContext, dockerRunOpts, dockerBuildTimeout, skipDefaultPorts, compilerOptions,
                    keepTempDockerfile, mavenCacheLocation, upstreamProjects, recompileDeps, project.getPackaging(),
                    pom, parentPoms, generateFeatures, compileArtifactPaths, testArtifactPaths, webResourceDirs);

            ServerFeatureUtil servUtil = getServerFeatureUtil();
            this.libertyDirPropertyFiles = BasicSupport.getLibertyDirectoryPropertyFiles(installDir, userDir,
                    serverDirectory);
            this.existingFeatures = servUtil.getServerFeatures(serverDirectory, libertyDirPropertyFiles);
            this.upstreamMavenProjects = upstreamMavenProjects;
        }

        @Override
        public void debug(String msg) {
            log.debug(msg);
        }

        @Override
        public void debug(String msg, Throwable e) {
            log.debug(msg, e);
        }

        @Override
        public void debug(Throwable e) {
            log.debug(e);
        }

        @Override
        public void warn(String msg) {
            log.warn(msg);
        }

        @Override
        public void info(String msg) {
            log.info(msg);
        }

        @Override
        public void error(String msg) {
            log.error(msg);
        }

        @Override
        public void error(String msg, Throwable e) {
            log.error(msg, e);
        }

        @Override
        public boolean isDebugEnabled() {
            return log.isDebugEnabled();
        }

        @Override
        public String getServerStartTimeoutExample() {
            return "'mvn liberty:dev -DserverStartTimeout=120'";
        }

        @Override
        public String getProjectName() {
            return project.getArtifactId();
        }

        @Override
        public void libertyCreate() throws PluginExecutionException {
            try {
                if (isUsingBoost()) {
                    log.info("Running boost:package");
                    runBoostMojo("package");
                } else {
                    runLibertyMojoCreate();
                }
            } catch (MojoExecutionException | ProjectBuildingException e) {
                throw new PluginExecutionException(e);
            }
        }

        @Override
        public boolean libertyGenerateFeatures(Collection<String> classes, boolean optimize) {
            try {
                if (classes != null) {
                    Element[] classesElem = new Element[classes.size()];
                    int i = 0;
                    for (String classPath : classes) {
                        classesElem[i] = element(name("classFile"), classPath);
                        i++;
                    }
                    // generate features for only the classFiles passed
                    runLibertyMojoGenerateFeatures(element(name("classFiles"), classesElem), optimize);
                } else {
                    // pass null for classFiles so that features are generated for ALL of the
                    // classes
                    runLibertyMojoGenerateFeatures(null, optimize);
                }
                return true; // successfully generated features
            } catch (MojoExecutionException e) {
                // log errors instead of throwing an exception so we do not flood console with
                // stacktrace
                if (e.getCause() != null && e.getCause() instanceof PluginExecutionException) {
                    // PluginExecutionException indicates that the binary scanner jar could not be found
                    log.error(e.getMessage() + ".\nDisabling the automatic generation of features.");
                    setFeatureGeneration(false);
                } else {
                    log.error(e.getMessage()
                    + "\nTo disable the automatic generation of features, type 'g' and press Enter.");
                }
                return false;
            }
        }

        @Override
        public void libertyInstallFeature() throws PluginExecutionException {
            try {
                runLibertyMojoInstallFeature(null, container ? super.getContainerName() : null);
            } catch (MojoExecutionException e) {
                throw new PluginExecutionException(e);
            }
        }

        @Override
        public void libertyDeploy() throws PluginExecutionException {
            try {
                runLibertyMojoDeploy();
            } catch (MojoExecutionException e) {
                throw new PluginExecutionException(e);
            }
        }

        @Override
        public void stopServer() {
            super.serverFullyStarted.set(false);

            if (container) {
                // TODO stop the container instead
                return;
            }
            try {
                ServerTask serverTask = initializeJava();
                serverTask.setOperation("stop");
                serverTask.execute();
            } catch (Exception e) {
                log.warn(MessageFormat.format(messages.getString("warn.server.stopped"), serverName));
            }
        }

        @Override
        public ServerTask getServerTask() throws Exception {
            if (serverTask != null) {
                return serverTask;
            } else {
                // Setup server task
                serverTask = initializeJava();
                copyConfigFiles();
                serverTask.setClean(clean);
                if (libertyDebug) {
                    setLibertyDebugPort(libertyDebugPort);

                    // set environment variables for server start task
                    serverTask.setOperation("debug");
                    serverTask.setEnvironmentVariables(getDebugEnvironmentVariables());
                } else {
                    serverTask.setOperation("run");
                }

                return serverTask;
            }
        }

        private Properties getPropertiesWithKeyPrefix(Properties p, String prefix) {
            Properties result = new Properties();
            if (p != null) {
                Enumeration<?> e = p.propertyNames();
                while (e.hasMoreElements()) {
                    String key = (String) e.nextElement();
                    if (key.startsWith(prefix)) {
                        result.put(key, p.get(key));
                    }
                }
            }
            return result;
        }

        private List<Dependency> getEsaDependency(List<Dependency> dependencies) {
            List<Dependency> deps = new ArrayList<Dependency>();
            if (dependencies != null) {
                for (Dependency d : dependencies) {
                    if ("esa".equals(d.getType())) {
                        deps.add(d);
                    }
                }
            }
            return deps;
        }

        // returns a list of dependencies used to build the project (scope compile or provided)
        private List<Dependency> getCompileDependency(List<Dependency> dependencies) {
            List<Dependency> deps = new ArrayList<Dependency>();
            if (dependencies != null) {
                for (Dependency d : dependencies) {
                    if ("compile".equals(d.getScope()) || "provided".equals(d.getScope())) {
                        deps.add(d);
                    }
                }
            }
            return deps;
        }

        // retun false if dependency lists are not equal, true if they are
        private boolean dependencyListsEquals(List<Dependency> oldDeps, List<Dependency> deps) {
            if (oldDeps.size() != deps.size()) {
                return false;
            }
            for (int i = 0; i < oldDeps.size(); i++) {
                if (!dependencyEquals(oldDeps.get(i), deps.get(i))) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Determines if dependency are equal if their groupId, artifactId, version,
         * type and scope are equal
         * 
         * @param dep1
         * @param dep2
         * @return return false if dependency objects are not equal, true if they are
         */
        private boolean dependencyEquals(Dependency dep1, Dependency dep2) {
            if (!dep1.toString().equals(dep2.toString())) {
                // compares groupId, artifactId, version, type
                return false;
            }
            if (!dep1.getScope().equals(dep2.getScope())) {
                return false;
            }
            return true;
        }

        private static final String LIBERTY_BOOTSTRAP_PROP = "liberty.bootstrap.";
        private static final String LIBERTY_JVM_PROP = "liberty.jvm.";
        private static final String LIBERTY_ENV_PROP = "liberty.env.";
        private static final String LIBERTY_VAR_PROP = "liberty.var.";
        private static final String LIBERTY_DEFAULT_VAR_PROP = "liberty.defaultVar.";

        private boolean hasServerPropertyChanged(MavenProject project, MavenProject backupProject) {
            Properties projProp = project.getProperties();
            Properties backupProjProp = backupProject.getProperties();

            if (!Objects.equals(getPropertiesWithKeyPrefix(projProp, LIBERTY_BOOTSTRAP_PROP),
                    getPropertiesWithKeyPrefix(backupProjProp, LIBERTY_BOOTSTRAP_PROP))) {
                return true;
            }

            if (!Objects.equals(getPropertiesWithKeyPrefix(projProp, LIBERTY_JVM_PROP),
                    getPropertiesWithKeyPrefix(backupProjProp, LIBERTY_JVM_PROP))) {
                return true;
            }

            if (!Objects.equals(getPropertiesWithKeyPrefix(projProp, LIBERTY_ENV_PROP),
                    getPropertiesWithKeyPrefix(backupProjProp, LIBERTY_ENV_PROP))) {
                return true;
            }
            return false;
        }

        private boolean hasServerVariableChanged(MavenProject project, MavenProject backupProject) {
            Properties projProp = project.getProperties();
            Properties backupProjProp = backupProject.getProperties();

            if (!Objects.equals(getPropertiesWithKeyPrefix(projProp, LIBERTY_VAR_PROP),
                    getPropertiesWithKeyPrefix(backupProjProp, LIBERTY_VAR_PROP))) {
                return true;
            }
            if (!Objects.equals(getPropertiesWithKeyPrefix(projProp, LIBERTY_DEFAULT_VAR_PROP),
                    getPropertiesWithKeyPrefix(backupProjProp, LIBERTY_DEFAULT_VAR_PROP))) {
                return true;
            }
            return false;
        }

        private boolean restartForLibertyMojoConfigChanged(Xpp3Dom config, Xpp3Dom oldConfig) {
            if (!Objects.equals(config.getChild("bootstrapProperties"), oldConfig.getChild("bootstrapProperties"))) {
                return true;
            } else if (!Objects.equals(config.getChild("bootstrapPropertiesFile"),
                    oldConfig.getChild("bootstrapPropertiesFile"))) {
                return true;
            } else if (!Objects.equals(config.getChild("jvmOptions"), oldConfig.getChild("jvmOptions"))) {
                return true;
            } else if (!Objects.equals(config.getChild("jvmOptionsFile"), oldConfig.getChild("jvmOptionsFile"))) {
                return true;
            } else if (!Objects.equals(config.getChild("serverEnv"), oldConfig.getChild("serverEnv"))) {
                return true;
            } else if (!Objects.equals(config.getChild("serverEnvFile"), oldConfig.getChild("serverEnvFile"))) {
                return true;
            } else if (!Objects.equals(config.getChild("configDirectory"), oldConfig.getChild("configDirectory"))) {
                return true;
            }
            return false;
        }

        @Override
        public boolean updateArtifactPaths(ProjectModule projectModule, boolean redeployCheck, boolean generateFeatures, ThreadPoolExecutor executor)
                throws PluginExecutionException {
            try {
                File buildFile = projectModule.getBuildFile();
                if (buildFile == null) {
                    buildFile = this.buildFile;
                }
                MavenProject upstreamProject = getMavenProject(buildFile);
                MavenProject backupUpstreamProject = upstreamProject;
                for (MavenProject p : upstreamMavenProjects) {
                    if (buildFile != null && p.getFile().getCanonicalPath().equals(buildFile.getCanonicalPath())) {
                        backupUpstreamProject = p;
                    }
                }
                

                // TODO rebuild the corresponding module if the compiler options have changed
                JavaCompilerOptions oldCompilerOptions = getMavenCompilerOptions(backupUpstreamProject);
                JavaCompilerOptions compilerOptions = getMavenCompilerOptions(upstreamProject);
                if (!oldCompilerOptions.getOptions().equals(compilerOptions.getOptions())) {
                    log.debug("Maven compiler options have been modified: " + compilerOptions.getOptions());
                    util.getProjectModule(buildFile).setCompilerOptions(compilerOptions);
                }

                Set<String> testArtifactPaths = projectModule.getTestArtifacts();
                Set<String> compileArtifactPaths = projectModule.getCompileArtifacts();

                if (this.parentBuildFiles.isEmpty()) {
                    compileArtifactPaths.clear();
                    testArtifactPaths.clear();
                } else {
                    // remove past artifacts and add the newest calculated (covers the case where a
                    // dependency was deleted)
                    // do not clear list as it may contain dependencies from parent projects
                    // update classpath for dependencies changes
                    testArtifactPaths.removeAll(backupUpstreamProject.getTestClasspathElements());
                    compileArtifactPaths.removeAll(backupUpstreamProject.getCompileClasspathElements());
                }
                testArtifactPaths.addAll(upstreamProject.getTestClasspathElements());
                compileArtifactPaths.addAll(upstreamProject.getCompileClasspathElements());

                // check if project module is a parent project and update child modules' artifacts
                if (!this.parentBuildFiles.isEmpty()
                        && this.parentBuildFiles.containsKey(projectModule.getBuildFile().getCanonicalPath())) {
                    updateArtifactPaths(projectModule.getBuildFile());
                }

                // check if compile dependencies have changed, regenerate features and redeploy if they have
                if (redeployCheck) {
                    // update upstream Maven projects list
                    int index = upstreamMavenProjects.indexOf(backupUpstreamProject);
                    upstreamMavenProjects.set(index, upstreamProject);

                    List<Dependency> deps = upstreamProject.getDependencies();
                    List<Dependency> oldDeps = backupUpstreamProject.getDependencies();

                    // detect compile dependency changes
                    if (!dependencyListsEquals(getCompileDependency(deps), getCompileDependency(oldDeps))) {
                        // optimize generate features
                        if (generateFeatures) {
                            log.debug("Detected a change in the compile dependencies for "
                                    + buildFile + " , regenerating features");
                            boolean generateFeaturesSuccess = libertyGenerateFeatures(null, true);
                            if (generateFeaturesSuccess) {
                                util.getJavaSourceClassPaths().clear();
                            }
                        }
                        runLibertyMojoDeploy();
                    }
                }
            } catch (ProjectBuildingException | DependencyResolutionRequiredException | IOException
                    | MojoExecutionException e) {
                log.error("An unexpected error occurred while processing changes in " + buildFile.getAbsolutePath()
                        + ": " + e.getMessage());
                log.debug(e);
                return false;
            }
            return true;
        }

        @Override
        public boolean updateArtifactPaths(File buildFile) {
            try {
                MavenProject parentProject = getMavenProject(buildFile);
                updateChildProjectArtifactPaths(buildFile, parentProject.getCompileClasspathElements(),
                        parentProject.getTestClasspathElements());
            } catch (ProjectBuildingException | IOException | DependencyResolutionRequiredException e) {
                log.error("An unexpected error occurred while processing changes in " + buildFile.getAbsolutePath()
                        + ": " + e.getMessage());
                log.debug(e);
                return false;
            }
            return true;
        }

        private void updateChildProjectArtifactPaths(File parentBuildFile, List<String> compileClasspathElements,
                List<String> testClasspathElements) throws IOException, ProjectBuildingException, DependencyResolutionRequiredException {
            // search for child projects
            List<String> childBuildFiles = this.parentBuildFiles.get(parentBuildFile.getCanonicalPath());
            if (childBuildFiles != null) {
                for (String childBuildPath : childBuildFiles) {
                    if (this.parentBuildFiles.containsKey(childBuildPath)) {
                        MavenProject project = getMavenProject(new File(childBuildPath));
                        if (project != null) {
                            compileClasspathElements.addAll(project.getCompileClasspathElements());
                            testClasspathElements.addAll(project.getTestClasspathElements());
                        }
                        updateChildProjectArtifactPaths(new File(childBuildPath), compileClasspathElements,
                                testClasspathElements);
                    } else {
                        // update artifacts on this project
                        Set<String> compileArtifacts = null;
                        Set<String> testArtifacts = null;
                        MavenProject project = null;
                        if (childBuildPath.equals(this.buildFile.getCanonicalPath())) {
                            compileArtifacts = util.getCompileArtifacts();
                            testArtifacts = util.getTestArtifacts();
                            project = getMavenProject(this.buildFile);
                        } else if (getProjectModule(new File(childBuildPath)) != null) {
                            ProjectModule projectModule = getProjectModule(new File(childBuildPath));
                            compileArtifacts = projectModule.getCompileArtifacts();
                            testArtifacts = projectModule.getTestArtifacts();
                            project = getMavenProject(projectModule.getBuildFile());
                        }
                        if (compileArtifacts != null && testArtifacts != null && project != null) {
                            compileArtifacts.clear();
                            testArtifacts.clear();
                            // TODO if a dependency is deleted from a parent project, it will still be in
                            // the child projects classpath elements
                            compileClasspathElements.addAll(project.getCompileClasspathElements());
                            testClasspathElements.addAll(project.getTestClasspathElements());
                            compileArtifacts.addAll(compileClasspathElements);
                            testArtifacts.addAll(testClasspathElements);
                        }
                    }
                }
            }
        }

        private MavenProject getMavenProject(File buildFile) throws ProjectBuildingException {
            ProjectBuildingResult build = mavenProjectBuilder.build(buildFile,
                    session.getProjectBuildingRequest().setResolveDependencies(true));
            return build.getProject();
        }

        @Override
        protected void updateLooseApp() throws PluginExecutionException {
        	// Only perform operations if we are a war type application
        	if (project.getPackaging().equals("war")) {
		    	// Check if we are using an exploded loose app
		    	if (LooseWarApplication.isExploded(project)) {
		    		if (!isExplodedLooseWarApp) {
		    			// The project was previously running with a "non-exploded" loose app.
		    			// Update this flag and redeploy as an exploded loose app.
		    			isExplodedLooseWarApp = true;
		    			
		    			// Validate maven-war-plugin version
		    			Plugin warPlugin = getPlugin("org.apache.maven.plugins", "maven-war-plugin");
		            	if (!validatePluginVersion(warPlugin.getVersion(), "3.3.1")) {
		            		log.warn("Exploded WAR functionality is enabled. Please use maven-war-plugin version 3.3.1 or greater for best results.");
		            	}
		            	
		    			redeployApp();
		    		} else {
		    			try {
		    				runExplodedMojo();
		    			} catch (MojoExecutionException e) {
		    				log.error("Failed to run war:exploded goal", e);
		    			}
		    		}
		    	} else {
		    		if (isExplodedLooseWarApp) {
		    			// Dev mode was previously running with an exploded loose war app. The app
		    			// must have been updated to remove any exploded war capabilities 
		    			// (filtering, overlay, etc). Update this flag and redeploy.
		    			isExplodedLooseWarApp = false;
		    			redeployApp();
		    		}
		    	}
        	}
        }

        @Override
        protected void resourceDirectoryCreated() throws IOException {
            if (project.getPackaging().equals("war") && LooseWarApplication.isExploded(project)) {
                try {
                    runMojo("org.apache.maven.plugins", "maven-resources-plugin", "resources");
                    runExplodedMojo();
                } catch (MojoExecutionException e) {
                    log.error("Failed to run goal(s)", e);
                }
            } 
        }

        @Override
        protected void resourceModifiedOrCreated(File fileChanged, File resourceParent, File outputDirectory) throws IOException {
        	if (project.getPackaging().equals("war") && LooseWarApplication.isExploded(project)) {
                try {
                    runMojo("org.apache.maven.plugins", "maven-resources-plugin", "resources");
                    runExplodedMojo();
                } catch (MojoExecutionException e) {
                    log.error("Failed to run goal(s)", e);
                }
            } else {
                copyFile(fileChanged, resourceParent, outputDirectory, null);
            }
        }

        @Override
        protected void resourceDeleted(File fileChanged, File resourceParent, File outputDirectory) throws IOException {
            deleteFile(fileChanged, resourceParent, outputDirectory, null);
            if (project.getPackaging().equals("war") && LooseWarApplication.isExploded(project)) {
                try {
                    runExplodedMojo();
                } catch (MojoExecutionException e) {
                    log.error("Failed to run goal(s)", e);
                }
            } 
        }

        @Override
        public boolean recompileBuildFile(File buildFile, Set<String> compileArtifactPaths,
                Set<String> testArtifactPaths, boolean generateFeatures, ThreadPoolExecutor executor) throws PluginExecutionException {
            // monitoring project pom.xml file changes in dev mode:
            // - liberty.* properties in project properties section
            // - changes in liberty plugin configuration in the build plugin section
            // - project dependencies changes
            boolean restartServer = false;
            boolean createServer = false;
            boolean installFeature = false;
            boolean redeployApp = false;
            boolean runBoostPackage = false;
            boolean optimizeGenerateFeatures = false;

            ProjectBuildingResult build;
            try {
                build = mavenProjectBuilder.build(buildFile,
                        session.getProjectBuildingRequest().setResolveDependencies(true));
            } catch (ProjectBuildingException e) {
                log.error("Could not parse pom.xml. " + e.getMessage());
                log.debug(e);
                return false;
            }

            // set the updated project in current session;
            Plugin backupLibertyPlugin = getLibertyPlugin();
            MavenProject backupProject = project;
            project = build.getProject();
            session.setCurrentProject(project);
            Plugin libertyPlugin = getLibertyPlugin();

            try {
                // TODO rebuild the corresponding module if the compiler options have changed
                JavaCompilerOptions oldCompilerOptions = getMavenCompilerOptions(backupProject);
                JavaCompilerOptions compilerOptions = getMavenCompilerOptions(project);
                if (!oldCompilerOptions.getOptions().equals(compilerOptions.getOptions())) {
                    log.debug("Maven compiler options have been modified: " + compilerOptions.getOptions());
                    util.updateJavaCompilerOptions(compilerOptions);
                }

                // Monitoring liberty properties in the pom.xml
                if (hasServerPropertyChanged(project, backupProject)) {
                    restartServer = true;
                }
                if (!restartServer && hasServerVariableChanged(project, backupProject)) {
                    createServer = true;
                }

                // monitoring Liberty plugin configuration changes in dev mode
                Xpp3Dom config;
                Xpp3Dom oldConfig;
                if (!restartServer) {
                    config = ExecuteMojoUtil.getPluginGoalConfig(libertyPlugin, "create", log);
                    oldConfig = ExecuteMojoUtil.getPluginGoalConfig(backupLibertyPlugin, "create", log);
                    if (!Objects.equals(config, oldConfig)) {
                        createServer = true;
                        if (restartForLibertyMojoConfigChanged(config, oldConfig)) {
                            restartServer = true;
                        }
                    }
                }
                config = ExecuteMojoUtil.getPluginGoalConfig(libertyPlugin, "install-feature", log);
                oldConfig = ExecuteMojoUtil.getPluginGoalConfig(backupLibertyPlugin, "install-feature", log);
                if (!Objects.equals(config, oldConfig)) {
                    installFeature = true;
                }
                config = ExecuteMojoUtil.getPluginGoalConfig(libertyPlugin, "deploy", log);
                oldConfig = ExecuteMojoUtil.getPluginGoalConfig(backupLibertyPlugin, "deploy", log);
                if (!Objects.equals(config, oldConfig)) {
                    redeployApp = true;
                }
                config = ExecuteMojoUtil.getPluginGoalConfig(libertyPlugin, "generate-features", log);
                oldConfig = ExecuteMojoUtil.getPluginGoalConfig(backupLibertyPlugin, "generate-features", log);
                if (!Objects.equals(config, oldConfig)) {
                    optimizeGenerateFeatures = true;
                }

                List<Dependency> deps = project.getDependencies();
                List<Dependency> oldDeps = backupProject.getDependencies();
                if (!dependencyListsEquals(oldDeps, deps)) {
                    runBoostPackage = true;
                    // detect esa dependency changes
                    if (!dependencyListsEquals(getEsaDependency(deps), getEsaDependency(oldDeps))) {
                        installFeature = true;
                    }

                    // detect compile dependency changes
                    if (!dependencyListsEquals(getCompileDependency(deps), getCompileDependency(oldDeps))) {
                        redeployApp = true;
                        optimizeGenerateFeatures = true;
                    }
                }
                // update classpath for dependencies changes
                if (this.parentBuildFiles.isEmpty()) {
                    compileArtifactPaths.clear();
                    testArtifactPaths.clear();
                } else {
                    // remove past artifacts and add the newest calculated (covers the case where a
                    // dependency was deleted)
                    // do not clear list as it may contain dependencies from parent projects
                    testArtifactPaths.removeAll(backupProject.getTestClasspathElements());
                    compileArtifactPaths.removeAll(backupProject.getCompileClasspathElements());
                }

                compileArtifactPaths.addAll(project.getCompileClasspathElements());
                testArtifactPaths.addAll(project.getTestClasspathElements());

                if (optimizeGenerateFeatures && generateFeatures) {
                    log.debug("Detected a change in the compile dependencies, regenerating features");
                    // always optimize generate features on dependency change
                    boolean generateFeaturesSuccess = libertyGenerateFeatures(null, true);
                    if (generateFeaturesSuccess) {
                        util.getJavaSourceClassPaths().clear();
                    } else {
                        installFeature = false; // skip installing features if generate features fails
                    }
                }
                if (restartServer) {
                    // - stop Server
                    // - create server or runBoostMojo
                    // - install feature
                    // - deploy app
                    // - start server
                    util.restartServer();
                    return true;
                } else {
                    if (isUsingBoost() && (createServer || runBoostPackage)) {
                        log.info("Running boost:package");
                        runBoostMojo("package");
                    } else if (createServer) {
                        runLibertyMojoCreate();
                    } else if (redeployApp) {
                    	runLibertyMojoDeploy();
                    }
                    if (installFeature) {
                        runLibertyMojoInstallFeature(null, super.getContainerName());
                    }
                }
                if (!(restartServer || createServer || redeployApp || installFeature || runBoostPackage)) {
                    // pom.xml is changed but not affecting liberty:dev mode. return true with the
                    // updated project set in the session
                    log.debug("changes in the pom.xml are not monitored by dev mode");
                    return true;
                }
            } catch (MojoExecutionException | ProjectBuildingException | DependencyResolutionRequiredException | IOException e) {
                log.error("An unexpected error occurred while processing changes in pom.xml. " + e.getMessage());
                log.debug(e);
                project = backupProject;
                session.setCurrentProject(backupProject);
                return false;
            }
            return true;
        }

        @Override
        public void installFeatures(File configFile, File serverDir) {
            try {
                ServerFeatureUtil servUtil = getServerFeatureUtil();
                Set<String> features = servUtil.getServerFeatures(serverDir, libertyDirPropertyFiles);
                if (features != null) {
                    Set<String> featuresCopy = new HashSet<String>(features);

                    if (existingFeatures != null) {
                        features.removeAll(existingFeatures);
                        // check if features have been removed
                        Set<String> existingFeaturesCopy = new HashSet<String>(existingFeatures);
                        existingFeaturesCopy.removeAll(featuresCopy);
                        if (!existingFeaturesCopy.isEmpty()) {
                            log.info("Configuration features have been removed");
                            existingFeatures.removeAll(existingFeaturesCopy);
                        }
                    }

                    // check if features have been added and install new features
                    if (!features.isEmpty()) {
                        log.info("Configuration features have been added");
                        Element[] featureElems = new Element[features.size() + 1];
                        featureElems[0] = element(name("acceptLicense"), "true");
                        String[] values = features.toArray(new String[features.size()]);
                        for (int i = 0; i < features.size(); i++) {
                            featureElems[i + 1] = element(name("feature"), values[i]);
                        }
                        runLibertyMojoInstallFeature(element(name("features"), featureElems), super.getContainerName());
                        existingFeatures.addAll(features);
                    }
                }
            } catch (MojoExecutionException e) {
                log.error("Failed to install features from configuration file", e);
            }
        }

        @Override
        public ServerFeatureUtil getServerFeatureUtilObj() {
            return getServerFeatureUtil();
        }

        @Override
        public Set<String> getExistingFeatures() {
            return this.existingFeatures;
        }

        @Override
        public boolean compile(File dir) {
            try {
                if (dir.equals(sourceDirectory)) {
                    runCompileMojoLogWarning();
                    runMojo("org.apache.maven.plugins", "maven-resources-plugin", "resources");
                }
                if (dir.equals(testSourceDirectory)) {
                    runTestCompileMojoLogWarning();
                    runMojo("org.apache.maven.plugins", "maven-resources-plugin", "testResources");
                }
                return true;
            } catch (MojoExecutionException e) {
                log.error("Unable to compile", e);
                return false;
            }
        }

        @Override
        public boolean compile(File dir, ProjectModule project) {
            MavenProject mavenProject = resolveMavenProject(project.getBuildFile());
            try {
                if (dir.equals(project.getSourceDirectory())) {
                    runCompileMojoLogWarning(mavenProject);
                    runMojoForProject("org.apache.maven.plugins", "maven-resources-plugin", "resources", mavenProject);
                }
                if (dir.equals(project.getTestSourceDirectory())) {
                    runTestCompileMojoLogWarning(mavenProject);
                    runMojoForProject("org.apache.maven.plugins", "maven-resources-plugin", "testResources", mavenProject);
                }
                return true;
            } catch (MojoExecutionException e) {
                log.error("Unable to compile", e);
                return false;
            }
        }

        @Override
        public void runUnitTests(File buildFile) throws PluginExecutionException, PluginScenarioException {
            MavenProject currentProject = resolveMavenProject(buildFile);
            try {
                runTestMojo("org.apache.maven.plugins", "maven-surefire-plugin", "test", currentProject);
                runTestMojo("org.apache.maven.plugins", "maven-surefire-report-plugin", "report-only", currentProject);
            } catch (MojoExecutionException e) {
                Throwable cause = e.getCause();
                if (cause != null && cause instanceof MojoFailureException) {
                    throw new PluginScenarioException("Unit tests failed: " + cause.getLocalizedMessage(), e);
                } else {
                    throw new PluginExecutionException("Failed to run unit tests", e);
                }
            }
        }

        @Override
        public void runIntegrationTests(File buildFile) throws PluginExecutionException, PluginScenarioException {
            MavenProject currentProject = resolveMavenProject(buildFile);
            try {
                runTestMojo("org.apache.maven.plugins", "maven-failsafe-plugin", "integration-test", currentProject);
                runTestMojo("org.apache.maven.plugins", "maven-surefire-report-plugin", "failsafe-report-only", currentProject);
                runTestMojo("org.apache.maven.plugins", "maven-failsafe-plugin", "verify", currentProject);
            } catch (MojoExecutionException e) {
                Throwable cause = e.getCause();
                if (cause != null && cause instanceof MojoFailureException) {
                    throw new PluginScenarioException("Integration tests failed: " + cause.getLocalizedMessage(), e);
                } else {
                    throw new PluginExecutionException("Failed to run integration tests", e);
                }
            }
        }

        @Override
        public void redeployApp() throws PluginExecutionException {
            try {
                runLibertyMojoDeploy();
            } catch (MojoExecutionException e) {
                throw new PluginExecutionException("liberty:deploy goal failed:" + e.getMessage());
            }
        }

        @Override
        public boolean isLooseApplication() {
            // dev mode forces deploy with looseApplication=true, but it only takes effect
            // if packaging is one of the supported loose app types
            return DeployMojoSupport.isSupportedLooseAppType(project.getPackaging());
        }

        @Override
        public File getLooseApplicationFile() {
            return getLooseAppConfigFile(project, container);
        }

    }

    private boolean isUsingBoost() {
        return boostPlugin != null;
    }

    @Override
    protected void doExecute() throws Exception {
        if (skip) {
            getLog().info("\nSkipping dev goal.\n");
            return;
        }

        String mvnVersion = runtime.getMavenVersion();
        log.debug("Maven version: " + mvnVersion);
        // Maven 3.8.2 and 3.8.3 contain a bug where compile artifacts are not resolved
        // correctly in threads. Block dev mode from running on these versions
        if (mvnVersion.equals("3.8.2") || mvnVersion.equals("3.8.3")) {
            throw new PluginExecutionException("Detected Maven version " + mvnVersion
                    + ". This version is not supported for dev mode. Upgrade to Maven 3.8.4 or higher to use dev mode.");
        }

        boolean isEar = false;
        if (project.getPackaging().equals("ear")) {
            isEar = true;
        }

        // If there are downstream projects (e.g. other modules depend on this module in the Maven Reactor build order),
        // then skip dev mode on this module but only run compile.
        List<MavenProject> upstreamMavenProjects = new ArrayList<MavenProject>();
        ProjectDependencyGraph graph = session.getProjectDependencyGraph();
        if (graph != null) {
            checkMultiModuleConflicts(graph);

            List<MavenProject> downstreamProjects = graph.getDownstreamProjects(project, true);

            if (!downstreamProjects.isEmpty()) {
                log.debug("Downstream projects: " + downstreamProjects);
                if (isEar) {
                    runMojo("org.apache.maven.plugins", "maven-ear-plugin", "generate-application-xml");
                    runMojo("org.apache.maven.plugins", "maven-resources-plugin", "resources");

                    installEmptyEarIfNotFound(project);
                } else if (project.getPackaging().equals("pom")) {
                    log.debug("Skipping compile/resources on module with pom packaging type");
                } else {
                    purgeLocalRepositoryArtifact();

                    runMojo("org.apache.maven.plugins", "maven-resources-plugin", "resources");
                    runCompileMojoLogWarning();
                }
                return;
            } else {
                // get all upstream projects
                upstreamMavenProjects.addAll(graph.getUpstreamProjects(project, true));
            }

            if (containsPreviousLibertyModule(graph)) {
                // skip this module
                return;
            }
        }

        // get all parent poms
        Map<String, List<String>> parentPoms = new HashMap<String, List<String>>();
        for (MavenProject proj : graph.getAllProjects()) {
            updateParentPoms(parentPoms, proj);
        }

        // default behavior of recompileDependencies
        if (recompileDependencies == null) {
            if (upstreamMavenProjects.isEmpty()) {
                // single module project default to false
                log.debug(
                        "The recompileDependencies parameter was not explicitly set. The default value -DrecompileDependencies=false will be used.");
                recompileDependencies = "false";
            } else {
                // multi module project default to true
                log.debug(
                        "The recompileDependencies parameter was not explicitly set. The default value for multi module projects -DrecompileDependencies=true will be used.");
                recompileDependencies = "true";
            }
        }
        boolean recompileDeps = Boolean.parseBoolean(recompileDependencies);
        if (recompileDeps) {
            if (!upstreamMavenProjects.isEmpty()) {
                log.info("The recompileDependencies parameter is set to \"true\". On a file change all dependent modules will be recompiled.");
            } else {
                log.info("The recompileDependencies parameter is set to \"true\". On a file change the entire project will be recompiled.");
            }
        } else {
            log.info("The recompileDependencies parameter is set to \"false\". On a file change only the affected classes will be recompiled.");
        }

        // Check if this is a Boost application
        boostPlugin = project.getPlugin("org.microshed.boost:boost-maven-plugin");

        processContainerParams();

        if (!container) {
            if (serverDirectory.exists()) {
                if (ServerStatusUtil.isServerRunning(installDirectory, super.outputDirectory, serverName)) {
                    throw new MojoExecutionException("The server " + serverName
                            + " is already running. Terminate all instances of the server before starting dev mode."
                            + " You can stop a server instance with the command 'mvn liberty:stop'.");
                }
            }
        } // else TODO check if the container is already running?

        // create an executor for tests with an additional queue of size 1, so
        // any further changes detected mid-test will be in the following run
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(1, true));

        if (isEar) {
            runMojo("org.apache.maven.plugins", "maven-ear-plugin", "generate-application-xml");
            runMojo("org.apache.maven.plugins", "maven-resources-plugin", "resources");
        } else if (project.getPackaging().equals("pom")) {
            log.debug("Skipping compile/resources on module with pom packaging type");
        } else {
            runMojo("org.apache.maven.plugins", "maven-resources-plugin", "resources");
            runCompileMojoLogWarning();
            runMojo("org.apache.maven.plugins", "maven-resources-plugin", "testResources");    
            runTestCompileMojoLogWarning();
        }

        sourceDirectory = new File(sourceDirectoryString.trim());
        testSourceDirectory = new File(testSourceDirectoryString.trim());

        ArrayList<File> javaFiles = new ArrayList<File>();
        listFiles(sourceDirectory, javaFiles, ".java");

        ArrayList<File> javaTestFiles = new ArrayList<File>();
        listFiles(testSourceDirectory, javaTestFiles, ".java");

        log.debug("Source directory: " + sourceDirectory);
        log.debug("Output directory: " + outputDirectory);
        log.debug("Test Source directory: " + testSourceDirectory);
        log.debug("Test Output directory: " + testOutputDirectory);

        if (isUsingBoost()) {
            log.info("Running boost:package");
            runBoostMojo("package");
        } else {
            if (generateFeatures) {
                // generate features on startup - provide all classes and only user specified
                // features to binary scanner
                try {
                    runLibertyMojoGenerateFeatures(null, true);
                } catch (MojoExecutionException e) {
                    if (e.getCause() != null && e.getCause() instanceof PluginExecutionException) {
                        // PluginExecutionException indicates that the binary scanner jar could not be found
                        log.error(e.getMessage() + ".\nDisabling the automatic generation of features.");
                        generateFeatures = false;
                    } else {
                        throw new MojoExecutionException(e.getMessage()
                        + " To disable the automatic generation of features, start dev mode with -DgenerateFeatures=false.",
                        e);
                    }
                }
            }
            runLibertyMojoCreate();
            // If non-container, install features before starting server. Otherwise, user
            // should have "RUN features.sh" in their Dockerfile if they want features to be
            // installed.
            if (!container) {
                runLibertyMojoInstallFeature(null, null);
            }
            runLibertyMojoDeploy();
        }
        
        if (project.getPackaging().equals("war")) {
            // Check if we are using the exploded loose app functionality and save for checking later on. 
            isExplodedLooseWarApp = LooseWarApplication.isExploded(project);
        
            // Validate maven-war-plugin version
            if (isExplodedLooseWarApp) {
        	    Plugin warPlugin = getPlugin("org.apache.maven.plugins", "maven-war-plugin");
        	    if (!validatePluginVersion(warPlugin.getVersion(), "3.3.2")) {
        		    log.warn("Exploded WAR functionality is enabled. Please use maven-war-plugin version 3.3.2 or greater for best results.");
        	    }
            }
        }
        
        // resource directories
        List<File> resourceDirs = getResourceDirectories(project, outputDirectory);
        
        List<Path> webResourceDirs = LooseWarApplication.getFilteredWebSourceDirectories(project);

        JavaCompilerOptions compilerOptions = getMavenCompilerOptions(project);

        // collect upstream projects
        List<ProjectModule> upstreamProjects = new ArrayList<ProjectModule>();
        if (!upstreamMavenProjects.isEmpty()) {
            for (MavenProject p : upstreamMavenProjects) {
                // get compiler options for upstream project
                JavaCompilerOptions upstreamCompilerOptions = getMavenCompilerOptions(p);

                Set<String> compileArtifacts = new HashSet<String>();
                Set<String> testArtifacts = new HashSet<String>();
                Build build = p.getBuild();
                File upstreamSourceDir = new File(build.getSourceDirectory());
                File upstreamOutputDir = new File(build.getOutputDirectory());
                File upstreamTestSourceDir = new File(build.getTestSourceDirectory());
                File upstreamTestOutputDir = new File(build.getTestOutputDirectory());
                // resource directories
                List<File> upstreamResourceDirs = getResourceDirectories(p, upstreamOutputDir);

                // properties that are set in the pom file
                Properties props = p.getProperties();

                // properties that are set by user via CLI parameters
                Properties userProps = session.getUserProperties();

                Plugin libertyPlugin = getLibertyPluginForProject(p);
                // use "dev" goal, although we don't expect the skip tests flags to be bound to any goal
                Xpp3Dom config = ExecuteMojoUtil.getPluginGoalConfig(libertyPlugin, "dev", log);
                
                boolean upstreamSkipTests = getBooleanFlag(config, userProps, props, "skipTests");
                boolean upstreamSkipITs = getBooleanFlag(config, userProps, props, "skipITs");
                boolean upstreamSkipUTs = getBooleanFlag(config, userProps, props, "skipUTs");

                // only force skipping unit test for ear modules otherwise honour existing skip
                // test params
                if (p.getPackaging().equals("ear")) {
                    upstreamSkipUTs = true;
                }

                // build list of dependent modules
                List<MavenProject> dependentProjects = graph.getDownstreamProjects(p, true);
                List<File> dependentModules = new ArrayList<File>();
                for (MavenProject depProj : dependentProjects) {
                    dependentModules.add(depProj.getFile());
                }

                ProjectModule upstreamProject = new ProjectModule(p.getFile(), p.getArtifactId(), p.getPackaging(),
                        compileArtifacts, testArtifacts, upstreamSourceDir, upstreamOutputDir, upstreamTestSourceDir,
                        upstreamTestOutputDir, upstreamResourceDirs, upstreamSkipTests, upstreamSkipUTs,
                        upstreamSkipITs, upstreamCompilerOptions, dependentModules);

                upstreamProjects.add(upstreamProject);
            }
        }

        // skip unit tests for ear applications
        if (isEar) {
            skipUTs = true;
        }

        // pom.xml
        File pom = project.getFile();

        // collect artifacts canonical paths in order to build classpath
        Set<String> compileArtifactPaths = new HashSet<String>(project.getCompileClasspathElements());
        Set<String> testArtifactPaths = new HashSet<String>(project.getTestClasspathElements());

        util = new DevMojoUtil(installDirectory, userDirectory, serverDirectory, sourceDirectory, testSourceDirectory,
                configDirectory, project.getBasedir(), multiModuleProjectDirectory, resourceDirs, compilerOptions,
                settings.getLocalRepository(), upstreamProjects, upstreamMavenProjects, recompileDeps, pom, parentPoms, 
                generateFeatures, compileArtifactPaths, testArtifactPaths, webResourceDirs);
        util.addShutdownHook(executor);
        util.startServer();

        // start watching for keypresses immediately
        util.runHotkeyReaderThread(executor);

        // Note that serverXmlFile can be null. DevUtil will automatically watch
        // all files in the configDirectory,
        // which is where the server.xml is located if a specific serverXmlFile
        // configuration parameter is not specified.
        try {
            util.watchFiles(outputDirectory, testOutputDirectory, executor, serverXmlFile, bootstrapPropertiesFile,
                    jvmOptionsFile);
        } catch (PluginScenarioException e) {
            if (e.getMessage() != null) {
                // a proper message is included in the exception if the server has been stopped
                // by another process
                log.info(e.getMessage());
            }
            return; // enter shutdown hook
        }
    }

    /**
     * Use the following priority ordering for skip test flags: <br>
     * 1. within Liberty Maven plugin’s configuration in a module <br>
     * 2. within Liberty Maven plugin’s configuration in a parent pom’s pluginManagement <br>
     * 3. by command line with -D <br>
     * 4. within a module’s properties <br>
     * 5. within a parent pom’s properties <br>
     * 
     * @param config the config xml that might contain the param as an attribute
     * @param userProps the Maven user properties
     * @param props the Maven project's properties
     * @param param the Boolean parameter to look for
     * @return a boolean in priority order, or null if the param was not found anywhere
     */
    public static boolean getBooleanFlag(Xpp3Dom config, Properties userProps, Properties props, String param) {
        // this handles 1 and 2
        Boolean pluginConfig = parseBooleanIfDefined(getConfigValue(config, param));
        
        // this handles 3
        Boolean userProp = parseBooleanIfDefined(userProps.getProperty(param));
        
        // this handles 4 and 5
        Boolean prop = parseBooleanIfDefined(props.getProperty(param));
        
        return getFirstNonNullValue(pluginConfig, userProp, prop);
    }

    /**
     * Gets the value of the given attribute, or null if the attribute is not found.
     * @param config
     * @param attribute
     * @return
     */
    private static String getConfigValue(Xpp3Dom config, String attribute) {
        return (config.getChild(attribute) == null ? null : config.getChild(attribute).getValue());
    }

    /**
     * Parses a Boolean from a String if the String is not null.  Otherwise returns null.
     * @param value the String to parse
     * @return a Boolean, or null if value is null
     */
    private static Boolean parseBooleanIfDefined(String value) {
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return null;
    }

    /**
     * Gets the value of the first Boolean object that is not null, in order from lowest to highest index.
     * @param booleans an array of Boolean objects, some of which may be null
     * @return the value of the first non-null Boolean, or false if everything is null
     */
    public static boolean getFirstNonNullValue(Boolean... booleans) {
        for (Boolean b : booleans) {
            if (b != null) {
                return b.booleanValue();
            }
        }
        return false;
    }

    /**
     * Update map with list of parent poms and their subsequent child poms
     * 
     * @param parentPoms Map of parent poms and subsequent child poms
     * @param proj       MavenProject
     */
    private void updateParentPoms(Map<String, List<String>> parentPoms, MavenProject proj) {
        MavenProject parentProject = proj.getParent();
        try {
            if (parentProject != null) {
                // append to existing list
                if (parentProject.getFile() != null) {
                    List<String> childPoms = parentPoms.get(parentProject.getFile().getCanonicalPath());
                    if (childPoms == null) {
                        childPoms = new ArrayList<String>();
                        childPoms.add(proj.getFile().getCanonicalPath());
                        parentPoms.put(parentProject.getFile().getCanonicalPath(), childPoms);
                    } else {
                        if (!childPoms.contains(proj.getFile().getCanonicalPath())) {
                            childPoms.add(proj.getFile().getCanonicalPath());
                        }
                    }
                    if (parentProject.getParent() != null) {
                        // recursively search for top most parent project
                        updateParentPoms(parentPoms, parentProject);
                    }
                }
            }
        } catch (IOException e) {
            log.error("An unexpected error occurred when trying to resolve " + proj.getFile() + ": " + e.getMessage());
            log.debug(e);
        }
    }

    private JavaCompilerOptions getMavenCompilerOptions(MavenProject currentProject) {
        Plugin plugin = getPluginForProject("org.apache.maven.plugins", "maven-compiler-plugin", currentProject);
        Xpp3Dom configuration = ExecuteMojoUtil.getPluginGoalConfig(plugin, "compile", log);
        JavaCompilerOptions compilerOptions = new JavaCompilerOptions();

        String showWarnings = getCompilerOption(configuration, "showWarnings", "maven.compiler.showWarnings", currentProject);
        if (showWarnings != null) {
            boolean showWarningsBoolean = Boolean.parseBoolean(showWarnings);
            log.debug("Setting showWarnings to " + showWarningsBoolean);
            compilerOptions.setShowWarnings(showWarningsBoolean);
        }

        String source = getCompilerOption(configuration, "source", "maven.compiler.source", currentProject);
        if (source != null) {
            log.debug("Setting compiler source to " + source);
            compilerOptions.setSource(source);
        }

        String target = getCompilerOption(configuration, "target", "maven.compiler.target", currentProject);
        if (target != null) {
            log.debug("Setting compiler target to " + target);
            compilerOptions.setTarget(target);
        }

        String release = getCompilerOption(configuration, "release", "maven.compiler.release", currentProject);
        if (release != null) {
            log.debug("Setting compiler release to " + release);
            compilerOptions.setRelease(release);
        }

        return compilerOptions;
    }

    /**
     * Gets a compiler option's value from CLI parameters, maven-compiler-plugin's
     * configuration or project properties.
     * 
     * @param configuration       The maven-compiler-plugin's configuration from
     *                            pom.xml
     * @param mavenParameterName  The maven-compiler-plugin parameter name to look
     *                            for
     * @param projectPropertyName The project property name to look for, if the
     *                            mavenParameterName's parameter could not be found
     *                            in the plugin configuration.
     * @param currentProject      The current Maven Project
     * @return The compiler option
     */
    private String getCompilerOption(Xpp3Dom configuration, String mavenParameterName, String projectPropertyName,
            MavenProject currentProject) {
        String option = null;
        // CLI parameter takes precedence over plugin configuration
        option = session.getUserProperties().getProperty(projectPropertyName);

        // Plugin configuration takes precedence over project property
        if (option == null && configuration != null) {
            Xpp3Dom child = configuration.getChild(mavenParameterName);
            if (child != null) {
                option = child.getValue();
            }
        }
        if (option == null) {
            option = currentProject.getProperties().getProperty(projectPropertyName);
        }
        return option;
    }

    private void processContainerParams() throws MojoExecutionException {
        if (container) {
            // this also sets the project property for use in DeployMojoSupport
            setContainer(true);
        }
    }

    private MavenProject resolveMavenProject(File buildFile) {
        ProjectBuildingResult build;
        MavenProject currentProject = project; // default to main project
        try {
            if (buildFile != null && !project.getFile().getCanonicalPath().equals(buildFile.getCanonicalPath())) {
                build = mavenProjectBuilder.build(buildFile,
                        session.getProjectBuildingRequest().setResolveDependencies(true));
                // if we can resolve the project associated with build file, run tests on
                // corresponding project
                if (build.getProject() != null) {
                    currentProject = build.getProject();
                }
            }
        } catch (ProjectBuildingException | IOException e) {
            log.error("An unexpected error occurred when trying to run integration tests for "
                    + buildFile.getAbsolutePath() + ": " + e.getMessage());
            log.debug(e);
        }
        return currentProject;
    }

    private void runTestMojo(String groupId, String artifactId, String goal, MavenProject project)
            throws MojoExecutionException {
        Plugin plugin = getPluginForProject(groupId, artifactId, project);
        Xpp3Dom config = ExecuteMojoUtil.getPluginGoalConfig(plugin, goal, log);

        // check if this is a project module or main module
        if (util.isMultiModuleProject()) {
            try {
                Set<String> testArtifacts;
                ProjectModule projectModule = util.getProjectModule(project.getFile());
                if (projectModule != null) {
                    testArtifacts = projectModule.getTestArtifacts();
                } else {
                    // assume this is the main module
                    testArtifacts = util.getTestArtifacts();
                }
                if (goal.equals("test") || goal.equals("integration-test")) {
                    injectClasspathElements(config, testArtifacts, project.getTestClasspathElements());
                }
            } catch (IOException | DependencyResolutionRequiredException e) {
                log.error(
                        "Unable to resolve test artifact paths for " + project.getFile() + ". Restart dev mode to ensure classpaths are properly resolved.");
                log.debug(e);
            }
        }

        if (goal.equals("test")) {
            injectTestId(config);
        } else if (goal.equals("integration-test")) {
            injectTestId(config);
            injectLibertyProperties(config);

            // clean up previous summary file
            File summaryFile = null;
            Xpp3Dom summaryFileElement = config.getChild("summaryFile");
            if (summaryFileElement != null && summaryFileElement.getValue() != null) {
                summaryFile = new File(summaryFileElement.getValue());
            } else {
                summaryFile = new File(project.getBuild().getDirectory(), "failsafe-reports/failsafe-summary.xml");
            }
            try {
                log.debug("Looking for summary file at " + summaryFile.getCanonicalPath());
            } catch (IOException e) {
                log.debug("Unable to resolve summary file " + e.getMessage());
            }
            if (summaryFile.exists()) {
                boolean deleteResult = summaryFile.delete();
                log.debug("Summary file deleted? " + deleteResult);
            } else {
                log.debug("Summary file doesn't exist");
            }
        } else if (goal.equals("failsafe-report-only")) {
            Plugin failsafePlugin = getPluginForProject("org.apache.maven.plugins", "maven-failsafe-plugin", project);
            Xpp3Dom failsafeConfig = ExecuteMojoUtil.getPluginGoalConfig(failsafePlugin, "integration-test", log);
            Xpp3Dom linkXRef = new Xpp3Dom("linkXRef");
            if (failsafeConfig != null) {
                Xpp3Dom reportsDirectoryElement = failsafeConfig.getChild("reportsDirectory");
                if (reportsDirectoryElement != null) {
                    Xpp3Dom reportDirectories = new Xpp3Dom("reportsDirectories");
                    reportDirectories.addChild(reportsDirectoryElement);
                    config.addChild(reportDirectories);
                }
                linkXRef = failsafeConfig.getChild("linkXRef");
                if (linkXRef == null) {
                    linkXRef = new Xpp3Dom("linkXRef");
                }
            }
            linkXRef.setValue("false");
            config.addChild(linkXRef);
        } else if (goal.equals("report-only")) {
            Plugin surefirePlugin = getPluginForProject("org.apache.maven.plugins", "maven-surefire-plugin", project);
            Xpp3Dom surefireConfig = ExecuteMojoUtil.getPluginGoalConfig(surefirePlugin, "test", log);
            Xpp3Dom linkXRef = new Xpp3Dom("linkXRef");
            if (surefireConfig != null) {
                Xpp3Dom reportsDirectoryElement = surefireConfig.getChild("reportsDirectory");
                if (reportsDirectoryElement != null) {
                    Xpp3Dom reportDirectories = new Xpp3Dom("reportsDirectories");
                    reportDirectories.addChild(reportsDirectoryElement);
                    config.addChild(reportDirectories);
                }
                linkXRef = surefireConfig.getChild("linkXRef");
                if (linkXRef == null) {
                    linkXRef = new Xpp3Dom("linkXRef");
                }
            }
            linkXRef.setValue("false");
            config.addChild(linkXRef);
        }

        log.debug("POM file: " + project.getFile() + "\n" + groupId + ":" + artifactId + " " + goal
                + " configuration:\n" + config);
        MavenSession tempSession = session.clone();
        tempSession.setCurrentProject(project);
        executeMojo(plugin, goal(goal), config, executionEnvironment(project, tempSession, pluginManager));
    }

    /**
     * Inject missing test artifacts (usually from upstream modules) for Maven
     * surefire and failsafe plugins
     * 
     * @param config                The configuration element
     * @param testArtifacts         The complete list of test artifacts resolved
     *                              from the build file and upstream build files
     * @param testClasspathElements The list of test artifacts resolved from the
     *                              build file
     */
    private void injectClasspathElements(Xpp3Dom config, Set<String> testArtifacts,
            List<String> testClasspathElements) {
        if (testArtifacts.size() > testClasspathElements.size()) {
            List<String> additionalClassPathElements = new ArrayList<String>();
            additionalClassPathElements.addAll(testArtifacts);
            additionalClassPathElements.removeAll(testClasspathElements);
            Xpp3Dom classpathElement = config.getChild("additionalClasspathElements");
            if (classpathElement == null) {
                classpathElement = new Xpp3Dom("additionalClasspathElements");
            }
            for (String element : additionalClassPathElements) {
                Xpp3Dom childElem = new Xpp3Dom("additionalClasspathElement");
                childElem.setValue(element);
                classpathElement.addChild(childElem);
            }
            config.addChild(classpathElement);
        }
    }

    /**
     * Force change a property so that the checksum calculated by
     * AbstractSurefireMojo is different every time.
     *
     * @param config The configuration element
     */
    private void injectTestId(Xpp3Dom config) {
        Xpp3Dom properties = config.getChild("properties");
        if (properties == null || properties.getChild(TEST_RUN_ID_PROPERTY_NAME) == null) {
            Element e = element(name("properties"), element(name(TEST_RUN_ID_PROPERTY_NAME), String.valueOf(runId++)));
            config.addChild(e.toDom());
        } else {
            properties.getChild(TEST_RUN_ID_PROPERTY_NAME).setValue(String.valueOf(runId++));
        }
    }

    /**
     * Add Liberty system properties for tests to consume.
     *
     * @param config The configuration element
     * @throws MojoExecutionException if the userDirectory canonical path cannot be
     *                                resolved
     */
    private void injectLibertyProperties(Xpp3Dom config) throws MojoExecutionException {
        Xpp3Dom sysProps = config.getChild("systemPropertyVariables");
        if (sysProps == null) {
            Element e = element(name("systemPropertyVariables"));
            sysProps = e.toDom();
            config.addChild(sysProps);
        }
        // don't overwrite existing properties if they are already defined
        addDomPropertyIfNotFound(sysProps, LIBERTY_HOSTNAME, util.getHostName());
        addDomPropertyIfNotFound(sysProps, LIBERTY_HTTP_PORT, util.getHttpPort());
        addDomPropertyIfNotFound(sysProps, LIBERTY_HTTPS_PORT, util.getHttpsPort());
        addDomPropertyIfNotFound(sysProps, MICROSHED_HOSTNAME, util.getHostName());
        addDomPropertyIfNotFound(sysProps, MICROSHED_HTTP_PORT, util.getHttpPort());
        addDomPropertyIfNotFound(sysProps, MICROSHED_HTTPS_PORT, util.getHttpsPort());
        try {
            addDomPropertyIfNotFound(sysProps, WLP_USER_DIR_PROPERTY_NAME, userDirectory.getCanonicalPath());
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Could not resolve canonical path of userDirectory parameter: " + userDirectory.getAbsolutePath(),
                    e);
        }
    }

    private void addDomPropertyIfNotFound(Xpp3Dom sysProps, String key, String value) {
        if (sysProps.getChild(key) == null && value != null) {
            sysProps.addChild(element(name(key), value).toDom());
        }
    }

    private void runBoostMojo(String goal) throws MojoExecutionException, ProjectBuildingException {

        MavenProject boostProject = this.project;
        MavenSession boostSession = this.session;

        log.debug("plugin version: " + boostPlugin.getVersion());
        executeMojo(boostPlugin, goal(goal), configuration(),
                executionEnvironment(boostProject, boostSession, pluginManager));

    }

    private void listFiles(File directory, List<File> files, String suffix) {
        if (directory != null) {
            // Get all files from a directory.
            File[] fList = directory.listFiles();
            if (fList != null) {
                for (File file : fList) {
                    if (file.isFile() && ((suffix == null) || (file.getName().toLowerCase().endsWith("." + suffix)))) {
                        files.add(file);
                    } else if (file.isDirectory()) {
                        listFiles(file, files, suffix);
                    }
                }
            }
        }
    }

    /**
     * Executes Maven goal passed but sets failOnError to false All errors are
     * logged as warning messages
     * 
     * @param goal         Maven compile goal
     * @param MavenProject Maven project to run compile goal against, null if
     *                     default project is to be used
     * @throws MojoExecutionException
     */
    private void runCompileMojo(String goal, MavenProject mavenProject) throws MojoExecutionException {
        Plugin plugin = getPluginForProject("org.apache.maven.plugins", "maven-compiler-plugin", mavenProject);
        MavenSession tempSession = session.clone();
        tempSession.setCurrentProject(mavenProject);
        MavenProject tempProject = mavenProject;
        Xpp3Dom config = ExecuteMojoUtil.getPluginGoalConfig(plugin, goal, log);
        config = Xpp3Dom.mergeXpp3Dom(configuration(element(name("failOnError"), "false")), config);
        log.info("Running maven-compiler-plugin:" + goal + " on " + tempProject.getFile());
        log.debug("configuration:\n" + config);
        executeMojo(plugin, goal(goal), config, executionEnvironment(tempProject, tempSession, pluginManager));
    }

    /**
     * Executes maven:compile but logs errors as warning messages
     * 
     * @throws MojoExecutionException
     */
    private void runCompileMojoLogWarning() throws MojoExecutionException {
        runCompileMojo("compile", project);
    }

    /**
     * Executes maven:compile but logs errors as warning messages
     * 
     * @throws MojoExecutionException
     */
    private void runCompileMojoLogWarning(MavenProject mavenProject) throws MojoExecutionException {
        runCompileMojo("compile", mavenProject);
    }

    /**
     * Executes maven:testCompile but logs errors as warning messages
     * 
     * @throws MojoExecutionException
     */
    private void runTestCompileMojoLogWarning() throws MojoExecutionException {
        runCompileMojo("testCompile", project);
    }

    /**
     * Executes maven:testCompile but logs errors as warning messages
     * 
     * @throws MojoExecutionException
     */
    private void runTestCompileMojoLogWarning(MavenProject mavenProject) throws MojoExecutionException {
        runCompileMojo("testCompile", mavenProject);
    }

    /**
     * Executes liberty:install-feature unless using Liberty in a container
     * 
     * @throws MojoExecutionException
     */
    @Override
    protected void runLibertyMojoInstallFeature(Element features, String containerName) throws MojoExecutionException {
        super.runLibertyMojoInstallFeature(features, containerName);
    }

    /**
     * Executes liberty:create unless using a container, then just create the
     * necessary server directories
     * 
     * @throws MojoExecutionException
     */
    @Override
    protected void runLibertyMojoCreate() throws MojoExecutionException {
        if (container) {
            log.debug("runLibertyMojoCreate check for installDirectory and serverDirectory");
            if (!installDirectory.isDirectory()) {
                installDirectory.mkdirs();
            }
            if (!serverDirectory.isDirectory()) {
                serverDirectory.mkdirs();
            }
        } else {
            super.runLibertyMojoCreate();
        }
    }

    /**
     * Executes liberty:generate-features.
     * 
     * @throws MojoExecutionException
     */
    @Override
    protected void runLibertyMojoGenerateFeatures(Element classFiles, boolean optimize) throws MojoExecutionException {
        super.runLibertyMojoGenerateFeatures(classFiles, optimize);
    }
}
