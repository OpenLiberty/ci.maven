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
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
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

/**
 * Start a liberty server in dev mode import to set ResolutionScope for
 * TEST as it helps build full transitive dependency classpath
 */
@Mojo(name = "dev", requiresDependencyCollection = ResolutionScope.TEST, requiresDependencyResolution = ResolutionScope.TEST)
public class DevMojo extends StartDebugMojoSupport {

    private static final String UPDATED_APP_MESSAGE_REGEXP = "CWWKZ0003I.*";

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
        
        public DevMojoUtil(List<String> jvmOptions, File serverDirectory, File sourceDirectory, File testSourceDirectory, File configDirectory, List<File> resourceDirs, boolean skipTests, boolean skipITs) throws IOException {
            super(jvmOptions, serverDirectory, sourceDirectory, testSourceDirectory, configDirectory, resourceDirs, skipTests, skipITs);
            this.existingDependencies = project.getDependencies();
            File pom = project.getFile();
            this.existingPom = readFile(pom);
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
        public void startServer() {
            try {
                if (serverTask == null) {
                    serverTask = initializeJava();
                }
                copyConfigFiles();
                serverTask.setClean(clean);
                serverTask.setOperation("start");
                // Set server start timeout
                if (serverStartTimeout < 0) {
                    serverStartTimeout = 30;
                }
                serverTask.setTimeout(Long.toString(serverStartTimeout * 1000));
                serverTask.execute();

                if (verifyTimeout < 0) {
                    verifyTimeout = 30;
                }
                long timeout = verifyTimeout * 1000;
                long endTime = System.currentTimeMillis() + timeout;
                if (applications != null) {
                    String[] apps = applications.split("[,\\s]+");
                    for (String archiveName : apps) {
                        String startMessage = serverTask.waitForStringInLog(START_APP_MESSAGE_REGEXP + archiveName,
                                timeout, serverTask.getLogFile());
                        if (startMessage == null) {
                            stopServer();
                            throw new MojoExecutionException(MessageFormat
                                    .format(messages.getString("error.server.start.verify"), verifyTimeout));
                        }
                        timeout = endTime - System.currentTimeMillis();
                    }
                }
            } catch (Exception e) {
                log.debug("Error starting server", e);
            }
        }
        
        @Override
        public void getArtifacts(List<String> artifactPaths){
            Set<Artifact> artifacts = project.getArtifacts();
            for (Artifact artifact : artifacts) {
                artifactPaths.add(artifact.getFile().getAbsolutePath());
            }
        }

        @Override
        public void recompileBuildFile(File buildFile, List<String> artifactPaths) {
            try {
                String modifiedPom = util.readFile(buildFile);
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
                                    if (a.getAbsolutePath().endsWith(".jar")) {
                                        addToClassPath.add(a.getAbsolutePath());
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
                    } else {
                        log.info("Unexpected change detected in pom.xml.  Please restart liberty:dev mode.");
                    }
                }

            } catch (Exception e) {
                log.debug("Could not recompile pom.xml", e);
            }
        }
        
        @Override
        public int getMessageOccurrences(String regexp, File logFile) {
            int messageOccurrences = -1;
            try {
                logFile = serverTask.getLogFile();
                regexp = UPDATED_APP_MESSAGE_REGEXP + DevMojo.this.project.getArtifactId();
                messageOccurrences = serverTask.countStringOccurrencesInFile(regexp, logFile);
                log.debug("Message occurrences before compile: " + messageOccurrences);
            } catch (Exception e) {
                log.debug("Failed to get message occurrences before compile", e);
            }
            return messageOccurrences;

        }
        
