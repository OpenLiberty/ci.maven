/**
 * (C) Copyright IBM Corporation 2019, 2025.
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
import java.nio.file.Paths;
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

import io.openliberty.tools.common.plugins.util.LibertyPropFilesUtility;
import io.openliberty.tools.maven.utils.CommonLogger;
import org.apache.maven.artifact.Artifact;
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
import io.openliberty.tools.common.plugins.util.BinaryScannerUtil;
import io.openliberty.tools.common.plugins.util.DevUtil;
import io.openliberty.tools.common.plugins.util.InstallFeatureUtil;
import io.openliberty.tools.common.plugins.util.JavaCompilerOptions;
import io.openliberty.tools.common.plugins.util.PluginExecutionException;
import io.openliberty.tools.common.plugins.util.PluginScenarioException;
import io.openliberty.tools.common.plugins.util.ProjectModule;
import io.openliberty.tools.common.plugins.util.ServerFeatureUtil;
import io.openliberty.tools.common.plugins.util.ServerFeatureUtil.FeaturesPlatforms;
import io.openliberty.tools.common.plugins.util.ServerStatusUtil;
import io.openliberty.tools.maven.applications.DeployMojoSupport;
import io.openliberty.tools.maven.applications.LooseWarApplication;
import io.openliberty.tools.maven.utils.DevHelper;
import io.openliberty.tools.maven.utils.ExecuteMojoUtil;

/**
 * Start a liberty server in dev mode import to set ResolutionScope for TEST as
 * it helps build full transitive dependency classpath
 */
