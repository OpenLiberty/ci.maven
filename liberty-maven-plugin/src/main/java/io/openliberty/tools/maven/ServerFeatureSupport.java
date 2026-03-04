/**
 * (C) Copyright IBM Corporation 2021, 2026.
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

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import io.openliberty.tools.common.plugins.util.ServerFeatureUtil;
import io.openliberty.tools.maven.utils.DevHelper;
import io.openliberty.tools.maven.utils.ExecuteMojoUtil;

public abstract class ServerFeatureSupport extends BasicSupport {
	
    private static final String LIBERTY_MAVEN_PLUGIN_GROUP_ID = "io.openliberty.tools";
    private static final String LIBERTY_MAVEN_PLUGIN_ARTIFACT_ID = "liberty-maven-plugin";

    private static final String LIBERTY_CONFIG_MAVEN_PROPS = "(^liberty\\.(env|jvm|bootstrap|var|defaultVar)\\.).+";
    private static final Pattern pattern = Pattern.compile(LIBERTY_CONFIG_MAVEN_PROPS);
    private static final String LATE_PROP_RESOLUTION_SYNTAX = "@\\{(.+?)\\}";
    private static final Pattern LATE_PROP_PATTERN = Pattern.compile(LATE_PROP_RESOLUTION_SYNTAX);

    private ServerFeatureUtil servUtil;

    protected Map<String,String> bootstrapMavenProps = new HashMap<String,String>();
    protected Map<String,String> envMavenProps = new HashMap<String,String>();
    protected List<String> jvmMavenPropNames = new ArrayList<String>();  // only used for tracking overriding properties - not included in the generated jvm.options file
    protected List<String> jvmMavenPropValues = new ArrayList<String>();
    protected Map<String,String> varMavenProps = new HashMap<String,String>();
    protected Map<String,String> defaultVarMavenProps = new HashMap<String,String>();

    /**
     * Location of jvm.options file.
     */
    @Parameter(property = "jvmOptionsFile")
    protected File jvmOptionsFile;

    @Parameter
    protected List<String> jvmOptions;

    protected enum PropertyType {
        BOOTSTRAP("liberty.bootstrap."),
        ENV("liberty.env."),
        JVM("liberty.jvm."),
        VAR("liberty.var."),
        DEFAULTVAR("liberty.defaultVar.");

        private final String prefix;

        private PropertyType(final String prefix) {
            this.prefix = prefix;
        }

        private static final Map<String, PropertyType> lookup = new HashMap<String, PropertyType>();

        static {
            for (PropertyType s : EnumSet.allOf(PropertyType.class)) {
                lookup.put(s.prefix, s);
            }
        }

        public static PropertyType getPropertyType(String propertyName) {
            // get a matcher object from pattern
            Matcher matcher = pattern.matcher(propertyName);

            // check whether Regex string is found in propertyName or not
            if (matcher.find()) {
                // strip off the end of the property name to get the prefix
                String prefix = matcher.group(1);
                return lookup.get(prefix);
            }
            return null;
        }

        public String getPrefix() {
            return prefix;
        }

    }
    /**
     * The current plugin's descriptor. This is auto-filled by Maven 3.
     */
    @Parameter( defaultValue = "${plugin}", readonly = true )
    private PluginDescriptor plugin;

    @Parameter
    protected Map<String, String> jdkToolchain;

    /**
     * The toolchain manager
     */
    @Component
    protected ToolchainManager toolchainManager;

    @Parameter(defaultValue = "${mojoExecution}", readonly = true)
    protected MojoExecution mojoExecution;

    protected Toolchain toolchain;

    protected class ServerFeatureMojoUtil extends ServerFeatureUtil {

        @Override
        public void debug(String msg) {
            if (isDebugEnabled()) {
                getLog().debug(msg);
            }
        }

        @Override
        public void debug(String msg, Throwable e) {
            if (isDebugEnabled()) {
                getLog().debug(msg, e);
            }
        }

        @Override
        public void debug(Throwable e) {
            if (isDebugEnabled()) {
                getLog().debug(e);
            }
        }

        @Override
        public void warn(String msg) {
            if (!suppressLogs) {
                getLog().warn(msg);
            } else {
                debug(msg);
            }
        }

        @Override
        public void info(String msg) {
            if (!suppressLogs) {
                getLog().info(msg);
            } else {
                debug(msg);
            }
        }

        @Override
        public void error(String msg, Throwable e) {
            getLog().error(msg, e);
        }

        @Override
        public void error(String msg) {
            getLog().error(msg);
        }

        @Override
        public boolean isDebugEnabled() {
            return getLog().isDebugEnabled();
        }
    }

    @Override
    protected void init() throws MojoExecutionException {
        super.init();
        initToolchain();
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
    protected ServerFeatureUtil getServerFeatureUtil(boolean suppressLogs, Map<String, File> libDirPropFiles) {
        if (servUtil == null) {
            createNewServerFeatureUtil();
            servUtil.setLibertyDirectoryPropertyFiles(libDirPropFiles);
        }
        if (suppressLogs) {
            servUtil.setSuppressLogs(true);
        } else {
            servUtil.setSuppressLogs(false);
        }
        return servUtil;
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
     * Get the list of projects that are relevant when certain goals (e.g. "dev" and "run") are executed
     * against a multi-module project. Relevant projects are any projects in the same downstream/upstream flow.
     * Jar projects without any downstream modules are implicitly ignored and any projects with skip=true 
     * configured in the plugin configuration are also ignored. If multiple "streams" still exist, a MojoExecutionException
     * is thrown logging the conflict. 
     * 
     * 
     * @param graph
     * @return
     * @throws MojoExecutionException
     */
    protected List<MavenProject> getRelevantMultiModuleProjects(ProjectDependencyGraph graph, boolean skipJars) throws MojoExecutionException {
    	getLog().debug("Resolve relevant multi-module projects");
    	
        List<MavenProject> sortedReactorProjects = graph.getSortedProjects();
        Set<MavenProject> conflicts = new LinkedHashSet<MavenProject>(); // keeps the order of items added in

        // A leaf here is a module without any downstream modules depending on it
        List<MavenProject> leaves = new ArrayList<MavenProject>();
        for (MavenProject reactorProject : sortedReactorProjects) {
            if (graph.getDownstreamProjects(reactorProject, true).isEmpty()) {
            	getLog().debug("Found final downstream project: " + reactorProject.getArtifactId());
                
            	if (skipConfigured(reactorProject)) {
            		getLog().debug("Skip configured on project: " + reactorProject.getArtifactId() + " - Ignoring");
            	} else if (reactorProject.getPackaging().equals("jar") && skipJars) {
            		getLog().debug(reactorProject.getArtifactId() + " is a jar project - Ignoring");
            	} else {
                    leaves.add(reactorProject);
            	}
            }
        }
            
        // At this point, the only leaves we should have is one final downstream project and the parent pom project. 
        // Loop through and find any conflicts.
        for (MavenProject leaf1 : leaves) {
            for (MavenProject leaf2 : leaves) {
            	// Check that the leaves are not the same module and that one of the leaves is not the parent.
                if (leaf1 != leaf2 && !(isSubModule(leaf2, leaf1) || isSubModule(leaf1, leaf2)) ) {
                    conflicts.add(leaf1);
                    conflicts.add(leaf2);
                }
            }
        }
        
        if (conflicts.isEmpty() ) {
        	List<MavenProject> devModeProjects = new ArrayList<MavenProject>();
        	for (MavenProject leaf : leaves) {
        		devModeProjects.addAll(graph.getUpstreamProjects(leaf, true));
        		devModeProjects.add(leaf);
        	}
        	
        	getLog().debug("Resolved multi-module projects: ");
        	for (MavenProject project : devModeProjects) {
        		getLog().debug(project.getArtifactId());
        	}
        	return devModeProjects;
        } else {
        	
        	List<String> conflictModuleRelativeDirs = new ArrayList<String>();
            for (MavenProject conflict : conflicts) {
                // Make the module path relative to the multi-module project directory
                conflictModuleRelativeDirs.add(getModuleRelativePath(conflict));
            }
            
            throw new MojoExecutionException("Found multiple independent modules in the Reactor build order: "
                    + conflictModuleRelativeDirs
                    + ". Skip a conflicting module in the Liberty configuration or specify the module containing the Liberty configuration that you want to use for the server by including the following parameters in the Maven command: -pl <module-with-liberty-config> -am");
        }
    }
    
    private boolean skipConfigured(MavenProject project) {
    	
        // Properties that are set in the pom file
        Properties props = project.getProperties();

        // Properties that are set by user via CLI parameters
        Properties userProps = session.getUserProperties();
    	Plugin libertyPlugin = getLibertyPluginForProject(project);
        Xpp3Dom config = ExecuteMojoUtil.getPluginGoalConfig(libertyPlugin, "dev", getLog());
        
        return DevHelper.getBooleanFlag(config, userProps, props, "skip");
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
            getLog().debug("Found a previous module in the Reactor build order that does not have downstream dependencies: "
                    + mostDownstreamModule);
            if (isSubModule(project, mostDownstreamModule)) {
                getLog().debug(
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
    
    protected Plugin getLibertyPluginForProject(MavenProject currentProject) {
        // Try getting the version from Maven 3's plugin descriptor
        String version = null;
        if (plugin != null && plugin.getPlugin() != null) {
            version = plugin.getVersion();
            getLog().debug("Setting plugin version to " + version);
        }
        Plugin projectPlugin = currentProject.getPlugin(LIBERTY_MAVEN_PLUGIN_GROUP_ID + ":" + LIBERTY_MAVEN_PLUGIN_ARTIFACT_ID);
        if (projectPlugin == null) {
            getLog().debug("Did not find liberty-maven-plugin configured in currentProject: "+currentProject.toString());
            projectPlugin = getPluginFromPluginManagement(LIBERTY_MAVEN_PLUGIN_GROUP_ID, LIBERTY_MAVEN_PLUGIN_ARTIFACT_ID, currentProject);
        } else {
            getLog().debug("Found liberty-maven-plugin configured in currentProject: "+currentProject.toString());
        }
        if (projectPlugin == null) {
            getLog().debug("Did not find liberty-maven-plugin in pluginManagement in currentProject: "+currentProject.toString());
            projectPlugin = plugin(LIBERTY_MAVEN_PLUGIN_GROUP_ID, LIBERTY_MAVEN_PLUGIN_ARTIFACT_ID, "LATEST");
        }
        if (version != null) {
            projectPlugin.setVersion(version);
        }
        return projectPlugin;
    }
    
    protected Plugin getPluginFromPluginManagement(String groupId, String artifactId, MavenProject currentProject) {
        Plugin retVal = null;
        PluginManagement pm = currentProject.getPluginManagement();
        if (pm != null) {
            for (Plugin p : pm.getPlugins()) {
                if (groupId.equals(p.getGroupId()) && artifactId.equals(p.getArtifactId())) {
                    retVal = p;
                    break;
                }
            }
        }
        return retVal;
    }
    
    /**
     * Given the groupId and artifactId get the corresponding plugin for the
     * specified project
     * 
     * @param groupId
     * @param artifactId
     * @param currentProject
     * @return Plugin
     */
    protected Plugin getPluginForProject(String groupId, String artifactId, MavenProject currentProject) {
        Plugin plugin = currentProject.getPlugin(groupId + ":" + artifactId);
        if (plugin == null) {
            plugin = getPluginFromPluginManagement(groupId, artifactId, currentProject);
        }
        if (plugin == null) {
            plugin = plugin(groupId(groupId), artifactId(artifactId), version("RELEASE"));
        }
        return plugin;
    }

    /**
     * Initialize the toolchain by calling the toolchain goal.
     * If useToolchainJdk is set to true, this method will also set the toolchain variable.
     *
     * @throws MojoExecutionException If an exception occurred while running toolchain goal
     */
    protected void initToolchain() throws MojoExecutionException {
        // Skip if toolchain support is not enabled
        if (jdkToolchain == null) {
            return;
        }
        if (toolchainManager == null) {
            getLog().warn("ToolchainManager is null. Falling back to system default JDK.");
            return;
        }
        List<Toolchain> tcs = toolchainManager.getToolchains(session, "jdk", jdkToolchain);
        if (tcs != null && !tcs.isEmpty()) {
            this.toolchain = tcs.get(0);
            getLog().info(MessageFormat.format(messages.getString("info.toolchain.initialized"), this.toolchain));
        } else {
            getLog().warn(MessageFormat.format(messages.getString("warn.toolchain.not.available"), jdkToolchain));
        }
    }

    /**
     *
     * @param serverEnvLines lines from server.env
     * @param jvmOptionsLines lines from jvm.options
     * @return true or false
     */
    protected static boolean isJavaHomeSet(List<String> serverEnvLines, List<String> jvmOptionsLines) {
        boolean javaHomeSet = false;
        for (String serverEnvLine : serverEnvLines) {
            if (serverEnvLine.startsWith("JAVA_HOME=")) {
                javaHomeSet = true;
                break;
            }
        }
        for (String jvmOptionLine : jvmOptionsLines) {
            if (jvmOptionLine.contains("-DJAVA_HOME=") || jvmOptionLine.contains("-Djava.home=")) {
                javaHomeSet = true;
                break;
            }
        }
        return javaHomeSet;
    }

    /**
     * Get JDK home directory from toolchain
     * 1. The java executable is found at: [JAVA_HOME]/bin/java
     * 2. The code starts by finding the full path to java (e.g., /path/to/jdk/bin/java).
     * 3. It calls getParentFile() once to get the bin directory (e.g., /path/to/jdk/bin).
     * 4. It calls getParent() again on the bin directory to get the root directory (e.g., /path/to/jdk), which is the JAVA_HOME.
     *
     * @param toolchain The toolchain to get JDK home from
     * @return The JDK home directory path, or null if it could not be determined
     */
    protected String getJdkHomeFromToolchain(Toolchain toolchain) {
        String javaBinFileLocation = toolchain.findTool("java");
        if (javaBinFileLocation != null) {
            File javaFile = new File(javaBinFileLocation);
            File binDir = javaFile.getParentFile();
            if (binDir != null) {
                return binDir.getParent();
            }else {
                getLog().warn("bin directory not found for "+ javaBinFileLocation);
            }
        }
        return null;
    }

    /**
     * @return environment variable map with Toolchain JDK
     */
    protected Map<String, String> getToolchainEnvVar() {

        if (toolchain == null) {
            return Collections.emptyMap();
        }
        String jdkHome = getJdkHomeFromToolchain(toolchain);
        if (jdkHome == null) {
            getLog().warn("Could not determine JDK home from toolchain. Toolchain will not be honored");
            return Collections.emptyMap();
        }
        if (jvmMavenPropNames.isEmpty() || envMavenProps.isEmpty()) {
            // run once to make sure project properties are loaded
            loadLibertyConfigFromProperties();
        }
        //check whether server.env properties or server.jvmOptions properties contain java home
        if(envMavenProps.containsKey("JAVA_HOME") || envMavenProps.containsKey("java.home") ||
                jvmMavenPropValues.stream()
                        .anyMatch(v->v.contains("-DJAVA_HOME=") || v.contains("-Djava.home="))){
            getLog().warn(MessageFormat.format(
                    messages.getString("warn.project.properties.java.home.configured"),
                    mojoExecution.getGoal()
            ));
            return Collections.emptyMap();
        }
        // 1. Read existing config files
        List<String> serverEnvLines = readConfigFileLines(getServerEnvFile());
        if (mergeServerEnv && serverEnvFile != null && serverEnvFile.exists() && configDirectory != null && configDirectory.exists()) {
                File configDirServerEnv = new File(configDirectory, "server.env");
                if (configDirServerEnv.exists()) {
                    serverEnvLines.addAll(readConfigFileLines(configDirServerEnv));
                }
            }

        List<String> jvmOptionsLines = readConfigFileLines(findConfigFile("jvm.options", jvmOptionsFile));

        // 2. Check for existing JAVA_HOME configuration
        // if user has configured JAVA_HOME in server.env or jvm.options, this will get higher precedence over toolchain JDK
        // hence a warning will be issued
        if (isJavaHomeSet(serverEnvLines, jvmOptionsLines)) {
            getLog().warn(MessageFormat.format(
                    messages.getString("warn.server.env.java.home.configured"),
                    mojoExecution.getGoal()
            ));
        } else {
            // 3. Apply toolchain configuration
            return populateEnviornmentVariablesMap(jdkHome);
        }
        return Collections.emptyMap();
    }

    /**
     * Determines the primary server.env file to read.
     * Checks serverEnvFile first, then a default location in serverDirectory.
     *
     * @return The File object for the server.env, or null if neither exists or is specified.
     */
    private File getServerEnvFile() {
        if (serverEnvFile != null && serverEnvFile.exists()) {
            return serverEnvFile;
        }
        if (configDirectory == null) {
            return null;
        }
        File defaultServerEnv = new File(configDirectory, "server.env");
        if (defaultServerEnv.exists()) {
            return defaultServerEnv;
        }
        return null;
    }

    /**
     * Reads all lines from a configuration file, handling null/non-existent files
     * and I/O exceptions gracefully.
     *
     * @param configFile The file to read.
     * @return A list of strings, each representing a line in the file. Returns an empty list on failure.
     */
    private List<String> readConfigFileLines(File configFile) {
        if (configFile == null || !configFile.exists()) {
            return Collections.emptyList();
        }
        Path configPath = configFile.toPath();
        try {
            return Files.readAllLines(configPath);
        } catch (IOException e) {
            getLog().warn("Error reading config file: " + configPath);
            return Collections.emptyList();
        }
    }

    /**
     * Applies the toolchain's JDK home to the ServerTask's environment variables.
     *
     * @param jdkHome    The resolved JDK home path.
     * @return envVars
     */
    private Map<String, String> populateEnviornmentVariablesMap(String jdkHome) {
        getLog().info(MessageFormat.format(
                messages.getString("info.toolchain.configured"),
                mojoExecution.getGoal(),
                jdkHome
        ));
        Map<String, String> envVars = new HashMap<>();
        envVars.put("JAVA_HOME", jdkHome);
        return envVars;
    }

    protected void loadLibertyConfigFromProperties() {
        loadLibertyConfigFromProperties(project.getProperties());
        loadLibertyConfigFromProperties(System.getProperties());
    }

    // Search the value parameter for any properties referenced with @{xxx} syntax and replace those with their property value if defined.
    protected String resolveLatePropertyReferences(String value) {
        String returnValue = value;

        if (value != null) {
            Matcher m = LATE_PROP_PATTERN.matcher(value);
            while (m.find()) {
                String varName = m.group(1);
                if (project.getProperties().containsKey(varName)) {
                    String replacementValue = project.getProperties().getProperty(varName);
                    if (replacementValue != null) {
                        returnValue = returnValue.replace("@{"+varName+"}", replacementValue);
                        getLog().debug("Replaced Liberty configuration property value @{"+varName+"} with value "+replacementValue);
                    }
                }
            }
        }

        return returnValue;
    }

    protected void loadLibertyConfigFromProperties(Properties props) {
        Set<Map.Entry<Object, Object>> entries = props.entrySet();
        for (Map.Entry<Object, Object> entry : entries) {
            String key = (String) entry.getKey();
            PropertyType propType = PropertyType.getPropertyType(key);

            if (propType != null) {
                String suffix = key.substring(propType.getPrefix().length());
                String value = (String) entry.getValue();
                // Check the value for late property resolution with @{xxx} syntax.
                value = resolveLatePropertyReferences(value);

                getLog().debug("Processing Liberty configuration from property with key "+key+" and value "+value);
                switch (propType) {
                    case ENV:        envMavenProps.put(suffix, value);
                        break;
                    case BOOTSTRAP:  bootstrapMavenProps.put(suffix, value);
                        break;
                    case JVM:        if (jvmMavenPropNames.contains(suffix)) {
                        int index = jvmMavenPropNames.indexOf(suffix);
                        getLog().debug("Remove duplicate property with name: "+suffix+" at position: "+index);
                        jvmMavenPropNames.remove(index);
                        jvmMavenPropValues.remove(index);
                    }
                        jvmMavenPropNames.add(suffix);  // need to keep track of names so that a system prop can override a project prop
                        jvmMavenPropValues.add(value);
                        break;
                    case VAR:        varMavenProps.put(suffix, value);
                        break;
                    case DEFAULTVAR: defaultVarMavenProps.put(suffix, value);
                        break;
                }
            }
        }
    }


    /*
     * Return specificFile if it exists; otherwise return the file with the requested fileName from the
     * configDirectory, but only if it exists. Null is returned if the file does not exist in either location.
     */
    protected File findConfigFile(String fileName, File specificFile) {
        if (specificFile != null && specificFile.exists()) {
            return specificFile;
        }

        File f = new File(configDirectory, fileName);
        if (configDirectory != null && f.exists()) {
            return f;
        }
        return null;
    }
}