        @Override
        public void runTestThread(ThreadPoolExecutor executor, String regexp, File logFile, int messageOccurrences) {
            try {
                executor.execute(new TestJob(regexp, logFile, messageOccurrences, executor));
            } catch (RejectedExecutionException e) {
                log.debug("Cannot add thread since max threads reached", e);
            }
        }

    }

    DevMojoUtil util;

    @Override
    protected void doExecute() throws Exception {
        // create an executor for tests with an additional queue of size 1, so any further changes detected mid-test will be in the following run
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

        boolean noConfigDir = false;

        // config files
        if (configDirectory == null || !configDirectory.exists()) {
            configDirectory = configFile.getParentFile();
            noConfigDir = true;
            log.debug("configDirectory set to: " + configDirectory.getAbsolutePath());
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
        
        util = new DevMojoUtil(jvmOptions, serverDirectory, sourceDirectory, testSourceDirectory, configDirectory, resourceDirs, skipTests, skipITs);

        util.addShutdownHook(executor);

        util.enableServerDebug(libertyDebugPort);

        util.startServer();

        // collect artifacts absolute paths in order to build classpath
        List<String> artifactPaths = new ArrayList<String>();
        util.getArtifacts(artifactPaths);
        
        // run tests at startup
        runTestThread(executor, null, null, -1);
                
        // src/main/java files
        Path srcPath = sourceDirectory.getAbsoluteFile().toPath(); 
        Path testSrcPath = testSourceDirectory.getAbsoluteFile().toPath();  
        Path configPath = configDirectory.getAbsoluteFile().toPath(); 
             
        // pom.xml
        File pom = project.getFile();
        
        util.watchFiles(srcPath, testSrcPath, configPath, pom, outputDirectory, testOutputDirectory, executor, artifactPaths, noConfigDir, configFile);
    }
    
    private void addArtifacts(org.eclipse.aether.graph.DependencyNode root, List<File> artifacts) {
        if (root.getArtifact() != null) {
            artifacts.add(root.getArtifact().getFile());
        }
      
        for (org.eclipse.aether.graph.DependencyNode node : root.getChildren()){
            addArtifacts(node, artifacts);
        }
    }
    
    private List<Artifact> getNewDependencies(List<Dependency> dependencies, List<Dependency> existingDependencies) throws MojoExecutionException{
        List<Artifact> updatedArtifacts = new ArrayList<Artifact>();
        for (Dependency dep : dependencies) {
            boolean newDependency = true;
            for (Dependency existingDep : existingDependencies) {
                if (dep.getArtifactId().equals(existingDep.getArtifactId())) {
                    newDependency = false;
                }
            }
            if (newDependency){
                log.debug("New dependency found: " + dep.getArtifactId());
                Artifact artifact = getArtifact(dep.getGroupId(), dep.getArtifactId(), dep.getType(), dep.getVersion());
                updatedArtifacts.add(artifact);
            }
        }
        return updatedArtifacts;
    }

    private void runTestThread(ThreadPoolExecutor executor, String regexp, File logFile, int messageOccurrences) {
        try {
            executor.execute(new TestJob(regexp, logFile, messageOccurrences, executor));
        } catch (RejectedExecutionException e) {
            log.debug("Cannot add thread since max threads reached", e);
        }
    }

    private class TestJob implements Runnable {
        private String regexp;
        private File logFile;
        private int messageOccurrences;
        private ThreadPoolExecutor executor;

        public TestJob(String regexp, File logFile, int messageOccurrences, ThreadPoolExecutor executor) {
            this.regexp = regexp;
            this.logFile = logFile;
            this.messageOccurrences = messageOccurrences;
            this.executor = executor;
        }

        @Override
        public void run() {
            runTests(regexp, logFile, messageOccurrences, executor);
        }
    }

    private void runTests(String regexp, File logFile, int messageOccurrences, ThreadPoolExecutor executor) {
        if (skipTests) {
            return;
        }

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            log.debug("Thread interrupted while waiting to start unit tests.", e);
        }

        // if queue size >= 1, it means a newer test has been queued so we should skip this and let that run instead
        if (executor.getQueue().size() >= 1) {
            log.debug("Changes were detected before tests began. Cancelling tests and resubmitting them.");
            return;
        }

        if (!skipUTs) {
            log.info("Running unit tests...");
            try {
                runUnitTests();
                log.info("Unit tests finished.");
            } catch (MojoExecutionException e) {
                Throwable cause = e.getCause();
                if (cause != null && cause instanceof MojoFailureException) {
                    log.debug(e);
                    log.error("Unit tests failed: " + cause.getLocalizedMessage());
                    // if unit tests failed, don't run integration tests
                    return;
                } else {
                    log.error("Failed to run unit tests", e);
                }
            }
        }
        
        // if queue size >= 1, it means a newer test has been queued so we should skip this and let that run instead
        if (executor.getQueue().size() >= 1) {
            log.info("Changes were detected while tests were running. Restarting tests.");
            return;
        }
        
        if (!skipITs) {
            if (regexp != null) {
                // wait until application has been updated
                if (appUpdateTimeout < 0) {
                    appUpdateTimeout = 5;
                }
                long timeout = appUpdateTimeout * 1000;
                serverTask.waitForUpdatedStringInLog(regexp, timeout, logFile, messageOccurrences);
            }

            log.info("Running integration tests...");
            try {
                runIntegrationTests();
                log.info("Integration tests finished.");
            } catch (MojoExecutionException e) {
                Throwable cause = e.getCause();
                if (cause != null && cause instanceof MojoFailureException) {
                    log.debug(e);
                    log.error("Integration tests failed: " + cause.getLocalizedMessage());
                } else {
                    log.error("Failed to run integration tests", e);
                }
            }
        }
    }

    private void runTests(String groupId, String artifactId, String phase) throws MojoExecutionException {

        Plugin plugin = project.getPlugin(groupId + ":" + artifactId);
        if (plugin == null) {
            plugin = plugin(
                groupId(groupId),
                artifactId(artifactId),
                version("RELEASE")
            );
        }
        Xpp3Dom config = null;

        List<Plugin> buildPlugins = project.getBuildPlugins();
        for (Plugin p : buildPlugins) {
            if (p.equals(plugin)) {
                config = (Xpp3Dom) p.getConfiguration();

                PluginExecution pe;
                Map<String, PluginExecution> peMap = p.getExecutionsAsMap();
                if ((pe = peMap.get("default-" + phase)) != null || (pe = peMap.get(phase)) != null || (pe = peMap.get("default")) != null) {
                    Xpp3Dom executionConfig = (Xpp3Dom) pe.getConfiguration();
                    config = Xpp3Dom.mergeXpp3Dom(executionConfig, config);
                }
                break;
            }
        }

        if (config == null) {
            log.debug("Could not find " + artifactId + " configuration for " + phase + " phase. Creating new configuration.");
            config = configuration();
        }

        if (phase.equals("test")) {
            injectTestId(config);
        } else if (phase.equals("integration-test")) {
            injectTestId(config);
            // clean up previous summary file
            File summaryFile = null;
            Xpp3Dom summaryFileElement = config.getChild("summaryFile");
            if (summaryFileElement != null) {
                summaryFile = new File(summaryFileElement.getValue());
            } else {
                summaryFile = new File(project.getBuild().getDirectory() + "/failsafe-reports/failsafe-summary.xml");
            }
            log.debug("Looking for summary file at " + summaryFile.getAbsolutePath());
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
     * Force change a property so that the checksum calculated by AbstractSurefireMojo is different every time.
     *
     * @param config The configuration element
     */
    private void injectTestId(Xpp3Dom config) {
        Xpp3Dom properties = config.getChild("properties");
        String propertyName = "liberty.dev.test.run.id";
        if (properties == null || properties.getChild(propertyName) == null) {
            Element e = element(name("properties"), element(name(propertyName), String.valueOf(runId++)));
            config.addChild(e.toDom());
        } else {
            properties.getChild(propertyName).setValue(String.valueOf(runId++));
        }
    }

    private void runUnitTests() throws MojoExecutionException {
        runTests("org.apache.maven.plugins", "maven-surefire-plugin", "test");
    }

    private void runIntegrationTests() throws MojoExecutionException {
        runTests("org.apache.maven.plugins", "maven-failsafe-plugin", "integration-test");
        runTests("org.apache.maven.plugins", "maven-failsafe-plugin", "verify");
    }

    private Element[] getPluginConfigurationElements(String goal, String testServerName, List<String> dependencies) {
        List elements = new ArrayList<Element>();
        if (testServerName != null){
            elements.add(element(name("serverName"), testServerName));
            elements.add(element(name("configDirectory"), configDirectory.getAbsolutePath()));
            if (goal.equals("install-feature") && (dependencies != null)) {
                Element[] featureElems = new Element[dependencies.size()];
                for (int i = 0; i < featureElems.length; i++){
                    featureElems[i] =  element(name("feature"), dependencies.get(i));
                }
                elements.add(element(name("features"),featureElems));
            } else if (goal.equals("install-apps")){
                elements.add(element(name("looseApplication"), "true"));
                elements.add(element(name("stripVersion"), "true"));
                elements.add(element(name("installAppPackages"), "project"));
                elements.add(element(name("configFile"), configFile.getAbsolutePath()));
            } else if (goal.equals("create-server")){
                elements.add(element(name("configFile"), configFile.getAbsolutePath()));
                if (assemblyArtifact != null){
                    Element[] featureElems = new Element[4];
                    featureElems[0] = element(name("groupId"), assemblyArtifact.getGroupId());
                    featureElems[1] = element(name("artifactId"), assemblyArtifact.getArtifactId());
                    featureElems[2] = element(name("version"), assemblyArtifact.getVersion());
                    featureElems[3] = element(name("type"), assemblyArtifact.getType());
                    elements.add(element(name("assemblyArtifact"), featureElems));
                }  
            }
        }
        return (Element[]) elements.toArray(new Element[elements.size()]);
    }

    private void runMojo(String plugin, String goal, String serverName, List<String> dependencies) throws MojoExecutionException {
        Plugin mavenPlugin = project.getPlugin(plugin);
        log.info("plugin version: " + mavenPlugin.getVersion());
        executeMojo(mavenPlugin, goal(goal), configuration(getPluginConfigurationElements(goal, serverName, dependencies)),
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
            }
            finally {
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
   
}
