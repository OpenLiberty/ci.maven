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
package net.wasdev.wlp.maven.plugins.server;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Resource;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.XMLUnit;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.twdata.maven.mojoexecutor.MojoExecutor.Element;

import net.wasdev.wlp.ant.ServerTask;
import net.wasdev.wlp.common.plugins.util.DevUtil;
import net.wasdev.wlp.common.plugins.util.PluginExecutionException;
import net.wasdev.wlp.common.plugins.util.PluginScenarioException;
import net.wasdev.wlp.common.plugins.util.ServerFeatureUtil;
import net.wasdev.wlp.maven.plugins.utils.MavenProjectUtil;

/**
 * Start a liberty server in dev mode import to set ResolutionScope for TEST as
 * it helps build full transitive dependency classpath
 */
@Mojo(name = "dev", requiresDependencyCollection = ResolutionScope.TEST, requiresDependencyResolution = ResolutionScope.TEST)
public class DevMojo extends StartDebugMojoSupport {

    private static final String TEST_RUN_ID_PROPERTY_NAME = "liberty.dev.test.run.id";

    @Parameter(property = "hotTests", defaultValue = "false")
    private boolean hotTests;

    @Parameter(property = "skipTests", defaultValue = "false")
    private boolean skipTests;

    @Parameter(property = "skipUTs", defaultValue = "false")
    private boolean skipUTs;

    @Parameter(property = "skipITs", defaultValue = "false")
    private boolean skipITs;

    @Parameter(property = "liberty.debug.port", defaultValue = "7777")
    private int libertyDebugPort;

    private int runId = 0;

    private ServerTask serverTask = null;

    @Component
    private BuildPluginManager pluginManager;

    /**
     * Time in seconds to wait while verifying that the server has started.
     */
    @Parameter(property = "verifyTimeout", defaultValue = "30")
    private int verifyTimeout = 30;

    /**
     * Time in seconds to wait while verifying that the application has updated.
     */
    @Parameter(property = "appUpdateTimeout", defaultValue = "5")
    private int appUpdateTimeout = 5;

    /**
     * Time in seconds to wait while verifying that the server has started.
     */
    @Parameter(property = "serverStartTimeout", defaultValue = "30")
    private int serverStartTimeout = 30;

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

        List<Dependency> existingDependencies;
        String existingPom;
        Set<String> existingFeatures;

