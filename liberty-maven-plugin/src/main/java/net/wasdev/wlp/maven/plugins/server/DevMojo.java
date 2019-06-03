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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.Watchable;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import com.sun.nio.file.SensitivityWatchEventModifier;

import org.apache.commons.io.FileUtils;
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
import net.wasdev.wlp.maven.plugins.applications.InstallAppMojoSupport;

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

        public DevMojoUtil(List<String> jvmOptions, File serverDirectory) {
            super(jvmOptions, serverDirectory);
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
        
    }

    DevMojoUtil util;

    @Override
    protected void doExecute() throws Exception {

        // check default directory for war.xml (apps or dropins)
        boolean looseApp = false;
        File appsDestDir = new File(serverDirectory, "apps");
        File dropinsDestDir = new File(serverDirectory, "dropins");
        if (warXmlExists(appsDestDir) || warXmlExists(dropinsDestDir)){
            looseApp = true;
        }
        if (!looseApp){
            log.info("Installing the application in loose application mode.");
            runMojo("install-apps", serverName, null);
        }
        
        util = new DevMojoUtil(jvmOptions, serverDirectory);

        sourceDirectory = new File(sourceDirectoryString.trim());
        testSourceDirectory = new File(testSourceDirectoryString.trim());

        log.debug("Source directory: " + sourceDirectory);
        log.debug("Output directory: " + outputDirectory);
        log.debug("Test Source directory: " + testSourceDirectory);
        log.debug("Test Output directory: " + testOutputDirectory);

        // create an executor for tests with an additional queue of size 1, so any further changes detected mid-test will be in the following run
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(1, true));

        util.addShutdownHook(executor);

        if (skip) {
            return;
        }
        if (isInstall) {
            installServerAssembly();
        } else {
            log.info(MessageFormat.format(messages.getString("info.install.type.preexisting"), ""));
            checkServerHomeExists();
        }

        util.enableServerDebug(libertyDebugPort);

        startDefaultServer();

        HashMap<File, List<File>> files = new HashMap<File, List<File>>();

        // config files
        if (configDirectory == null) {
            configDirectory = configFile.getParentFile();
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

        // pom.xml dependencies
        List<Dependency> existingDependencies = project.getDependencies();
        
        // run tests at startup
        runTestThread(executor, null, null, -1);
                
        // src/main/java files
        Path srcPath = sourceDirectory.getAbsoluteFile().toPath(); 
        Path testSrcPath = testSourceDirectory.getAbsoluteFile().toPath();  
        Path configPath = configDirectory.getAbsoluteFile().toPath(); 
        
        // pom.xml
        File pom = project.getFile();
        String existingPom = readFile(pom);

        // collect artifacts absolute paths in order to build classpath
        Set<Artifact> artifacts = project.getArtifacts();
        List<String> artifactPaths = new ArrayList<String>();
        for (Artifact artifact : artifacts) {
            artifactPaths.add(artifact.getFile().getAbsolutePath());
        }
        
        // start watching files
        try (WatchService watcher = FileSystems.getDefault().newWatchService();) {
            
            registerAll(sourceDirectory.toPath(), srcPath, watcher);
            registerAll(testSourceDirectory.toPath(), testSrcPath, watcher);
            registerAll(configDirectory.toPath(), configPath, watcher);
            for (File resourceDir : resourceDirs) {
                registerAll(resourceDir.toPath(), resourceDir.getAbsoluteFile().toPath(), watcher);
            }
            
            pom.getParentFile().toPath().register(watcher, new WatchEvent.Kind[]{StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_CREATE}, SensitivityWatchEventModifier.HIGH);
            log.debug("Registering watchservice directory: " + pom.getParentFile().toPath());
            
            while (true) {
                final WatchKey wk = watcher.take();
                for (WatchEvent<?> event : wk.pollEvents()) {
                    final Path changed = (Path) event.context();

                    final Watchable watchable = wk.watchable();
                    final Path directory = (Path) watchable;
                    log.debug("Processing events for watched directory: " + directory);
                    
                    File fileChanged = new File(directory.toString(), changed.toString());
                    log.debug("Changed: " + changed + "; " + event.kind());
                    
                    // resource file check
                    File resourceParent = null;
                    for (File resourceDir : resourceDirs){
                        if (directory.startsWith(resourceDir.toPath())){
                            resourceParent = resourceDir;
                        }
                    }
                    
                    // src/main/java directory 
                    if (directory.startsWith(sourceDirectory.toPath())) {
                        ArrayList<File> javaFilesChanged = new ArrayList<File>();
                        javaFilesChanged.add(fileChanged);
                        if (fileChanged.exists() && fileChanged.getName().endsWith(".java") && (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY || event.kind() == StandardWatchEventKinds.ENTRY_CREATE)) {
                            log.debug("Java source file modified: " + fileChanged.getName());
                            recompileJavaSource(files, javaFilesChanged, artifactPaths, executor);
                        } else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE){
                            log.debug("Java file deleted: " + fileChanged.getName());
                            deleteJavaFile(fileChanged, outputDirectory, sourceDirectory);
                           
                        }
                    } else if (directory.startsWith(testSourceDirectory.toPath())) { // src/main/test
                        ArrayList<File> javaFilesChanged = new ArrayList<File>();
                        javaFilesChanged.add(fileChanged);
                        if (fileChanged.exists() && fileChanged.getName().endsWith(".java") && (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY || event.kind() == StandardWatchEventKinds.ENTRY_CREATE)) {
                            recompileJavaTest(files, javaFilesChanged, artifactPaths, executor);
                            
                        } else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) { 
                            log.debug("Java file deleted: " + fileChanged.getName());
                            deleteJavaFile(fileChanged, testOutputDirectory, testSourceDirectory);
                        }
                    } else if (directory.startsWith(configDirectory.toPath())) {  // config files                                                                    
                        if (fileChanged.exists() && (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY ||  event.kind() == StandardWatchEventKinds.ENTRY_CREATE)) {
                            copyFile(fileChanged, configDirectory, serverDirectory);
                        } else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE){
                            log.debug("Config file deleted: " + fileChanged.getName());
                            deleteFile(fileChanged, configDirectory, serverDirectory);
                        }
                    } else if (resourceParent != null && directory.startsWith(resourceParent.toPath())){ // resources
                        log.debug("Resource dir: " + resourceParent.toString());
                        log.debug("File within resource directory");
                        if (fileChanged.exists() && (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY ||  event.kind() == StandardWatchEventKinds.ENTRY_CREATE)) {
                            copyFile(fileChanged, resourceParent, outputDirectory);  
                        } else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                            log.debug("Resource file deleted: " + fileChanged.getName());
                            deleteFile(fileChanged, resourceParent, outputDirectory);
                        }
                    } else if (fileChanged.equals(pom) && directory.startsWith(pom.getParentFile().toPath()) && event.kind() == StandardWatchEventKinds.ENTRY_MODIFY){ // pom.xml
                        String modifiedPom = readFile(pom);
                        XMLUnit.setIgnoreWhitespace(true);
                        XMLUnit.setIgnoreAttributeOrder(true);
                        XMLUnit.setIgnoreComments(true);
                        DetailedDiff diff = new DetailedDiff(XMLUnit.compareXML(existingPom, modifiedPom));
                        List<?> allDifferences = diff.getAllDifferences();
                        log.debug("Number of differences in the pom: " + allDifferences.size());
                        
                        if (!allDifferences.isEmpty()){
                            log.info("Pom has been modified");
                            MavenProject updatedProject = loadProject(pom);
                            List<Dependency> dependencies = updatedProject.getDependencies();
                            log.debug("Dependencies size: " + dependencies.size());
                            log.debug("Existing dependencies size: " + existingDependencies.size());
      
                            List<String> dependencyIds = new ArrayList<String>();          
                            List<Artifact> updatedArtifacts = getNewDependencies(dependencies, existingDependencies);

                            if (!updatedArtifacts.isEmpty()){
                                for (Artifact artifact : updatedArtifacts){
                                    if (("esa").equals(artifact.getType())) {
                                        dependencyIds.add(artifact.getArtifactId());
                                    }
                                    
                                    org.eclipse.aether.artifact.Artifact aetherArtifact = new org.eclipse.aether.artifact.DefaultArtifact(artifact.getGroupId(),  artifact.getArtifactId(), artifact.getType(), artifact.getVersion());
                                    org.eclipse.aether.graph.Dependency dependency = new org.eclipse.aether.graph.Dependency(aetherArtifact, null, true);
                                    
                                    CollectRequest collectRequest = new CollectRequest();
                                    collectRequest.setRoot(dependency);
                                    collectRequest.setRepositories(repositories);
                                    
                                    List<String> addToClassPath = new ArrayList<String>();
                                    DependencyRequest depRequest = new DependencyRequest(collectRequest, null);
                                    try {
                                        DependencyResult dependencyResult = repositorySystem.resolveDependencies(repoSession, depRequest);
                                        org.eclipse.aether.graph.DependencyNode root = dependencyResult.getRoot();
                                        List<File> artifactsList = new ArrayList<File>();
                                        addArtifacts(root, artifactsList);
                                        for (File a : artifactsList) {
                                           log.debug("Artifact: " + a);
                                           if (a.getAbsolutePath().endsWith(".jar")){
                                               addToClassPath.add(a.getAbsolutePath());
                                           }
                                        }
                                    } catch (DependencyResolutionException e) {
                                        throw new MojoExecutionException(e.getMessage(), e);
                                    }
                                    artifactPaths.addAll(addToClassPath);
                                }
                                
                                if (!dependencyIds.isEmpty()){
                                    runMojo("install-feature", serverName, dependencyIds);
                                    dependencyIds.clear();
                                }
                                
                                // update dependencies  
                                existingDependencies = dependencies;
                                existingPom = modifiedPom;
                            } else {
                                log.info("Unexpected change detected in pom.xml.  Please restart liberty:dev mode.");
                            }
                        }
                    }
                }
                // reset the key
                boolean valid = wk.reset();
                if (!valid) {
                    log.info("WatchService key has been unregistered");
                }
            }
        }
    }
    
    private String readFile(File file) throws IOException {
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);

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
        
    private void copyFile(File fileChanged, File srcDir, File targetDir){
        try{
            String relPath = fileChanged.getAbsolutePath()
                    .substring(fileChanged.getAbsolutePath().indexOf(srcDir.getAbsolutePath())
                            + srcDir.getAbsolutePath().length());

            File targetResource = new File(targetDir.getAbsolutePath() + relPath);
            log.info("Copying file: " + fileChanged.getAbsolutePath() + " to: "
                    + targetResource.getAbsolutePath());
            FileUtils.copyFile(fileChanged, targetResource);
        } catch (IOException e) {
            log.error("Failed to copy file: " + e.toString());
        }
    }
    
    private void deleteJavaFile(File fileChanged, File classesDir, File compileSourceRoot){
        if (fileChanged.getName().endsWith(".java")){
            String fileName = fileChanged.getName().substring(0, fileChanged.getName().indexOf(".java"));
            File parentFile = fileChanged.getParentFile();
            String relPath = parentFile.getAbsolutePath()
                    .substring(parentFile.getAbsolutePath().indexOf(compileSourceRoot.getAbsolutePath())
                            + compileSourceRoot.getAbsolutePath().length())
                    + "/" + fileName + ".class";
            File targetFile = new File(classesDir.getAbsolutePath() + relPath);

            if (targetFile.exists()) {
                targetFile.delete();
                log.info("Java class deleted: " + targetFile.getAbsolutePath());
            }
        } else {
            log.debug("File deleted but was not a java file: " + fileChanged.getName());
        }
    }
    
    private void recompileJavaSource(HashMap<File, List<File>> files, List<File> javaFilesChanged, List<String> artifactPaths, ThreadPoolExecutor executor) throws Exception {
        recompileJava(files, javaFilesChanged, artifactPaths, executor, false);
    }

    private void recompileJavaTest(HashMap<File, List<File>> files, List<File> javaFilesChanged, List<String> artifactPaths, ThreadPoolExecutor executor) throws Exception {
        recompileJava(files, javaFilesChanged, artifactPaths, executor, true);
    }
    
    private void recompileJava(HashMap<File, List<File>> files, List<File> javaFilesChanged, List<String> artifactPaths, ThreadPoolExecutor executor, boolean tests) throws Exception {
        File logFile = null;
        String regexp = null;
        int messageOccurrences = -1;
        if (!(skipTests || skipITs)) {
            // before compiling source and running tests, check number of "application updated" messages
            logFile = serverTask.getLogFile();
            regexp = UPDATED_APP_MESSAGE_REGEXP + DevMojo.this.project.getArtifactId();
            messageOccurrences = serverTask.countStringOccurrencesInFile(regexp, logFile);
            log.debug("Message occurrences before compile: " + messageOccurrences);
        }

        // source root is src/main/java or src/test/java
        File classesDir = tests ? testOutputDirectory : outputDirectory;

        List<String> optionList = new ArrayList<>();
        List<File> outputDirs = new ArrayList<File>();
        
        if (tests){
            outputDirs.add(outputDirectory);
            outputDirs.add(testOutputDirectory);
        } else {
            outputDirs.add(outputDirectory);
        }
        Set<File> classPathElems = getClassPath(artifactPaths, outputDirs);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        fileManager.setLocation(StandardLocation.CLASS_PATH, classPathElems);
        fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singleton(classesDir));

        Iterable<? extends JavaFileObject> compilationUnits = fileManager
                .getJavaFileObjectsFromFiles(javaFilesChanged);

        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, optionList, null,
                compilationUnits);
        boolean didCompile = task.call();
        if (didCompile) {
            if (tests) {
                log.info("Tests compilation was successful.");
            } else {
                log.info("Source compilation was successful.");
            }

            // run tests after successful compile
            if (tests) {
                // if only tests were compiled, don't need to wait for app to update
                runTestThread(executor, null, null, -1);
            } else {
                runTestThread(executor, regexp, logFile, messageOccurrences);
            }
        } else {
            if (tests) {
                log.info("Tests compilation had errors.");
            } else {
                log.info("Source compilation had errors.");
            }
        }
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
        }
        return (Element[]) elements.toArray(new Element[elements.size()]);
    }

    private void runMojo(String goal, String serverName, List<String> dependencies) throws MojoExecutionException {
        Plugin libertyMavenPlugin = project.getPlugin("net.wasdev.wlp.maven.plugins:liberty-maven-plugin");
        log.info("plugin version: " + libertyMavenPlugin.getVersion());
        executeMojo(libertyMavenPlugin, goal(goal), configuration(getPluginConfigurationElements(goal, serverName, dependencies)),
                executionEnvironment(project, session, pluginManager));
    }


    
    private void startDefaultServer() throws Exception {
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
                String startMessage = serverTask.waitForStringInLog(START_APP_MESSAGE_REGEXP + archiveName, timeout,
                        serverTask.getLogFile());
                if (startMessage == null) {
                    util.stopServer();
                    throw new MojoExecutionException(
                            MessageFormat.format(messages.getString("error.server.start.verify"), verifyTimeout));
                }
                timeout = endTime - System.currentTimeMillis();
            }
        }
    }

    private void deleteFile (File deletedFile, File dir, File targetDir){
        log.debug("File that was deleted: " + deletedFile.getAbsolutePath());
        String relPath = deletedFile.getAbsolutePath().substring(
                deletedFile.getAbsolutePath().indexOf(dir.getAbsolutePath()) + dir.getAbsolutePath().length());
        File targetFile = new File(targetDir.getAbsolutePath() + relPath);
        log.debug("Target file exists: " + targetFile.exists());
        if (targetFile.exists()) {
            targetFile.delete();
            log.info("Deleted file: " + targetFile.getAbsolutePath());
        }
    }
    
    private Set<File> getClassPath(List<String> artifactPaths, List<File> outputDirs) {
        List<URL> urls = new ArrayList<>();
        ClassLoader c = Thread.currentThread().getContextClassLoader();
        while (c != null) {
            if (c instanceof URLClassLoader) {
                urls.addAll(Arrays.asList(((URLClassLoader) c).getURLs()));
            }
            c = c.getParent();
        }

        Set<String> parsedFiles = new HashSet<>();
        Deque<String> toParse = new ArrayDeque<>();
        for (URL url : urls) {
            toParse.add(new File(url.getPath()).getAbsolutePath());
        }

        for (String artifactPath : artifactPaths) {
            toParse.add(new File(artifactPath).getAbsolutePath());
        }

        Set<File> classPathElements = new HashSet<>();
        classPathElements.addAll(outputDirs);
        while (!toParse.isEmpty()) {
            String s = toParse.poll();
            if (!parsedFiles.contains(s)) {
                parsedFiles.add(s);
                File file = new File(s);
                if (file.exists() && file.getName().endsWith(".jar")) {
                    classPathElements.add(file);
                    if (!file.isDirectory() && file.getName().endsWith(".jar")) {
                        try (JarFile jar = new JarFile(file)) {
                            Manifest mf = jar.getManifest();
                            if (mf == null || mf.getMainAttributes() == null) {
                                continue;
                            }
                            Object classPath = mf.getMainAttributes().get(Attributes.Name.CLASS_PATH);
                            if (classPath != null) {
                                for (String i : classPath.toString().split(" ")) {
                                    File f;
                                    try {
                                        URL u = new URL(i);
                                        f = new File(u.getPath());
                                    } catch (MalformedURLException e) {
                                        f = new File(file.getParentFile(), i);
                                    }
                                    if (f.exists()) {
                                        toParse.add(f.getAbsolutePath());
                                    }
                                }
                            }
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to open class path file " + file, e);
                        }
                    }
                }
            }
        }
        return classPathElements;
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
    
    private void registerAll(final Path start, final Path dir, final WatchService watcher) throws IOException {
        // register directory and sub-directories
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                throws IOException {
                    log.debug("Registering watchservice directory: " + dir.toString());
                    dir.register(watcher, new WatchEvent.Kind[]{StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_CREATE}, SensitivityWatchEventModifier.HIGH);
                    return FileVisitResult.CONTINUE;
            }

        });

    }
    
    private boolean warXmlExists(File dir){
        boolean looseApp = false;
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".war.xml") && file.getName().startsWith(DevMojo.this.project.getArtifactId())) {
                    looseApp = true;
                    break;
                }
            }
        }
        return looseApp;
    }
   
}
