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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
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
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.twdata.maven.mojoexecutor.MojoExecutor.Element;

import io.openliberty.tools.ant.ServerTask;
import io.openliberty.tools.common.plugins.util.DevUtil;
import io.openliberty.tools.common.plugins.util.JavaCompilerOptions;
import io.openliberty.tools.common.plugins.util.PluginExecutionException;
import io.openliberty.tools.common.plugins.util.PluginScenarioException;
import io.openliberty.tools.common.plugins.util.ServerFeatureUtil;
import io.openliberty.tools.common.plugins.util.ServerStatusUtil;
import io.openliberty.tools.common.plugins.util.UpstreamProject;
import io.openliberty.tools.maven.BasicSupport;
import io.openliberty.tools.maven.applications.DeployMojoSupport;
import io.openliberty.tools.maven.utils.ExecuteMojoUtil;

/**
 * Start a liberty server in dev mode import to set ResolutionScope for TEST as
 * it helps build full transitive dependency classpath
 */
@Mojo(name = "dev", requiresDependencyCollection = ResolutionScope.TEST, requiresDependencyResolution = ResolutionScope.TEST)
public class DevMojo extends StartDebugMojoSupport {

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
        // resource directories
        List<File> resourceDirs = new ArrayList<File>();
        if (outputDir.exists()) {
            List<Resource> resources = project.getResources();
            for (Resource resource : resources) {
                File resourceFile = new File(resource.getDirectory());
                if (resourceFile.exists()) {
                    resourceDirs.add(resourceFile);
                }
            }
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
                List<UpstreamProject> upstreamProjects, List<MavenProject> upstreamMavenProjects) throws IOException {
            super(new File(project.getBuild().getDirectory()), serverDirectory, sourceDirectory, testSourceDirectory,
                    configDirectory, projectDirectory, multiModuleProjectDirectory, resourceDirs, hotTests, skipTests,
                    skipUTs, skipITs, project.getArtifactId(), serverStartTimeout, verifyTimeout, verifyTimeout,
                    ((long) (compileWait * 1000L)), libertyDebug, false, false, pollingTest, container, dockerfile,
                    dockerBuildContext, dockerRunOpts, dockerBuildTimeout, skipDefaultPorts, compilerOptions,
                    keepTempDockerfile, mavenCacheLocation, upstreamProjects);

            ServerFeature servUtil = getServerFeatureUtil();
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
        private List<Dependency> getCompileDependency(List<Dependency> dependencies) {
            List<Dependency> deps = new ArrayList<Dependency>();
            if (dependencies != null) {
                for (Dependency d : dependencies) {
                    if ("compile".equals(d.getScope())) {
                        deps.add(d);
                    }
                }
            }
            return deps;
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
        public boolean updateArtifactPaths(File buildFile, List<String> compileArtifactPaths,
                List<String> testArtifactPaths, boolean redeployCheck, ThreadPoolExecutor executor)
                throws PluginExecutionException {
            ProjectBuildingResult build;
            try {
                build = mavenProjectBuilder.build(buildFile,
                        session.getProjectBuildingRequest().setResolveDependencies(true));
                MavenProject upstreamProject = build.getProject();
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
                    util.getUpstreamProject(buildFile).setCompilerOptions(compilerOptions);
                }

                testArtifactPaths.clear();
                testArtifactPaths.addAll(upstreamProject.getTestClasspathElements());
                compileArtifactPaths.clear();
                compileArtifactPaths.addAll(upstreamProject.getCompileClasspathElements());

                // check if compile dependencies have changed and redeploy if they have
                if (redeployCheck) {
                    // update upstream Maven projects list
                    int index = upstreamMavenProjects.indexOf(backupUpstreamProject);
                    upstreamMavenProjects.set(index, upstreamProject);

                    List<Dependency> deps = upstreamProject.getDependencies();
                    List<Dependency> oldDeps = backupUpstreamProject.getDependencies();
                    if (!deps.equals(oldDeps)) {
                        // detect compile dependency changes
                        if (!getCompileDependency(deps).equals(getCompileDependency(oldDeps))) {
                            runLibertyMojoDeploy();
                        }
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
        public boolean recompileBuildFile(File buildFile, List<String> compileArtifactPaths,
                List<String> testArtifactPaths, ThreadPoolExecutor executor) throws PluginExecutionException {
            // monitoring project pom.xml file changes in dev mode:
            // - liberty.* properites in project properties section
            // - changes in liberty plugin configuration in the build plugin section
            // - project dependencies changes
            boolean restartServer = false;
            boolean createServer = false;
            boolean installFeature = false;
            boolean redeployApp = false;
            boolean runBoostPackage = false;

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

                List<Dependency> deps = project.getDependencies();
                List<Dependency> oldDeps = backupProject.getDependencies();
                if (!deps.equals(oldDeps)) {
                    runBoostPackage = true;
                    // detect esa dependency changes
                    if (!getEsaDependency(deps).equals(getEsaDependency(oldDeps))) {
                        installFeature = true;
                    }
                    // detect compile dependency changes
                    if (!getCompileDependency(deps).equals(getCompileDependency(oldDeps))) {
                        redeployApp = true;
                    }
                }
                // update classpath for dependencies changes
                compileArtifactPaths.clear();
                compileArtifactPaths.addAll(project.getCompileClasspathElements());
                testArtifactPaths.clear();
                testArtifactPaths.addAll(project.getTestClasspathElements());

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
                    // updated
                    // project set in the session
                    log.debug("changes in the pom.xml are not monitored by dev mode");
                    return true;
                }
            } catch (MojoExecutionException | ProjectBuildingException | DependencyResolutionRequiredException e) {
                log.error("An unexpected error occurred while processing changes in pom.xml. " + e.getMessage());
                log.debug(e);
                project = backupProject;
                session.setCurrentProject(backupProject);
                return false;
            }
            return true;
        }

        @Override
        public void checkConfigFile(File configFile, File serverDir) {
            try {
                ServerFeature servUtil = getServerFeatureUtil();
                Set<String> features = servUtil.getServerFeatures(serverDir, libertyDirPropertyFiles);
                if (features != null) {
                    features.removeAll(existingFeatures);
                    if (!features.isEmpty()) {
                        log.info("Configuration features have been added");
                        Element[] featureElems = new Element[features.size() + 1];
                        featureElems[0] = element(name("acceptLicense"), "true");
                        String[] values = features.toArray(new String[features.size()]);
                        for (int i = 0; i < features.size(); i++) {
                            featureElems[i + 1] = element(name("feature"), values[i]);
                        }
                        runLibertyMojoInstallFeature(element(name("features"), featureElems), super.getContainerName());
                        this.existingFeatures.addAll(features);
                    }
                }
            } catch (MojoExecutionException e) {
                log.error("Failed to install features from configuration file", e);
            }
        }

        @Override
        public boolean compile(File dir) {
            try {
                if (dir.equals(sourceDirectory)) {
                    runMojo("org.apache.maven.plugins", "maven-compiler-plugin", "compile");
                    runMojo("org.apache.maven.plugins", "maven-resources-plugin", "resources");
                }
                if (dir.equals(testSourceDirectory)) {
                    runMojo("org.apache.maven.plugins", "maven-compiler-plugin", "testCompile");
                    runMojo("org.apache.maven.plugins", "maven-resources-plugin", "testResources");
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

                    // if the ear artifact is not in .m2, install a dummy ear as a workaround so that downstream modules can build
                    installEarToM2(project);
                } else {
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
            runLibertyMojoCreate();
            // If non-container, install features before starting server. Otherwise, user
            // should have "RUN features.sh" in their Dockerfile if they want features to be
            // installed.
            if (!container) {
                runLibertyMojoInstallFeature(null, null);
            }
            runLibertyMojoDeploy();
        }
        // resource directories
        List<File> resourceDirs = getResourceDirectories(project, outputDirectory);

        JavaCompilerOptions compilerOptions = getMavenCompilerOptions(project);

        // collect upstream projects
        List<UpstreamProject> upstreamProjects = new ArrayList<UpstreamProject>();
        if (!upstreamMavenProjects.isEmpty()) {
            for (MavenProject p : upstreamMavenProjects) {
                // get compiler options for upstream project
                JavaCompilerOptions upstreamCompilerOptions = getMavenCompilerOptions(p);

                List<String> compileArtifacts = new ArrayList<String>();
                List<String> testArtifacts = new ArrayList<String>();
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

                // CLI properties should always take precedence, otherwise use the values set in
                // the pom file
                boolean upstreamSkipTests = Boolean
                        .parseBoolean(userProps.getProperty("skipTests") != null ? userProps.getProperty("skipTests")
                                : props.getProperty("skipTests"));
                boolean upstreamSkipITs = Boolean
                        .parseBoolean(userProps.getProperty("skipITs") != null ? userProps.getProperty("skipITs")
                                : props.getProperty("skipITs"));
                boolean upstreamSkipUTs = Boolean
                        .parseBoolean(userProps.getProperty("skipUTs") != null ? userProps.getProperty("skipUTs")
                                : props.getProperty("skipUTs"));

                // only force skipping unit test for ear modules otherwise honour existing skip
                // test params
                if (p.getPackaging().equals("ear")) {
                    upstreamSkipUTs = true;
                }

                UpstreamProject upstreamProject = new UpstreamProject(p.getFile(), p.getArtifactId(), compileArtifacts,
                        testArtifacts, upstreamSourceDir, upstreamOutputDir, upstreamTestSourceDir,
                        upstreamTestOutputDir, upstreamResourceDirs, upstreamSkipTests, upstreamSkipUTs,
                        upstreamSkipITs, upstreamCompilerOptions);
                upstreamProjects.add(upstreamProject);
            }
        }
        // skip unit tests for ear applications
        if (isEar) {
            skipUTs = true;
        }
        util = new DevMojoUtil(installDirectory, userDirectory, serverDirectory, sourceDirectory, testSourceDirectory,
                configDirectory, project.getBasedir(), multiModuleProjectDirectory, resourceDirs, compilerOptions,
                settings.getLocalRepository(), upstreamProjects, upstreamMavenProjects);
        util.addShutdownHook(executor);
        util.startServer();

        // collect artifacts canonical paths in order to build classpath
        List<String> compileArtifactPaths = project.getCompileClasspathElements(); 
        List<String> testArtifactPaths = project.getTestClasspathElements();
        // pom.xml
        File pom = project.getFile();

        // start watching for keypresses immediately
        util.runHotkeyReaderThread(executor);

        // Note that serverXmlFile can be null. DevUtil will automatically watch
        // all files in the configDirectory,
        // which is where the server.xml is located if a specific serverXmlFile
        // configuration parameter is not specified.
        try {
            if (!upstreamProjects.isEmpty()) {
                // watch upstream projects for hot compilation if they exist
                util.watchFiles(pom, outputDirectory, testOutputDirectory, executor, compileArtifactPaths,
                        testArtifactPaths, serverXmlFile, bootstrapPropertiesFile, jvmOptionsFile);
            } else {
                util.watchFiles(pom, outputDirectory, testOutputDirectory, executor, compileArtifactPaths,
                        testArtifactPaths, serverXmlFile, bootstrapPropertiesFile, jvmOptionsFile);
            }
        } catch (PluginScenarioException e) {
            if (e.getMessage() != null) {
                // a proper message is included in the exception if the server has been stopped
                // by another process
                log.info(e.getMessage());
            }
            return; // enter shutdown hook
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
     * Gets a compiler option's value from CLI paramaters, maven-compiler-plugin's
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
                // if we can reesolve the project associated with build file, run IT tests on
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

    private void runTestMojo(String groupId, String artifactId, String goal, MavenProject currentProject) throws MojoExecutionException {
        Plugin plugin = getPluginForProject(groupId, artifactId, currentProject);
        Xpp3Dom config = ExecuteMojoUtil.getPluginGoalConfig(plugin, goal, log);

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
                summaryFile = new File(currentProject.getBuild().getDirectory(), "failsafe-reports/failsafe-summary.xml");
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
            Plugin failsafePlugin = getPluginForProject("org.apache.maven.plugins", "maven-failsafe-plugin", currentProject);
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
            Plugin surefirePlugin = getPluginForProject("org.apache.maven.plugins", "maven-surefire-plugin", currentProject);
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

        log.debug("POM file: " + currentProject.getFile() + "\n" + groupId + ":" + artifactId + " " + goal
                + " configuration:\n" + config);
        MavenSession testSession = session.clone();
        testSession.setCurrentProject(currentProject);
        executeMojo(plugin, goal(goal), config, executionEnvironment(currentProject, testSession, pluginManager));
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

    private static ServerFeature serverFeatureUtil;

    private ServerFeature getServerFeatureUtil() {
        if (serverFeatureUtil == null) {
            serverFeatureUtil = new ServerFeature();
        }
        return serverFeatureUtil;
    }

    private class ServerFeature extends ServerFeatureUtil {

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
        public void error(String msg, Throwable e) {
            log.error(msg, e);
        }

    }

    /**
     * Executes Maven goal passed but sets failOnError to false All errors are
     * logged as warning messages
     * 
     * @param goal Maven compile goal
     * @throws MojoExecutionException
     */
    private void runCompileMojo(String goal) throws MojoExecutionException {
        Plugin plugin = getPlugin("org.apache.maven.plugins", "maven-compiler-plugin");
        Xpp3Dom config = ExecuteMojoUtil.getPluginGoalConfig(plugin, goal, log);
        config = Xpp3Dom.mergeXpp3Dom(configuration(element(name("failOnError"), "false")), config);
        log.info("Running maven-compiler-plugin:" + goal);
        log.debug("configuration:\n" + config);
        executeMojo(plugin, goal(goal), config, executionEnvironment(project, session, pluginManager));
    }

    /**
     * Executes maven:compile but logs errors as warning messages
     * 
     * @throws MojoExecutionException
     */
    private void runCompileMojoLogWarning() throws MojoExecutionException {
        runCompileMojo("compile");
    }

    /**
     * Executes maven:testCompile but logs errors as warning messages
     * 
     * @throws MojoExecutionException
     */
    private void runTestCompileMojoLogWarning() throws MojoExecutionException {
        runCompileMojo("testCompile");
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
}