        public DevMojoUtil(File serverDirectory, File sourceDirectory, File testSourceDirectory, File configDirectory,
                List<File> resourceDirs) throws IOException {
            super(serverDirectory, sourceDirectory, testSourceDirectory, configDirectory, resourceDirs, hotTests,
                    skipTests, skipUTs, skipITs, project.getArtifactId(), appUpdateTimeout);

            this.existingDependencies = project.getDependencies();
            File pom = project.getFile();
            this.existingPom = readFile(pom);
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
        public ServerTask getDebugServerTask() throws IOException {
            if (serverTask != null) {
                return serverTask;
            } else {
                // Setup server task
                serverTask = initializeJava();
                copyConfigFiles();
                serverTask.setClean(clean);
                serverTask.setOperation("debug");
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

        @Override
        public boolean recompileBuildFile(File buildFile, List<String> artifactPaths, ThreadPoolExecutor executor) {
            try {
                String modifiedPom = readFile(buildFile);
                XMLUnit.setIgnoreWhitespace(true);
                XMLUnit.setIgnoreAttributeOrder(true);
                XMLUnit.setIgnoreComments(true);
                DetailedDiff diff = new DetailedDiff(XMLUnit.compareXML(this.existingPom, modifiedPom));
                List<?> allDifferences = diff.getAllDifferences();
                log.debug("Number of differences in the pom: " + allDifferences.size());

                if (!allDifferences.isEmpty()) {
                    log.info("Pom has been modified");
                    MavenProject updatedProject = loadProject(buildFile);
                    List<Dependency> dependencies = updatedProject.getDependencies();
                    log.debug("Dependencies size: " + dependencies.size());
                    log.debug("Existing dependencies size: " + this.existingDependencies.size());

                    List<String> dependencyIds = new ArrayList<String>();
                    List<Artifact> updatedArtifacts = getNewDependencies(dependencies, this.existingDependencies);

                    if (!updatedArtifacts.isEmpty()) {
                        for (Artifact artifact : updatedArtifacts) {
                            if (("esa").equals(artifact.getType())) {
                                dependencyIds.add(artifact.getArtifactId());
                            }

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
                            try {
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
                            } catch (DependencyResolutionException e) {
                                throw new MojoExecutionException(e.getMessage(), e);
                            }
                            artifactPaths.addAll(addToClassPath);
                        }

                        if (!dependencyIds.isEmpty()) {
                            runMojo("net.wasdev.wlp.maven.plugins:liberty-maven-plugin", "install-feature", serverName,
                                    dependencyIds);
                            dependencyIds.clear();
                        }

                        // update dependencies
                        this.existingDependencies = dependencies;
                        this.existingPom = modifiedPom;

                        return true;
                    } else {
                        log.info("Unhandled change detected in pom.xml. Restart liberty:dev mode for it to take effect.");
                    }
                }

            } catch (Exception e) {
                log.debug("Could not recompile pom.xml", e);
            }
            return false;
        }

        @Override
        public void checkConfigFile(File configFile, File serverDir) {
            try {
                ServerFeature servUtil = getServerFeatureUtil();
                Set<String> features = servUtil.getServerFeatures(serverDir);
                features.removeAll(existingFeatures);
                if (!features.isEmpty()) {
                    List<String> configFeatures = new ArrayList<String>(features);
                    log.info("Configuration features have been added");
                    runMojo("net.wasdev.wlp.maven.plugins:liberty-maven-plugin", "install-feature", serverName,
                            configFeatures);
                    this.existingFeatures.addAll(features);
                }
            } catch (MojoExecutionException e) {
                log.error("Failed to install features from configuration file", e);
            }
        }

        @Override
        public boolean compile(File dir) {
            try {
                if (dir.equals(sourceDirectory)) {
                    log.info("Running maven-compiler-plugin:compile");
                    runMojo("org.apache.maven.plugins:maven-compiler-plugin", "compile", null, null);

                    log.info("Running maven-compiler-plugin:resources");
                    runMojo("org.apache.maven.plugins:maven-resources-plugin", "resources", null, null);
                }
                if (dir.equals(testSourceDirectory)) {
                    log.info("Running maven-compiler-plugin:testCompile");
                    runMojo("org.apache.maven.plugins:maven-compiler-plugin", "testCompile", null, null);
                    log.info("Running maven-compiler-plugin:testResources");
                    runMojo("org.apache.maven.plugins:maven-resources-plugin", "testResources", null, null);
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
                runMojo("org.apache.maven.plugins:maven-war-plugin", "war", null, null);
                runTestMojo("org.apache.maven.plugins", "maven-failsafe-plugin", "integration-test");
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

    @Override
    protected void doExecute() throws Exception {
        if (skip) {
            return;
        }
        
        // create an executor for tests with an additional queue of size 1, so
        // any further changes detected mid-test will be in the following run
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(1, true));

        log.info("Running maven-compiler-plugin:compile");
        runMojo("org.apache.maven.plugins:maven-compiler-plugin", "compile", null, null);
        log.info("Running maven-compiler-plugin:resources");
        runMojo("org.apache.maven.plugins:maven-resources-plugin", "resources", null, null);
        log.info("Running maven-compiler-plugin:testCompile");
        runMojo("org.apache.maven.plugins:maven-compiler-plugin", "testCompile", null, null);
        log.info("Running maven-compiler-plugin:testResources");
        runMojo("org.apache.maven.plugins:maven-resources-plugin", "testResources", null, null);
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

        log.info("Running goal: create-server");
        runMojo("net.wasdev.wlp.maven.plugins:liberty-maven-plugin", "create-server", serverName, null);
        log.info("Running goal: install-feature");
        runMojo("net.wasdev.wlp.maven.plugins:liberty-maven-plugin", "install-feature", serverName, null);
        log.info("Running goal: install-apps");
        runMojo("net.wasdev.wlp.maven.plugins:liberty-maven-plugin", "install-apps", serverName, null);
        
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

        DevMojoUtil util = new DevMojoUtil(serverDirectory, sourceDirectory, testSourceDirectory, configDirectory, resourceDirs);
        util.addShutdownHook(executor);
        util.enableServerDebug(libertyDebugPort);
        util.startServer(serverStartTimeout, verifyTimeout);

        // collect artifacts canonical paths in order to build classpath
        List<String> artifactPaths = util.getArtifacts();

        if (hotTests && testSourceDirectory.exists()) {
            // if hot testing, run tests on startup and then watch for keypresses
            util.runTestThread(false, executor, -1, false, false);
        } else {
            // else watch for keypresses immediately
            util.runHotkeyReaderThread(executor);
        }

        // pom.xml
        File pom = project.getFile();
        
        util.watchFiles(pom, outputDirectory, testOutputDirectory, executor, artifactPaths, configFile);
    }

    private void addArtifacts(org.eclipse.aether.graph.DependencyNode root, List<File> artifacts) {
        if (root.getArtifact() != null) {
            artifacts.add(root.getArtifact().getFile());
        }

        for (org.eclipse.aether.graph.DependencyNode node : root.getChildren()) {
            addArtifacts(node, artifacts);
        }
    }

    private List<Artifact> getNewDependencies(List<Dependency> dependencies, List<Dependency> existingDependencies)
            throws MojoExecutionException {
        List<Artifact> updatedArtifacts = new ArrayList<Artifact>();
        for (Dependency dep : dependencies) {
            boolean newDependency = true;
            for (Dependency existingDep : existingDependencies) {
                if (dep.getArtifactId().equals(existingDep.getArtifactId())) {
                    newDependency = false;
                    break;
                }
            }
            if (newDependency) {
                log.debug("New dependency found: " + dep.getArtifactId());
                Artifact artifact = getArtifact(dep.getGroupId(), dep.getArtifactId(), dep.getType(), dep.getVersion());
                updatedArtifacts.add(artifact);
            }
        }
        return updatedArtifacts;
    }

    private void runTestMojo(String groupId, String artifactId, String phase) throws MojoExecutionException {

        Plugin plugin = project.getPlugin(groupId + ":" + artifactId);
        if (plugin == null) {
            plugin = plugin(groupId(groupId), artifactId(artifactId), version("RELEASE"));
        }
        Xpp3Dom config = null;

        List<Plugin> buildPlugins = project.getBuildPlugins();
        for (Plugin p : buildPlugins) {
            if (p.equals(plugin)) {
                config = (Xpp3Dom) p.getConfiguration();
                
                PluginExecution pluginExecution = null;
                Map<String, PluginExecution> peMap = p.getExecutionsAsMap();

                String[] defaultExecutionIds = new String[]{ "default-" + phase, phase, "default" };
                for (String executionId : defaultExecutionIds) {
                    pluginExecution = peMap.get(executionId);
                    if (pluginExecution != null) {
                        break;
                    }
                }
                if (pluginExecution != null) {
                    Xpp3Dom executionConfig = (Xpp3Dom) pluginExecution.getConfiguration();
                    config = Xpp3Dom.mergeXpp3Dom(executionConfig, config);
                }
                break;
            }
        }

        if (config == null) {
            log.debug("Could not find " + artifactId + " configuration for " + phase
                    + " phase. Creating new configuration.");
            config = configuration();
        }

        if (phase.equals("test")) {
            injectTestId(config);
        } else if (phase.equals("integration-test")) {
            injectTestId(config);
            injectLibertyProperties(config);
            // clean up previous summary file
            File summaryFile = null;
            Xpp3Dom summaryFileElement = config.getChild("summaryFile");
            if (summaryFileElement != null) {
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
        }
        log.debug(artifactId + " configuration for " + phase + " phase: " + config);

        executeMojo(plugin, goal(phase), config, executionEnvironment(project, session.clone(), pluginManager));
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
     * @param config The configuration element
     * @throws MojoExecutionException if the userDirectory canonical path cannot be resolved
     */
    private void injectLibertyProperties(Xpp3Dom config) throws MojoExecutionException {
        Xpp3Dom sysProps = config.getChild("systemPropertyVariables");
        if (sysProps == null) {
            Element e = element(name("systemPropertyVariables"));
            sysProps = e.toDom();
            config.addChild(sysProps);
        }
        // don't overwrite existing properties if they are already defined
        if (sysProps.getChild("liberty.user.dir") == null) {
            // pass in userDirectory parameter
            try {
                sysProps.addChild(element(name("liberty.user.dir"), userDirectory.getCanonicalPath()).toDom());
            } catch (IOException e) {
                throw new MojoExecutionException("Could not resolve canonical path of userDirectory parameter: " + userDirectory.getAbsolutePath(), e);
            }
        }
    }

    private Element[] getPluginConfigurationElements(String goal, String testServerName, List<String> dependencies) {
        List<Element> elements = new ArrayList<Element>();
        try {
            if (testServerName != null) {
                elements.add(element(name("serverName"), testServerName));
                elements.add(element(name("configDirectory"), configDirectory.getCanonicalPath()));
                if (goal.equals("install-feature") && (dependencies != null)) {
                    Element[] featureElems = new Element[dependencies.size()];
                    for (int i = 0; i < featureElems.length; i++) {
                        featureElems[i] = element(name("feature"), dependencies.get(i));
                    }
                    elements.add(element(name("features"), featureElems));
                } else if (goal.equals("install-apps")) {
                    String appsDirectory = MavenProjectUtil.getPluginExecutionConfiguration(project, 
                        "net.wasdev.wlp.maven.plugins", "liberty-maven-plugin", "install-apps", "appsDirectory");
                    if (appsDirectory != null) {
                        elements.add(element(name("appsDirectory"), appsDirectory));
                    }
                    elements.add(element(name("looseApplication"), "true"));
                    elements.add(element(name("stripVersion"), "true"));
                    elements.add(element(name("installAppPackages"), "project"));
                    elements.add(element(name("configFile"), configFile.getCanonicalPath()));
                } else if (goal.equals("create-server")) {
                    elements.add(element(name("configFile"), configFile.getCanonicalPath()));
                    if (assemblyArtifact != null) {
                        Element[] featureElems = new Element[4];
                        featureElems[0] = element(name("groupId"), assemblyArtifact.getGroupId());
                        featureElems[1] = element(name("artifactId"), assemblyArtifact.getArtifactId());
                        featureElems[2] = element(name("version"), assemblyArtifact.getVersion());
                        featureElems[3] = element(name("type"), assemblyArtifact.getType());
                        elements.add(element(name("assemblyArtifact"), featureElems));
                    }
                }
            }
        } catch (IOException e) {
            log.error("Unable to resolve canonical paths " + e.getMessage());
        }
        return elements.toArray(new Element[elements.size()]);
    }

    private void runMojo(String plugin, String goal, String serverName, List<String> dependencies)
            throws MojoExecutionException {
        Plugin mavenPlugin = project.getPlugin(plugin);
        log.info("plugin version: " + mavenPlugin.getVersion());
        executeMojo(mavenPlugin, goal(goal),
                configuration(getPluginConfigurationElements(goal, serverName, dependencies)),
                executionEnvironment(project, session, pluginManager));
    }

    private static MavenProject loadProject(File pomFile) throws IOException, XmlPullParserException {
        MavenProject ret = null;
        MavenXpp3Reader mavenReader = new MavenXpp3Reader();

        if (pomFile != null && pomFile.exists()) {
            FileReader reader = null;
            try {
                reader = new FileReader(pomFile);
                Model model = mavenReader.read(reader);
                model.setPomFile(pomFile);
                ret = new MavenProject(model);
            } finally {
                reader.close();
            }
        }
        return ret;
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