@Mojo(name = "dev", requiresDependencyCollection = ResolutionScope.TEST, requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
public class DevMojo extends LooseAppSupport {

    private static final String TEST_RUN_ID_PROPERTY_NAME = "liberty.dev.test.run.id";
    private static final String LIBERTY_HOSTNAME = "liberty.hostname";
    private static final String LIBERTY_HTTP_PORT = "liberty.http.port";
    private static final String LIBERTY_HTTPS_PORT = "liberty.https.port";
    private static final String MICROSHED_HOSTNAME = "microshed_hostname";
    private static final String MICROSHED_HTTP_PORT = "microshed_http_port";
    private static final String MICROSHED_HTTPS_PORT = "microshed_https_port";
    private static final String WLP_USER_DIR_PROPERTY_NAME = "wlp.user.dir";
    private static final String GEN_FEAT_LIBERTY_DEP_WARNING = "Liberty ESA feature dependencies were detected in the pom.xml file and automatic generation of features is [On]. "
            + "Automatic generation of features does not support Liberty ESA feature dependencies. "
            + "Remove any Liberty ESA feature dependencies from the pom.xml file or disable automatic generation of features by typing 'g' and press Enter.";

    DevMojoUtil util = null;

    @Parameter(property = "changeOnDemandTestsAction", defaultValue = "false")
    private boolean changeOnDemandTestsAction;

    @Parameter(property = "hotTests", defaultValue = "false")
    private boolean hotTests;

    @Parameter(property = "skipTests", defaultValue = "false")
    private boolean skipTests;

    @Parameter(property = "skipUTs", defaultValue = "false")
    private boolean skipUTs;

    @Parameter(property = "skipITs", defaultValue = "false")
    private boolean skipITs;
 
    /**
     * If set to `true`, the `install-feature` goal will be skipped when `dev` mode is started on an already existing Liberty runtime installation. 
     * It will also be skipped when `dev` mode is running and a restart of the server is triggered either directly by the user or by application changes. 
     * The `install-feature` goal will be invoked though when `dev` mode is running and a change to the configured features is detected. 
     * The default value is `false`.
     */
    @Parameter(property = "skipInstallFeature", defaultValue = "false")
    protected boolean skipInstallFeature;
 
    @Parameter(property = "debug", defaultValue = "true")
    private boolean libertyDebug;

    @Parameter(property = "debugPort", defaultValue = "7777")
    private int libertyDebugPort;

    @Parameter(property = "container", defaultValue = "false")
    private boolean container;

    @Parameter(property = "generateFeatures", defaultValue = "false")
    private boolean generateFeatures;

    /**
     * If generateToSrc is true, then create the file containing new features in the src directory
     * Otherwise, place the file in the target directory where the Liberty server is defined.
     */
    @Parameter(property = "generateToSrc", defaultValue = "false")
    private boolean generateToSrc;

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
     * Containerfile used to build an image to then start a container with
     */
    @Parameter(alias="containerfile", property = "containerfile")
    private File containerfile;

    @Parameter(alias="dockerfile", property = "dockerfile")
    public void setDockerfile(File dockerfile) {
        if (dockerfile != null && this.containerfile != null) {
            getLog().warn("Both containerfile and dockerfile have been set. Using containerfile value.");
        } else {
            this.containerfile = dockerfile;
        }
    }

    /**
     * Context (directory) to use for the build when building the container image
     */
    @Parameter(alias="containerBuildContext", property = "containerBuildContext")
    private File containerBuildContext;

    @Parameter(alias="dockerBuildContext", property = "dockerBuildContext")
    public void setDockerBuildContext(File dockerBuildContext) {
        if (dockerBuildContext != null && this.containerBuildContext != null) {
            getLog().warn("Both containerBuildContext and dockerBuildContext have been set. Using containerBuildContext value.");
        } else {
            this.containerBuildContext = dockerBuildContext;
        }
    }

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
     * Additional options for the container run command when dev mode starts a
     * container. Takes precedence over dockerRunOpts.
     */
    @Parameter(alias="containerRunOpts", property = "containerRunOpts")
    private String containerRunOpts;

    @Parameter(alias="dockerRunOpts", property = "dockerRunOpts")
    public void setDockerRunOpts(String dockerRunOpts) {
        if (dockerRunOpts != null && this.containerRunOpts != null) {
            getLog().warn("Both containerRunopts and dockerRunOpts have been set. Using containerRunOpts value.");
        } else {
            this.containerRunOpts = dockerRunOpts;
        }
    }

    /**
     * Specify the amount of time in seconds that dev mode waits for the container
     * build command to run to completion. Default to 600 seconds.
     */
    @Parameter(alias="containerBuildTimeout", property = "containerBuildTimeout", defaultValue = "600")
    private int containerBuildTimeout;

    @Parameter(alias="dockerBuildTimeout", property = "dockerBuildTimeout")
    public void setDockerBuildTimeout(int dockerBuildTimeout) {
        if (dockerBuildTimeout != 600 && this.containerBuildTimeout != 600) {
            getLog().warn("Both containerBuildTimeout and dockerBuildTimeout have been set. Using containerBuildTimeout value.");
        } else if (dockerBuildTimeout != 600) {
            this.containerBuildTimeout = dockerBuildTimeout;
        }
    }

    /**
     * If true, the default container port mappings are skipped in the container run
     * command
     */
    @Parameter(property = "skipDefaultPorts", defaultValue = "false")
    private boolean skipDefaultPorts;

    /**
     * If true, preserve the temporary Containerfile/Dockerfile used in the container build command
     */
    private Boolean keepTempContainerfile;

    @Parameter(alias="keepTempContainerfile", property = "keepTempContainerfile")
    public void setKeepTempContainerfile(Boolean keepTempContainerfile) {
        this.keepTempContainerfile = keepTempContainerfile;
    }

    @Parameter(alias="keepTempDockerfile", property = "keepTempDockerfile", defaultValue = "false")
    public void setKeepTempDockerfile(Boolean keepTempDockerfile) {
        if (this.keepTempContainerfile == null) {
            setKeepTempContainerfile(keepTempDockerfile);
        }
    }
    
    private boolean isExplodedLooseWarApp = false;
    private boolean isNewInstallation = true;
    private static Map<String,Boolean> compileMojoError = new HashMap<>();

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
            getLog().debug("No resource directory detected, using default directory: " + defaultResourceDir);
            resourceDirs.add(defaultResourceDir);
        }
        return resourceDirs;
    }

    private class DevMojoUtil extends DevUtil {
        Set<String> existingFeatures;
        Set<String> existingPlatforms;
        Map<String, File> libertyDirPropertyFiles = new HashMap<String, File>();
        List<MavenProject> upstreamMavenProjects;

        public DevMojoUtil(File installDir, File userDir, File serverDirectory, File sourceDirectory,
                           File testSourceDirectory, File configDirectory, File projectDirectory, File multiModuleProjectDirectory,
                           List<File> resourceDirs, JavaCompilerOptions compilerOptions, String mavenCacheLocation,
                           List<ProjectModule> upstreamProjects, List<MavenProject> upstreamMavenProjects, boolean recompileDeps,
                           File pom, Map<String, List<String>> parentPoms, boolean generateFeatures, boolean generateToSrc, boolean skipInstallFeature,
                           Set<String> compileArtifactPaths, Set<String> testArtifactPaths, List<Path> webResourceDirs, File serverOutputDirectory) throws IOException, PluginExecutionException {
            super(new File(project.getBuild().getDirectory()), serverDirectory, sourceDirectory, testSourceDirectory,
                    configDirectory, projectDirectory, multiModuleProjectDirectory, resourceDirs, changeOnDemandTestsAction, hotTests, skipTests,
                    skipUTs, skipITs, skipInstallFeature, project.getArtifactId(), serverStartTimeout, verifyTimeout, verifyTimeout,
                    ((long) (compileWait * 1000L)), libertyDebug, false, false, pollingTest, container, containerfile,
                    containerBuildContext, containerRunOpts, containerBuildTimeout, skipDefaultPorts, compilerOptions,
                    keepTempContainerfile, mavenCacheLocation, upstreamProjects, recompileDeps, project.getPackaging(),
                    pom, parentPoms, generateFeatures, generateToSrc, compileArtifactPaths, testArtifactPaths, webResourceDirs, compileMojoError);

            this.libertyDirPropertyFiles = LibertyPropFilesUtility.getLibertyDirectoryPropertyFiles(new CommonLogger(getLog()), installDir, userDir,
                    serverDirectory, serverOutputDirectory);
            ServerFeatureUtil servUtil = getServerFeatureUtil(true, libertyDirPropertyFiles);  
            FeaturesPlatforms fp = servUtil.getServerFeatures(serverDirectory, libertyDirPropertyFiles);
            if (fp != null) {
            	this.existingFeatures = fp.getFeatures();
            	this.existingPlatforms = fp.getPlatforms();
            }
            this.upstreamMavenProjects = upstreamMavenProjects;

            setContainerEngine(this);
        }

        @Override
        public void debug(String msg) {
            getLog().debug(msg);
        }

        @Override
        public void debug(String msg, Throwable e) {
            getLog().debug(msg, e);
        }

        @Override
        public void debug(Throwable e) {
            getLog().debug(e);
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
        public void error(String msg) {
            getLog().error(msg);
        }

        @Override
        public void error(String msg, Throwable e) {
            getLog().error(msg, e);
        }

        @Override
        public boolean isDebugEnabled() {
            return getLog().isDebugEnabled();
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
                    getLog().info("Running boost:package");
                    runBoostMojo("package");
                } else {
                    runLibertyMojoCreate();
                }
            } catch (MojoExecutionException e) {
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
                    getLog().error(e.getMessage() + ".\nDisabling the automatic generation of features.");
                    setFeatureGeneration(false);
                } else {
                    getLog().error(e.getMessage()
                    + "\nTo disable the automatic generation of features, type 'g' and press Enter.");
                }
                return false;
            }
        }

        @Override
        public void libertyInstallFeature() throws PluginExecutionException {
            try {
                runLibertyMojoInstallFeature(null, null, container ? super.getContainerName() : null);
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
            compileMojoError.clear();
            if (container) {
                // TODO stop the container instead
                return;
            }
            try {
                ServerTask serverTask = initializeJava();
                serverTask.setOperation("stop");
                serverTask.execute();
            } catch (Exception e) {
                getLog().warn(MessageFormat.format(messages.getString("warn.server.stopped"), serverName));
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

        private static final String LIBERTY_RUNTIME_PROP = "liberty.runtime.";
        private static final String LIBERTY_BOOTSTRAP_PROP = "liberty.bootstrap.";
        private static final String LIBERTY_JVM_PROP = "liberty.jvm.";
        private static final String LIBERTY_ENV_PROP = "liberty.env.";
        private static final String LIBERTY_VAR_PROP = "liberty.var.";
        private static final String LIBERTY_DEFAULT_VAR_PROP = "liberty.defaultVar.";

        private boolean hasInstallationPropChanged(MavenProject project, MavenProject backupProject) {
            Properties projProp = project.getProperties();
            Properties backupProjProp = backupProject.getProperties();

            if (!Objects.equals(getPropertiesWithKeyPrefix(projProp, LIBERTY_RUNTIME_PROP),
                    getPropertiesWithKeyPrefix(backupProjProp, LIBERTY_RUNTIME_PROP))) {
                return true;
            }

            return false;
        }

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

        private boolean hasInstallationConfigChanged(Xpp3Dom config, Xpp3Dom oldConfig) {
            if (!Objects.equals(config.getChild("installDirectory"), oldConfig.getChild("installDirectory"))) {
                return true;
            } else if (!Objects.equals(config.getChild("runtimeArchive"), oldConfig.getChild("runtimeArchive"))) {
                return true;
            } else if (!Objects.equals(config.getChild("runtimeArtifact"), oldConfig.getChild("runtimeArtifact"))) {
                return true;
            } else if (!Objects.equals(config.getChild("assemblyArchive"), oldConfig.getChild("assemblyArchive"))) {
                return true;
            } else if (!Objects.equals(config.getChild("assemblyArtifact"), oldConfig.getChild("assemblyArtifact"))) {
                return true;
            } else if (!Objects.equals(config.getChild("libertyRuntimeGroupId"), oldConfig.getChild("libertyRuntimeGroupId"))) {
                return true;
            } else if (!Objects.equals(config.getChild("libertyRuntimeArtifactId"), oldConfig.getChild("libertyRuntimeArtifactId"))) {
                return true;
            } else if (!Objects.equals(config.getChild("libertyRuntimeVersion"), oldConfig.getChild("libertyRuntimeVersion"))) {
                return true;
            } else if (!Objects.equals(config.getChild("serverName"), oldConfig.getChild("serverName"))) {
                return true;
            } else if (!Objects.equals(config.getChild("userDirectory"), oldConfig.getChild("userDirectory"))) {
                return true;
            } else if (!Objects.equals(config.getChild("outputDirectory"), oldConfig.getChild("outputDirectory"))) {
                return true;
            } else if (!Objects.equals(config.getChild("runtimeInstallDirectory"), oldConfig.getChild("runtimeInstallDirectory"))) {
                return true;
            } else if (!Objects.equals(config.getChild("assemblyInstallDirectory"), oldConfig.getChild("assemblyInstallDirectory"))) {
                return true;
            } else if (!Objects.equals(config.getChild("install"), oldConfig.getChild("install"))) {
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
                    getLog().debug("Maven compiler options have been modified: " + compilerOptions.getOptions());
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
                            getLog().debug("Detected a change in the compile dependencies for "
                                    + buildFile + " , regenerating features");
                            boolean generateFeaturesSuccess = libertyGenerateFeatures(null, true);
                            if (generateFeaturesSuccess) {
                                util.getJavaSourceClassPaths().clear();
                            }
                            // install new generated features, will not trigger install-feature if the feature list has not changed
                            util.installFeaturesToTempDir(generatedFeaturesFile, configDirectory, null,
                                generateFeaturesSuccess);
                        }
                        runLibertyMojoDeploy();
                    }
                }
            } catch (ProjectBuildingException | DependencyResolutionRequiredException | IOException
                    | MojoExecutionException e) {
                getLog().error("An unexpected error occurred while processing changes in " + buildFile.getAbsolutePath()
                        + ": " + e.getMessage());
                getLog().debug(e);
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
                getLog().error("An unexpected error occurred while processing changes in " + buildFile.getAbsolutePath()
                        + ": " + e.getMessage());
                getLog().debug(e);
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
            MavenProject builtProject = build.getProject();
            updateUpstreamProjectsArtifactPathToOutputDirectory(builtProject);
            return builtProject;
        }

        /**
         * From the project we're running dev mode from, get the artifact representing each of the upstream modules
         * and make sure the artifact File is associated to the build output (target/classes, not the .m2 repo).
         * 
         * Because of the way we dynamically build new model objects we need to do this from a given model's perspective.
         * 
         * @param startingProject 
         */
        private void updateUpstreamProjectsArtifactPathToOutputDirectory(MavenProject startingProject){
            Map<String,Artifact> artifactMap = startingProject.getArtifactMap();
            for (MavenProject p : upstreamMavenProjects) {
                Artifact projArtifact = artifactMap.get(p.getGroupId() + ":" + p.getArtifactId());
                if (projArtifact != null) {
                	updateArtifactPathToOutputDirectory(p, projArtifact);
                }
            }
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
                        if (!validatePluginVersion(warPlugin.getVersion(), "3.3.2")) {
                            getLog().warn(
                                    "Exploded WAR functionality is enabled. Please use maven-war-plugin version 3.3.2 or greater for best results.");
                        }
                        
                        redeployApp();
                    } else {
                        try {
                            runExplodedMojo();
                        } catch (MojoExecutionException e) {
                            getLog().error("Failed to run war:exploded goal", e);
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
                    getLog().error("Failed to run goal(s)", e);
                }
            } 
        }

        @Override
        protected void resourceModifiedOrCreated(File fileChanged, File resourceParent, File outputDirectory) throws IOException {
            /**
             * There is an asymmetry here that we take advantage of in the exploded case. For multi-mod, this would be a copyFile, which
             * does not apply Maven filters.
             */
            try {
                runMojo("org.apache.maven.plugins", "maven-resources-plugin", "resources");
            } catch (MojoExecutionException e) {
                getLog().error("Failed to run goal(s)", e);
            }
        }

        @Override
        protected void resourceDeleted(File fileChanged, File resourceParent, File outputDirectory) throws IOException {

            /**
             * Why is this so asymmetric compared to resourceModifiedOrCreated() above? For two reasons: 1. The resources:resources plugin
             * goal doesn't update the target/output directory with deletions, so we have to use our own custom deleteFile() method 2. In
             * the case of the exploded loose app format, even having deleted the file from the outputDirectory ('target/classes'), the
             * resource would typically also have been collected into the exploded 'webapp' directory. Even though it would take precedence
             * in 'target/classes' when it ends up in both locations, it will still be present in the 'webapp' directory. So we re-run the
             * exploded goal to force an "outdated" update cleaning this file from this location. Another approach might have been to do a
             * delteFile() in the 'webapp' directory.
             */

            deleteFile(fileChanged, resourceParent, outputDirectory, null);
            if (project.getPackaging().equals("war") && LooseWarApplication.isExploded(project)) {
                try {
                    runExplodedMojo();
                } catch (MojoExecutionException e) {
                    getLog().error("Failed to run goal(s)", e);
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
            boolean reinstallLiberty = false; // if this gets set to true, need to throw PluginExecutionException so user can run 'clean'
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
                getLog().error("Could not parse pom.xml. " + e.getMessage());
                getLog().debug(e);
                return false;
            }

            // set the updated project in current session;
            Plugin backupLibertyPlugin = getLibertyPlugin();
            Plugin backupWarPlugin = getPluginForProject("org.apache.maven.plugins", "maven-war-plugin", project);
            MavenProject backupProject = project;
            project = build.getProject();
            session.setCurrentProject(project);
            Plugin libertyPlugin = getLibertyPlugin();
            Plugin warPlugin = getPluginForProject("org.apache.maven.plugins", "maven-war-plugin", project);

            try {
                // TODO rebuild the corresponding module if the compiler options have changed
                JavaCompilerOptions oldCompilerOptions = getMavenCompilerOptions(backupProject);
                JavaCompilerOptions compilerOptions = getMavenCompilerOptions(project);
                if (!oldCompilerOptions.getOptions().equals(compilerOptions.getOptions())) {
                    getLog().debug("Maven compiler options have been modified: " + compilerOptions.getOptions());
                    util.updateJavaCompilerOptions(compilerOptions);
                }

                // Monitoring liberty properties in the pom.xml
                if (hasInstallationPropChanged(project, backupProject)) {
                    // Note that a change in installation config values requires a restart of dev mode. 
                    reinstallLiberty = true;
                    getLog().error("A change in Liberty runtime installation configuration requires a restart of dev mode. Stopping dev mode.");
                }
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
                    config = ExecuteMojoUtil.getPluginGoalConfig(libertyPlugin, "create", getLog());
                    oldConfig = ExecuteMojoUtil.getPluginGoalConfig(backupLibertyPlugin, "create", getLog());
                    if (!Objects.equals(config, oldConfig)) {
                        createServer = true;
                        if (restartForLibertyMojoConfigChanged(config, oldConfig)) {
                            restartServer = true;
                        }
                        if (hasInstallationConfigChanged(config, oldConfig)) {
                            // Note that a change in installation config values requires a restart of dev mode. 
                            reinstallLiberty = true;
                            getLog().error("A change in Liberty runtime installation configuration requires a restart of dev mode. Stopping dev mode.");
                        }
                    }
                }
                config = ExecuteMojoUtil.getPluginGoalConfig(libertyPlugin, "install-feature", getLog());
                oldConfig = ExecuteMojoUtil.getPluginGoalConfig(backupLibertyPlugin, "install-feature", getLog());
                if (!Objects.equals(config, oldConfig)) {
                    installFeature = true;
                }
                config = ExecuteMojoUtil.getPluginGoalConfig(libertyPlugin, "deploy", getLog());
                oldConfig = ExecuteMojoUtil.getPluginGoalConfig(backupLibertyPlugin, "deploy", getLog());
                if (!Objects.equals(config, oldConfig)) {
                    redeployApp = true;
                }
                config = ExecuteMojoUtil.getPluginGoalConfig(warPlugin, "exploded", getLog());
                oldConfig = ExecuteMojoUtil.getPluginGoalConfig(backupWarPlugin, "exploded", getLog());
                if (!Objects.equals(config, oldConfig) || !warPlugin.getVersion().equals(backupWarPlugin.getVersion())) {
                    redeployApp = true;
                }
                config = ExecuteMojoUtil.getPluginGoalConfig(libertyPlugin, "generate-features", getLog());
                oldConfig = ExecuteMojoUtil.getPluginGoalConfig(backupLibertyPlugin, "generate-features", getLog());
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
                        // adding or removing compile dependencies (including version changes) will need
                        // to deploy loose app again to remove or add or update embedded libraries in
                        // the loose app
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

                boolean generateFeaturesSuccess = false;
                if (optimizeGenerateFeatures && generateFeatures) {
                    getLog().debug("Detected a change in the compile dependencies, regenerating features");
                    // always optimize generate features on dependency change
                    generateFeaturesSuccess = libertyGenerateFeatures(null, true);
                    if (generateFeaturesSuccess) {
                        util.getJavaSourceClassPaths().clear();
                    } else {
                        installFeature = false; // skip installing features if generate features fails
                    }

                }
                
                // We don't currently have the ability to dynamically add new directories to be watched
                // There is so much that we are dynamically able to do that this could be surprising.
                // For now issue a warning
                Set<Path> oldMonitoredWebResourceDirs = new HashSet<Path>(this.monitoredWebResourceDirs);
                Set<Path> newMonitoredWebResourceDirs = new HashSet<Path>(LooseWarApplication.getWebSourceDirectoriesToMonitor(project));
                if (!oldMonitoredWebResourceDirs.equals(newMonitoredWebResourceDirs)) {
                    getLog().warn("Change detected in the set of filtered web resource directories, since dev mode was first launched.  Adding/deleting a web resource directory has no change on the set of directories monitored by dev mode.  Changing the watch list will require a dev mode restart");
                }
                
                // convert to Path, which seems to offer more reliable cross-platform, relative path comparison, then compare
                Set<Path> oldResourceDirs = new HashSet<Path>();
                this.resourceDirs.forEach(r -> oldResourceDirs.add(r.toPath()));                
                Set<Path> newResourceDirs = new HashSet<Path>();
                project.getResources().forEach(r -> newResourceDirs.add(Paths.get(r.getDirectory())));
                if (!oldResourceDirs.equals(newResourceDirs)) {
                    getLog().warn("Change detected in the set of resource directories, since dev mode was first launched. Adding/deleting a resource directory has no change on the set of directories monitored by dev mode.  Changing the watch list will require a dev mode restart");
                }                
                
                if (reinstallLiberty) {
                    project = backupProject;
                    session.setCurrentProject(backupProject);
                    util.stopServer();
                    throw new PluginExecutionException("A change in Liberty runtime installation configuration requires a restart of dev mode. Please run the 'dev' goal again for the change to take effect.");
                } else if (restartServer) {
                    // - stop Server
                    // - create server or runBoostMojo
                    // - install feature
                    // - deploy app
                    // - start server
                    util.restartServer();
                    return true;
                } else {
                    if (isUsingBoost() && (createServer || runBoostPackage)) {
                        getLog().info("Running boost:package");
                        runBoostMojo("package");
                    } else if (createServer) {
                        runLibertyMojoCreate();
                    } else if (redeployApp) {
                        util.installFeaturesToTempDir(generatedFeaturesFile, configDirectory, null,
                                generateFeaturesSuccess);
                        runLibertyMojoDeploy();
                    }
                    if (installFeature) {
                        runLibertyMojoInstallFeature(null, null, super.getContainerName());
                    }
                }
                if (!(reinstallLiberty || restartServer || createServer || redeployApp || installFeature || runBoostPackage)) {
                    // pom.xml is changed but not affecting liberty:dev mode. return true with the
                    // updated project set in the session
                    getLog().debug("changes in the pom.xml are not monitored by dev mode");
                    return true;
                }
            } catch (MojoExecutionException | DependencyResolutionRequiredException | IOException e) {
                getLog().error("An unexpected error occurred while processing changes in pom.xml. " + e.getMessage());
                if (installFeature) {
                    libertyDependencyWarning(generateFeatures, e);
                }
                getLog().debug(e);
                project = backupProject;
                session.setCurrentProject(backupProject);
                return false;
            }
            return true;
        }

        // check if generateFeatures is enabled and install feature failed. If Liberty dependencies
        // are in the build file display warning
        private void libertyDependencyWarning(boolean generateFeatures, Exception e) {
            if (generateFeatures && !getEsaDependency(project.getDependencies()).isEmpty()
                    && e.getMessage().contains(InstallFeatureUtil.CONFLICT_MESSAGE)) {
                getLog().warn(GEN_FEAT_LIBERTY_DEP_WARNING);
            }
        }

        @Override
        public void installFeatures(File configFile, File serverDir, boolean generateFeatures) {
            try {
                ServerFeatureUtil servUtil = getServerFeatureUtil(true, libertyDirPropertyFiles);
                FeaturesPlatforms fp = servUtil.getServerFeatures(serverDir, libertyDirPropertyFiles);
                Set<String> features = null;
                if (fp != null) {
                	features = fp.getFeatures();
                }
                if (features != null) {
                    Set<String> featuresCopy = new HashSet<String>(features);

                    if (existingFeatures != null) {
                        features.removeAll(existingFeatures);
                        // check if features have been removed
                        Set<String> existingFeaturesCopy = new HashSet<String>(existingFeatures);
                        existingFeaturesCopy.removeAll(featuresCopy);
                        if (!existingFeaturesCopy.isEmpty()) {
                            getLog().info("Configuration features have been removed: " + existingFeaturesCopy);
                        }
                    }

                    // check if features have been added and install new features
                    if (!features.isEmpty()) {
                        getLog().info("Configuration features have been added: " + features);
                        // pass all new features to install-feature as backup in case the serverDir cannot be accessed
                        Element[] featureElems = new Element[features.size() + 1];
                        featureElems[0] = element(name("acceptLicense"), "true");
                        String[] values = features.toArray(new String[features.size()]);
                        for (int i = 0; i < features.size(); i++) {
                            featureElems[i + 1] = element(name("feature"), values[i]);
                        }
                        runLibertyMojoInstallFeature(element(name("features"), featureElems), serverDir, super.getContainerName());
                    }
                }
            } catch (MojoExecutionException e) {
                getLog().error("Failed to install features from configuration file", e);
                libertyDependencyWarning(generateFeatures, e);
            }
        }

        @Override
        public ServerFeatureUtil getServerFeatureUtilObj() {
            // suppress logs from ServerFeatureUtil so that dev console is not flooded
            return getServerFeatureUtil(true, libertyDirPropertyFiles);
        }

        @Override
        public Set<String> getExistingFeatures() {
            return this.existingFeatures;
        }

        @Override
        public void updateExistingFeatures() {
            ServerFeatureUtil servUtil = getServerFeatureUtil(true, libertyDirPropertyFiles);
            FeaturesPlatforms fp = servUtil.getServerFeatures(serverDirectory, libertyDirPropertyFiles);
            Set<String> features = new HashSet<String>();
            Set<String> platforms = new HashSet<String>();
            if (fp != null) {
            	features = fp.getFeatures();
            	platforms = fp.getPlatforms();
            }
            existingFeatures = features;
            existingPlatforms = platforms;
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
                getLog().error("Unable to compile", e);
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
                getLog().error("Unable to compile", e);
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

    private void doDevMode() throws MojoExecutionException {
        String mvnVersion = runtime.getMavenVersion();
        getLog().debug("Maven version: " + mvnVersion);
        // Maven 3.8.2 and 3.8.3 contain a bug where compile artifacts are not resolved
        // correctly in threads. Block dev mode from running on these versions
        if (mvnVersion.equals("3.8.2") || mvnVersion.equals("3.8.3")) {
            throw new MojoExecutionException("Detected Maven version " + mvnVersion
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
        	
        	// In a multi-module build, dev mode will only be run on one project (the farthest downstream) and compile will
        	// be run on any relative upstream projects. If this current project in the Maven Reactor is not one of those projects, skip it.  
        	boolean skipJars = true;
        	if("spring-boot-project".equals(getDeployPackages())) {
        		skipJars = false;
        	}
        	List<MavenProject> relevantProjects = getRelevantMultiModuleProjects(graph, skipJars);
        	if (!relevantProjects.contains(project)) {
        		getLog().info("\nSkipping module " + project.getArtifactId() + " which is not included in this invocation of dev mode.\n");
        		return;
        	}

            List<MavenProject> downstreamProjects = graph.getDownstreamProjects(project, true);

            if (!downstreamProjects.isEmpty()) {
                getLog().debug("Downstream projects: " + downstreamProjects);
                if (isEar) {
                    runMojo("org.apache.maven.plugins", "maven-ear-plugin", "generate-application-xml");
                    runMojo("org.apache.maven.plugins", "maven-resources-plugin", "resources");
                    getOrCreateEarArtifact(project);
                } else if (project.getPackaging().equals("pom")) {
                    getLog().debug("Skipping compile/resources on module with pom packaging type");
                } else {
                    runMojo("org.apache.maven.plugins", "maven-resources-plugin", "resources");
                    try {
                        runCompileMojoLogWarningWithException("compile");
                    } catch (MojoExecutionException e) {
                        // set init recompile necessary in case any module fail
                        compileMojoError.put(project.getName(),Boolean.TRUE);
                    }
                    if(hotTests) {
                        try {
                            runCompileMojoLogWarningWithException("testCompile");
                        } catch (MojoExecutionException e) {
                            compileMojoError.put(project.getName(),Boolean.TRUE);
                        }
                    }
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
                getLog().debug(
                        "The recompileDependencies parameter was not explicitly set. The default value -DrecompileDependencies=false will be used.");
                recompileDependencies = "false";
            } else {
                // multi module project default to true
                getLog().debug(
                        "The recompileDependencies parameter was not explicitly set. The default value for multi module projects -DrecompileDependencies=true will be used.");
                recompileDependencies = "true";
            }
        }
        boolean recompileDeps = Boolean.parseBoolean(recompileDependencies);
        if (recompileDeps) {
            if (!upstreamMavenProjects.isEmpty()) {
                getLog().info("The recompileDependencies parameter is set to \"true\". On a file change all dependent modules will be recompiled.");
            } else {
                getLog().info("The recompileDependencies parameter is set to \"true\". On a file change the entire project will be recompiled.");
            }
        } else {
            getLog().info("The recompileDependencies parameter is set to \"false\". On a file change only the affected classes will be recompiled.");
        }

        // Check if this is a Boost application
        boostPlugin = project.getPlugin("org.microshed.boost:boost-maven-plugin");

        processContainerParams();

        if (serverDirectory.exists()) {
            if (ServerStatusUtil.isServerRunning(installDirectory, super.outputDirectory, serverName)) {
                if (!container) {
                    throw new MojoExecutionException("The server " + serverName
                        + " is already running. Terminate all instances of the server before starting dev mode."
                        + " You can stop a server instance with the command 'mvn liberty:stop'.");
                } else {
                    getLog().warn("Running server detected, which could cause unexpected results. To terminate the local running server, run the command 'mvn liberty:stop'.  Also, the warning may occur because a previous server execution did not stop cleanly, in which case you may want to run 'mvn clean' before re-running");
                }
            }
        }

        // create an executor for tests with an additional queue of size 1, so
        // any further changes detected mid-test will be in the following run
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(1, true));

        sourceDirectory = new File(sourceDirectoryString.trim());
        testSourceDirectory = new File(testSourceDirectoryString.trim());

        ArrayList<File> javaFiles = new ArrayList<File>();
        listFiles(sourceDirectory, javaFiles, "java");

        ArrayList<File> javaTestFiles = new ArrayList<File>();
        listFiles(testSourceDirectory, javaTestFiles, "java");

        getLog().debug("Source directory: " + sourceDirectory);
        getLog().debug("Output directory: " + outputDirectory);
        getLog().debug("Test Source directory: " + testSourceDirectory);
        getLog().debug("Test Output directory: " + testOutputDirectory);

        if (isEar) {
            runMojo("org.apache.maven.plugins", "maven-ear-plugin", "generate-application-xml");
            runMojo("org.apache.maven.plugins", "maven-resources-plugin", "resources");
            runMojo("org.apache.maven.plugins", "maven-resources-plugin", "testResources");
            if(!javaTestFiles.isEmpty()) {
                // for test classes in ear
                try {
                    runCompileMojoLogWarningWithException("testCompile");
                } catch (MojoExecutionException e) {
                    compileMojoError.put(project.getName(), Boolean.TRUE);
                }
            }
        } else if (project.getPackaging().equals("pom")) {
            getLog().debug("Skipping compile/resources on module with pom packaging type");
        } else {
            runMojo("org.apache.maven.plugins", "maven-resources-plugin", "resources");
            try {
                runCompileMojoLogWarningWithException("compile");
            } catch (MojoExecutionException e) {
                compileMojoError.put(project.getName(),Boolean.TRUE);
            }
            runMojo("org.apache.maven.plugins", "maven-resources-plugin", "testResources");
            if(!javaTestFiles.isEmpty()) {
                try {
                    runCompileMojoLogWarningWithException("testCompile");
                } catch (MojoExecutionException e) {
                    compileMojoError.put(project.getName(), Boolean.TRUE);
                }
            }
        }



        if (isUsingBoost()) {
            getLog().info("Running boost:package");
            runBoostMojo("package");
        } else {
            // If generate features to server directory then create server first.
            if (generateFeatures) {
                if (generateToSrc) {
                    generateFeaturesOnStartup();
                    runLibertyMojoCreate();
                } else {
                    runLibertyMojoCreate();
                    generateFeaturesOnStartup();
                }
            } else {
                runLibertyMojoCreate();
            }
            // If non-container, install features before starting server. Otherwise, user
            // should have "RUN features.sh" in their Containerfile/Dockerfile if they want features to be
            // installed.
            // Added check here for the new skip install feature parameter. 
            // Need to also check if this is a new Liberty installation or not. The isNewInstallation flag is set by runLibertyMojoCreate.
            if (!container && (!skipInstallFeature || isNewInstallation)) {
                runLibertyMojoInstallFeature(null, null, null);
            } else if (skipInstallFeature) {
                getLog().info("Skipping installation of features due to skipInstallFeature configuration.");
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
                    getLog().warn("Exploded WAR functionality is enabled. Please use maven-war-plugin version 3.3.2 or greater for best results.");
                }
            }
        }
        
        // resource directories
        List<File> resourceDirs = getResourceDirectories(project, outputDirectory);
        
        List<Path> webResourceDirs = LooseWarApplication.getWebSourceDirectoriesToMonitor(project);

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
                Xpp3Dom config = ExecuteMojoUtil.getPluginGoalConfig(libertyPlugin, "dev", getLog());
                
                boolean upstreamSkipTests = DevHelper.getBooleanFlag(config, userProps, props, "skipTests");
                boolean upstreamSkipITs = DevHelper.getBooleanFlag(config, userProps, props, "skipITs");
                boolean upstreamSkipUTs = DevHelper.getBooleanFlag(config, userProps, props, "skipUTs");

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

        try {
            // collect artifacts canonical paths in order to build classpath
            Set<String> compileArtifactPaths = new HashSet<String>(project.getCompileClasspathElements());
            Set<String> testArtifactPaths = new HashSet<String>(project.getTestClasspathElements());

            util = new DevMojoUtil(installDirectory, userDirectory, serverDirectory, sourceDirectory, testSourceDirectory,
                    configDirectory, project.getBasedir(), multiModuleProjectDirectory, resourceDirs, compilerOptions,
                    settings.getLocalRepository(), upstreamProjects, upstreamMavenProjects, recompileDeps, pom, parentPoms, 
                    generateFeatures, generateToSrc, skipInstallFeature, compileArtifactPaths, testArtifactPaths, webResourceDirs, new File(super.outputDirectory,serverName));
        } catch (IOException | PluginExecutionException |DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Error initializing dev mode.", e);
        }

        util.addShutdownHook(executor);
        
        try {
            util.startServer();
        } catch (PluginExecutionException e) {
            throw new MojoExecutionException("Error starting the server in dev mode.", e);
        }

        // start watching for keypresses immediately
        util.runHotkeyReaderThread(executor);

        // Note that serverXmlFile can be null. DevUtil will automatically watch
        // all files in the configDirectory,
        // which is where the server.xml is located if a specific serverXmlFile
        // configuration parameter is not specified.
        try {
            util.watchFiles(outputDirectory, testOutputDirectory, executor, serverXmlFile, bootstrapPropertiesFile,
                    jvmOptionsFile);
        } catch (Exception e) {
            if (e.getMessage() != null) {
                // a proper message is included in the exception if the server has been stopped
                // by another process
                getLog().info(e.getMessage());
            }
            return; // enter shutdown hook
        }
    }

    private void generateFeaturesOnStartup() throws MojoExecutionException {
        // generate features on startup - provide all classes and only user specified
        // features to binary scanner
        try {
            String generatedFileCanonicalPath;
            try {
                generatedFileCanonicalPath = new File(configDirectory,
                        BinaryScannerUtil.GENERATED_FEATURES_FILE_PATH).getCanonicalPath();
            } catch (IOException e) {
                generatedFileCanonicalPath = new File(configDirectory,
                        BinaryScannerUtil.GENERATED_FEATURES_FILE_PATH).toString();
            }
            getLog().warn(
                    "The source configuration directory will be modified. Features will automatically be generated in a new file: "
                            + generatedFileCanonicalPath);
            runLibertyMojoGenerateFeatures(null, true);
        } catch (MojoExecutionException e) {
            if (e.getCause() != null && e.getCause() instanceof PluginExecutionException) {
                // PluginExecutionException indicates that the binary scanner jar could not be found
                getLog().error(e.getMessage() + ".\nDisabling the automatic generation of features.");
                generateFeatures = false;
            } else {
                throw new MojoExecutionException(e.getMessage()
                + " To disable the automatic generation of features, start dev mode with -DgenerateFeatures=false.",
                e);
            }
        }
    }

    @Override
    public void execute() throws MojoExecutionException {
        init();
        
        if (skip) {
            getLog().info("\nSkipping dev goal.\n");
            return;
        }

        doDevMode();
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
            getLog().error("An unexpected error occurred when trying to resolve " + proj.getFile() + ": " + e.getMessage());
            getLog().debug(e);
        }
    }

    private JavaCompilerOptions getMavenCompilerOptions(MavenProject currentProject) {
        Plugin plugin = getPluginForProject("org.apache.maven.plugins", "maven-compiler-plugin", currentProject);
        Xpp3Dom configuration = ExecuteMojoUtil.getPluginGoalConfig(plugin, "compile", getLog());
        JavaCompilerOptions compilerOptions = new JavaCompilerOptions();

        String showWarnings = getCompilerOption(configuration, "showWarnings", "maven.compiler.showWarnings", currentProject);
        if (showWarnings != null) {
            boolean showWarningsBoolean = Boolean.parseBoolean(showWarnings);
            getLog().debug("Setting showWarnings to " + showWarningsBoolean);
            compilerOptions.setShowWarnings(showWarningsBoolean);
        }

        String release = getCompilerOption(configuration, "release", "maven.compiler.release", currentProject);
        String source = getCompilerOption(configuration, "source", "maven.compiler.source", currentProject);
        String target = getCompilerOption(configuration, "target", "maven.compiler.target", currentProject);
        if (release != null) {
            getLog().debug("Setting compiler release to " + release);
            if (source != null) {
                getLog().debug("Compiler option source will be ignored since release is specified");
            }
            if (target != null) {
                getLog().debug("Compiler option target will be ignored since release is specified");
            }
            compilerOptions.setRelease(release);
        } else {
            // add source and target only if release is not set
            if (source != null) {
                getLog().debug("Setting compiler source to " + source);
                compilerOptions.setSource(source);
            }
            if (target != null) {
                getLog().debug("Setting compiler target to " + target);
                compilerOptions.setTarget(target);
            }
        }

        String encoding = getCompilerOption(configuration, "encoding", "project.build.sourceEncoding", currentProject);
        if (encoding != null) {
            getLog().debug("Setting compiler encoding to " + encoding);
            compilerOptions.setEncoding(encoding);
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
            getLog().error("An unexpected error occurred when trying to run integration tests for "
                    + buildFile.getAbsolutePath() + ": " + e.getMessage());
            getLog().debug(e);
        }
        return currentProject;
    }

    private void runTestMojo(String groupId, String artifactId, String goal, MavenProject project)
            throws MojoExecutionException {
        Plugin plugin = getPluginForProject(groupId, artifactId, project);
        Xpp3Dom config = ExecuteMojoUtil.getPluginGoalConfig(plugin, goal, getLog());

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
                getLog().error(
                        "Unable to resolve test artifact paths for " + project.getFile() + ". Restart dev mode to ensure classpaths are properly resolved.");
                getLog().debug(e);
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
                getLog().debug("Looking for summary file at " + summaryFile.getCanonicalPath());
            } catch (IOException e) {
                getLog().debug("Unable to resolve summary file " + e.getMessage());
            }
            if (summaryFile.exists()) {
                boolean deleteResult = summaryFile.delete();
                getLog().debug("Summary file deleted? " + deleteResult);
            } else {
                getLog().debug("Summary file doesn't exist");
            }
        } else if (goal.equals("failsafe-report-only")) {
            Plugin failsafePlugin = getPluginForProject("org.apache.maven.plugins", "maven-failsafe-plugin", project);
            Xpp3Dom failsafeConfig = ExecuteMojoUtil.getPluginGoalConfig(failsafePlugin, "integration-test", getLog());
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
            Xpp3Dom surefireConfig = ExecuteMojoUtil.getPluginGoalConfig(surefirePlugin, "test", getLog());
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

        getLog().debug("POM file: " + project.getFile() + "\n" + groupId + ":" + artifactId + " " + goal
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

    private void runBoostMojo(String goal) throws MojoExecutionException {

        MavenProject boostProject = this.project;
        MavenSession boostSession = this.session;

        getLog().debug("plugin version: " + boostPlugin.getVersion());
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
        Xpp3Dom config = ExecuteMojoUtil.getPluginGoalConfig(plugin, goal, getLog());
        config = Xpp3Dom.mergeXpp3Dom(configuration(element(name("failOnError"), "false")), config);
        getLog().info("Running maven-compiler-plugin:" + goal + " on " + tempProject.getFile());
        getLog().debug("configuration:\n" + config);
        executeMojo(plugin, goal(goal), config, executionEnvironment(tempProject, tempSession, pluginManager));
    }

    private void runCompileMojoLogWarningWithException(String goal) throws MojoExecutionException {
        Plugin plugin = getPluginForProject("org.apache.maven.plugins", "maven-compiler-plugin", project);
        MavenSession tempSession = session.clone();
        tempSession.setCurrentProject(project);
        MavenProject tempProject = project;
        Xpp3Dom config = ExecuteMojoUtil.getPluginGoalConfig(plugin, goal, getLog());
        config = Xpp3Dom.mergeXpp3Dom(configuration(element(name("failOnError"), "true")), config);
        getLog().info("Running maven-compiler-plugin:" + goal + " on " + tempProject.getFile());
        getLog().debug("configuration:\n" + config);
        executeMojo(plugin, goal(goal), config, executionEnvironment(tempProject, tempSession, pluginManager));

        updateArtifactPathToOutputDirectory(project);
    }

    /**
     * Executes maven:compile but logs errors as warning messages
     * 
     * @throws MojoExecutionException
     */
    private void runCompileMojoLogWarning() throws MojoExecutionException {
        runCompileMojo("compile", project);
        updateArtifactPathToOutputDirectory(project);
    }

    /**
     * Executes maven:compile but logs errors as warning messages
     * 
     * @throws MojoExecutionException
     */
    private void runCompileMojoLogWarning(MavenProject mavenProject) throws MojoExecutionException {
        runCompileMojo("compile", mavenProject);
        updateArtifactPathToOutputDirectory(mavenProject);
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
    protected void runLibertyMojoInstallFeature(Element features, File serverDir, String containerName) throws MojoExecutionException {
        super.runLibertyMojoInstallFeature(features, serverDir, containerName);
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
            getLog().debug("runLibertyMojoCreate check for installDirectory and serverDirectory");
            if (!installDirectory.isDirectory()) {
                installDirectory.mkdirs();
            }
            if (!serverDirectory.isDirectory()) {
                serverDirectory.mkdirs();
            }
        } else {
            // Check to see if Liberty was already installed and set flag accordingly.
            if (installDirectory != null) {
                try {
                    File installDirectoryCanonicalFile = installDirectory.getCanonicalFile();
                    // Quick check to see if a Liberty installation exists at the installDirectory CLK999
                    // Do not mark this as a non-new installation if the installDirectory is different than
                    // the previous one listed in liberty-plugin-config.xml? But the only way it already exists
                    // and is different is if it is an external installation, which should then be managed outside
                    // of the plugin goals as far as feature installation goes. So perhaps we leave it be, but
                    // print a log message to indicate we detected a change in install directory location?
                    File file = new File(installDirectoryCanonicalFile, "lib/ws-launch.jar");
                    if (file.exists()) {
                        this.isNewInstallation = false;
                    }
                } catch (IOException e) {
                }
            }
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
