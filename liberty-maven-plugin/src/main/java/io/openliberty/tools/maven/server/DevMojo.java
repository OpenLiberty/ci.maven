/**
 * (C) Copyright IBM Corporation 2019.
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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
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
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.twdata.maven.mojoexecutor.MojoExecutor.Element;

import io.openliberty.tools.ant.ServerTask;
import io.openliberty.tools.common.plugins.util.DevUtil;
import io.openliberty.tools.common.plugins.util.PluginExecutionException;
import io.openliberty.tools.common.plugins.util.PluginScenarioException;
import io.openliberty.tools.common.plugins.util.ServerFeatureUtil;
import io.openliberty.tools.common.plugins.util.ServerStatusUtil;
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
     * Time in seconds to wait while verifying that the application has started.
     */
    @Parameter(property = "verifyTimeout", defaultValue = "30")
    private int verifyTimeout;

    /**
     * Time in seconds to wait while verifying that the application has updated.
     */
    @Parameter(property = "appUpdateTimeout", defaultValue = "5")
    private int appUpdateTimeout;

    /**
     * Time in seconds to wait while verifying that the server has started.
     */
    @Parameter(property = "serverStartTimeout", defaultValue = "30")
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

    private class DevMojoUtil extends DevUtil {

        Set<String> existingFeatures;

        public DevMojoUtil(File serverDirectory, File sourceDirectory, File testSourceDirectory, File configDirectory,
                List<File> resourceDirs) throws IOException {
            super(serverDirectory, sourceDirectory, testSourceDirectory, configDirectory, resourceDirs, hotTests,
                    skipTests, skipUTs, skipITs, project.getArtifactId(), verifyTimeout, appUpdateTimeout,
                    ((long) (compileWait * 1000L)), libertyDebug);

            ServerFeature servUtil = getServerFeatureUtil();
            this.existingFeatures = servUtil.getServerFeatures(serverDirectory);
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
        public void stopServer() {
            try {
                ServerTask serverTask = initializeJava();
                serverTask.setOperation("stop");
                serverTask.execute();
            } catch (Exception e) {
                // ignore
                log.debug("Error stopping server", e);
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

                    // set the same variables in server.env after it was written to the target
                    // location by copyConfigFiles() above
                    enableServerDebug();
                } else {
                    serverTask.setOperation("run");
                }
                return serverTask;
            }
        }

        @Override
        public List<String> getArtifacts() {
            List<String> artifactPaths = new ArrayList<String>();
            Set<Artifact> artifacts = project.getArtifacts();
            for (Artifact artifact : artifacts) {
                try {
                    artifactPaths.add(artifact.getFile().getCanonicalPath());
                } catch (IOException e) {
                    log.error("Unable to resolve project artifact " + e.getMessage());
                }
            }
            return artifactPaths;
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
            if (!Objects.equals(config.getChild("bootstrapProperties"),
                    oldConfig.getChild("bootstrapProperties"))) {
                return true;
            } else if (!Objects.equals(config.getChild("bootstrapPropertiesFile"),
                    oldConfig.getChild("bootstrapPropertiesFile"))) {
                return true;
            } else if (!Objects.equals(config.getChild("jvmOptions"),
                    oldConfig.getChild("jvmOptions"))) {
                return true;
            } else if (!Objects.equals(config.getChild("jvmOptionsFile"),
                    oldConfig.getChild("jvmOptionsFile"))) {
                return true;
            } else if (!Objects.equals(config.getChild("serverEnv"),
                    oldConfig.getChild("serverEnv"))) {
                return true;
            } else if (!Objects.equals(config.getChild("serverEnvFile"),
                    oldConfig.getChild("serverEnvFile"))) {
                return true;
            } else if (!Objects.equals(config.getChild("configDirectory"),
                    oldConfig.getChild("configDirectory"))) {
                return true;
            }
            return false;
        }

        @Override
        public boolean recompileBuildFile(File buildFile, List<String> artifactPaths, ThreadPoolExecutor executor) {
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
                    // update classpath for dependencies changes
                    List<Artifact> updatedArtifacts = getNewDependencies(deps, oldDeps);
                    if (!updatedArtifacts.isEmpty()) {
                        for (Artifact artifact : updatedArtifacts) {
                            org.eclipse.aether.artifact.Artifact aetherArtifact = new org.eclipse.aether.artifact.DefaultArtifact(
                                    artifact.getGroupId(), artifact.getArtifactId(), artifact.getType(),
                                    artifact.getVersion());
                            org.eclipse.aether.graph.Dependency dependency = new org.eclipse.aether.graph.Dependency(
                                    aetherArtifact, null, true);

                            CollectRequest collectRequest = new CollectRequest();
                            collectRequest.setRoot(dependency);
                            collectRequest.setRepositories(repositories);

                            List<String> addToClassPath = new ArrayList<String>();
                            DependencyRequest depRequest = new DependencyRequest(collectRequest, null);

                            DependencyResult dependencyResult = repositorySystem.resolveDependencies(repoSession,
                                    depRequest);
                            org.eclipse.aether.graph.DependencyNode root = dependencyResult.getRoot();
                            List<File> artifactsList = new ArrayList<File>();
                            addArtifacts(root, artifactsList);
                            for (File a : artifactsList) {
                                log.debug("Artifact: " + a);
                                if (a.getCanonicalPath().endsWith(".jar")) {
                                    addToClassPath.add(a.getCanonicalPath());
                                }
                            }
                            artifactPaths.addAll(addToClassPath);
                        }
                    }
                }

                if (restartServer) {
                    // TODO: restart server automatically
                    // - stop Server
                    // - create server or runBoostMojo
                    // - install feature
                    // - deploy app
                    // - start server
                    log.error("Changes for Liberty server bootstrap properties, environment variables or JVM options "
                            + "in the pom.xml have been detected. Restart liberty:dev mode for the changes to take effect.");
                    project = backupProject;
                    session.setCurrentProject(backupProject);
                    return false;
                } else {
                    if (isUsingBoost() && (createServer || runBoostPackage)) {
                        log.info("Running boost:package");
                        runBoostMojo("package", false);
                    } else if (createServer) {
                        runLibertyMojoCreate();
                    } else if (redeployApp) {
                        runLibertyMojoDeploy();
                    }
                    if (installFeature) {
                        runLibertyMojoInstallFeature(null);
                    }
                }
                if (!(restartServer || createServer || redeployApp || installFeature || runBoostPackage)) {
                    // pom.xml is changed but not affecting liberty:dev mode. return true with the updated 
                    // project set in the session 
                    log.debug("changes in the pom.xml are not monitored by dev mode");
                    return true;
                }
            } catch (IOException | DependencyResolutionException | MojoExecutionException
                    | ProjectBuildingException e) {
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
                Set<String> features = servUtil.getServerFeatures(serverDir);
                if (features != null) {
                    features.removeAll(existingFeatures);
                    if (!features.isEmpty()) {
                        log.info("Configuration features have been added");
                        Element[] featureElems = new Element[features.size() + 1];
                        featureElems[0] = element(name("acceptLicense"), "true");
                        String[] values = features.toArray(new String[features.size()]);
                        for (int i = 0; i < features.size(); i++) {
                            featureElems[i+1] = element(name("feature"), values[i]);
                        }
                        runLibertyMojoInstallFeature(element(name("features"), featureElems));
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
        public void runUnitTests() throws PluginExecutionException, PluginScenarioException {
            try {
                runTestMojo("org.apache.maven.plugins", "maven-surefire-plugin", "test");
                runTestMojo("org.apache.maven.plugins", "maven-surefire-report-plugin", "report-only");
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
        public void runIntegrationTests() throws PluginExecutionException, PluginScenarioException {
            try {
                runTestMojo("org.apache.maven.plugins", "maven-failsafe-plugin", "integration-test");
                runTestMojo("org.apache.maven.plugins", "maven-surefire-report-plugin", "failsafe-report-only");
                runTestMojo("org.apache.maven.plugins", "maven-failsafe-plugin", "verify");
            } catch (MojoExecutionException e) {
                Throwable cause = e.getCause();
                if (cause != null && cause instanceof MojoFailureException) {
                    throw new PluginScenarioException("Integration tests failed: " + cause.getLocalizedMessage(), e);
                } else {
                    throw new PluginExecutionException("Failed to run integration tests", e);
                }
            }
        }
    }

    private boolean isUsingBoost() {
        return boostPlugin != null;
    }

    @Override
    protected void doExecute() throws Exception {
        if (skip) {
            return;
        }

        // skip unit tests for ear packaging
        if (project.getPackaging().equals("ear")) {
            skipUTs = true;
        }

        // Check if this is a Boost application
        boostPlugin = project.getPlugin("org.microshed.boost:boost-maven-plugin");

        if (serverDirectory.exists()) {
            // passing liberty installDirectory, outputDirectory and serverName to determine server status
            if (ServerStatusUtil.isServerRunning(installDirectory, super.outputDirectory, serverName)) {
                throw new MojoExecutionException("The server " + serverName
                        + " is already running. Terminate all instances of the server before starting dev mode.");
            }
        }

        // create an executor for tests with an additional queue of size 1, so
        // any further changes detected mid-test will be in the following run
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(1, true));

        runMojo("org.apache.maven.plugins", "maven-compiler-plugin", "compile");
        runMojo("org.apache.maven.plugins", "maven-resources-plugin", "resources");
        runMojo("org.apache.maven.plugins", "maven-compiler-plugin", "testCompile");
        runMojo("org.apache.maven.plugins", "maven-resources-plugin", "testResources");
        
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
            runBoostMojo("package", false);
        } else {
            runLibertyMojoCreate();
            runLibertyMojoInstallFeature(null);
            runLibertyMojoDeploy();
        }
        // resource directories
        List<File> resourceDirs = new ArrayList<File>();
        if (outputDirectory.exists()) {
            List<Resource> resources = project.getResources();
            for (Resource resource : resources) {
                File resourceFile = new File(resource.getDirectory());
                if (resourceFile.exists()) {
                    resourceDirs.add(resourceFile);
                }
            }
        }
        if (resourceDirs.isEmpty()) {
            File defaultResourceDir = new File(project.getBasedir() + "/src/main/resources");
            log.debug("No resource directory detected, using default directory: " + defaultResourceDir);
            resourceDirs.add(defaultResourceDir);
        }

        util = new DevMojoUtil(serverDirectory, sourceDirectory, testSourceDirectory, configDirectory, resourceDirs);
        util.addShutdownHook(executor);
        util.startServer(serverStartTimeout);

        // collect artifacts canonical paths in order to build classpath
        List<String> artifactPaths = util.getArtifacts();

        if (hotTests && testSourceDirectory.exists()) {
            // if hot testing, run tests on startup and then watch for
            // keypresses
            util.runTestThread(false, executor, -1, false, false);
        } else {
            // else watch for keypresses immediately
            util.runHotkeyReaderThread(executor);
        }

        // pom.xml
        File pom = project.getFile();

        // Note that serverXmlFile can be null. DevUtil will automatically watch
        // all files in the configDirectory,
        // which is where the server.xml is located if a specific serverXmlFile
        // configuration parameter is not specified.
        try {
            util.watchFiles(pom, outputDirectory, testOutputDirectory, executor, artifactPaths, serverXmlFile);
        } catch (PluginScenarioException e) { // this exception is caught when the server has been stopped by another process
            log.info(e.getMessage()); 
            return; // enter shutdown hook 
        }
    }

    private void addArtifacts(org.eclipse.aether.graph.DependencyNode root, List<File> artifacts) {
        if (root.getArtifact() != null) {
            artifacts.add(root.getArtifact().getFile());
        }

        for (org.eclipse.aether.graph.DependencyNode node : root.getChildren()) {
            addArtifacts(node, artifacts);
        }
    }

    private List<Artifact> getNewDependencies(List<Dependency> dependencies, List<Dependency> existingDependencies) {
        List<Artifact> updatedArtifacts = new ArrayList<Artifact>();
        for (Dependency dep : dependencies) {
            boolean newDependency = true;
            try {
                // resolve new artifact
                Artifact artifact = getArtifact(dep.getGroupId(), dep.getArtifactId(), dep.getType(), dep.getVersion());

                // match dependencies based on artifactId, groupId, version and type
                for (Dependency existingDep : existingDependencies) {
                    Artifact existingArtifact = getArtifact(existingDep.getGroupId(), existingDep.getArtifactId(), existingDep.getType(), existingDep.getVersion());
                    if (Objects.equals(artifact.getArtifactId(), existingArtifact.getArtifactId())
                            && Objects.equals(artifact.getGroupId(), existingArtifact.getGroupId())
                            && Objects.equals(artifact.getVersion(), existingArtifact.getVersion())
                            && Objects.equals(artifact.getType(), existingArtifact.getType())) {
                        newDependency = false;
                        break;
                    }
                }
                if (newDependency) {
                    log.debug("New dependency found: " + artifact.toString());
                    updatedArtifacts.add(artifact);
                }
            } catch (MojoExecutionException e) {
                log.warn(e.getMessage());
            } catch (IllegalArgumentException e) {
                log.warn(dep.toString() + " is not valid: " + e.getMessage());
            }
        }
        return updatedArtifacts;
    }

    private void runTestMojo(String groupId, String artifactId, String goal) throws MojoExecutionException {
        Plugin plugin = getPlugin(groupId, artifactId);
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
                summaryFile = new File(project.getBuild().getDirectory() + "/failsafe-reports/failsafe-summary.xml");
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
            Plugin failsafePlugin = getPlugin("org.apache.maven.plugins", "maven-failsafe-plugin");
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
            Plugin surefirePlugin = getPlugin("org.apache.maven.plugins", "maven-surefire-plugin");
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

        log.debug(groupId + ":" + artifactId + " " + goal + " configuration:\n" + config);
        executeMojo(plugin, goal(goal), config, executionEnvironment(project, session.clone(), pluginManager));
    }

    /**
     * Force change a property so that the checksum calculated by
     * AbstractSurefireMojo is different every time.
     *
     * @param config
     *            The configuration element
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
     * @param config
     *            The configuration element
     * @throws MojoExecutionException
     *             if the userDirectory canonical path cannot be resolved
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

    private void runBoostMojo(String goal, boolean rebuildProject)
            throws MojoExecutionException, ProjectBuildingException {

        MavenProject boostProject = this.project;
        MavenSession boostSession = this.session;

        if (rebuildProject) {
            // Reload pom
            File pomFile = new File(project.getFile().getAbsolutePath());
            ProjectBuildingResult build = mavenProjectBuilder.build(pomFile,
                    session.getProjectBuildingRequest().setResolveDependencies(true));
            boostProject = build.getProject();
            boostSession.setCurrentProject(boostProject);
        }

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

    }
}
