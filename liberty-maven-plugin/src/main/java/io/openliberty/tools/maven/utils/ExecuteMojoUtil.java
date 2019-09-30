package io.openliberty.tools.maven.utils;

import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.twdata.maven.mojoexecutor.MojoExecutor.Element;

public class ExecuteMojoUtil {

    // https://maven.apache.org/plugins/maven-compiler-plugin/compile-mojo.html
    private static final ArrayList<String> COMPILE_PARAMS = new ArrayList<>(Arrays.asList(
            "annotationProcessorPaths", "annotationProcessors", "compilerArgs", "compilerArgument",
            "compilerArguments", "compilerId", "compilerReuseStrategy", "compilerVersion", "debug",
            "debuglevel", "encoding", "excludes", "executable", "failOnError", "failOnWarning",
            "fileExtensions", "forceJavacCompilerUse", "fork", "generatedSourcesDirectory", 
            "includes", "jdkToolchain", "maxmem", "meminitial", "multiReleaseOutput", "optimize",
            "outputFileName", "parameters", "proc", "release", "showDeprecation", "showWarnings",
            "skipMain", "skipMultiThreadWarning", "source", "staleMillis", "target",
            "useIncrementalCompilation", "verbose"
    ));

    // https://maven.apache.org/plugins/maven-compiler-plugin/testCompile-mojo.html
    private static final ArrayList<String> TEST_COMPILE_PARAMS = new ArrayList<>(Arrays.asList(
            "annotationProcessorPaths", "annotationProcessors", "compilerArgs", "compilerArgument",
            "compilerArguments", "compilerId", "compilerReuseStrategy", "compilerVersion", "debug",
            "debuglevel", "encoding", "executable", "failOnError", "failOnWarning", 
            "fileExtensions", "forceJavacCompilerUse", "fork", "generatedTestSourcesDirectory", 
            "jdkToolchain", "maxmem", "meminitial", "optimize", "outputFileName", "parameters",
            "proc", "release", "showDeprecation", "showWarnings", "skip", "skipMultiThreadWarning",
            "source", "staleMillis", "target", "testCompilerArgument", "testCompilerArguments",
            "testExcludes", "testIncludes", "testRelease", "testSource", "testTarget",
            "useIncrementalCompilation", "verbose"
    ));

    // https://maven.apache.org/plugins/maven-resources-plugin/resources-mojo.html
    private static final ArrayList<String> RESOURCES_PARAMS = new ArrayList<>(Arrays.asList(
            "outputDirectory", "addDefaultExcludes", "delimiters", "encoding", "escapeString",
            "escapeWindowsPaths", "fileNameFiltering", "filters", "includeEmptyDirs", 
            "mavenFilteringHints", "nonFilteredFileExtensions", "overwrite", "skip",
            "supportMultiLineFiltering", "useBuildFilters", "useDefaultDelimiters"
    ));

    // https://maven.apache.org/plugins/maven-resources-plugin/testResources-mojo.html
    private static final ArrayList<String> TEST_RESOURCES_PARAMS = new ArrayList<>(Arrays.asList(
            "outputDirectory", "resources", "addDefaultExcludes", "delimiters", "encoding",
            "escapeString", "escapeWindowsPaths", "fileNameFiltering", "filters",
            "includeEmptyDirs", "mavenFilteringHints", "nonFilteredFileExtensions",
            "overwrite", "skip", "supportMultiLineFiltering", "useBuildFilters",
            "useDefaultDelimiters"
    ));

    // https://maven.apache.org/surefire/maven-surefire-plugin/test-mojo.html
    private static final ArrayList<String> TEST_PARAMS = new ArrayList<>(Arrays.asList(
            "testSourceDirectory", "additionalClasspathElements", "argLine", "basedir",
            "childDelegation", "classesDirectory", "classpathDependencyExcludes", 
            "classpathDependencyScopeExclude", "debugForkedProcess", "dependenciesToScan",
            "disableXmlReport", "enableAssertions", "encoding", "environmentVariables",
            "excludedGroups", "excludes", "excludesFile", "failIfNoSpecifiedTests",
            "failIfNoTests", "forkCount", "forkMode", "forkedProcessExitTimeoutInSeconds",
            "forkedProcessTimeoutInSeconds", "groups", "includes", "includesFile",
            "junitArtifactName", "junitPlatformArtifactName", "jvm", "objectFactory",
            "parallel", "parallelOptimized", "parallelTestsTimeoutForcedInSeconds",
            "parallelTestsTimeoutInSeconds", "perCoreThreadCount", "printSummary",
            "properties", "redirectTestOutputToFile", "remoteRepositories", "reportFormat",
            "reportNameSuffix", "reportsDirectory", "rerunFailingTestsCount", "reuseForks",
            "runOrder", "shutdown", "skip", "skipAfterFailureCount", "skipExec", 
            "skipTests", "suiteXmlFiles", "systemProperties", "systemPropertiesFile",
            "systemPropertyVariables", "tempDir", "test", "testClassesDirectory",
            "testFailureIgnore", "testNGArtifactName", "threadCount", "threadCountClasses",
            "threadCountMethods", "threadCountSuites", "trimStackTrace", "useFile",
            "useManifestOnlyJar", "useModulePath", "useSystemClassLoader",
            "useUnlimitedThreads", "workingDirectory"
    ));

    // https://maven.apache.org/surefire/maven-failsafe-plugin/integration-test-mojo.html
    private static final ArrayList<String> INTEGRATION_TEST_PARAMS = new ArrayList<>(Arrays.asList(
            "summaryFile", "testSourceDirectory", "additionalClasspathElements", "argLine",
            "basedir", "childDelegation", "classesDirectory", "classpathDependencyExcludes",
            "classpathDependencyScopeExclude", "debugForkedProcess", "dependenciesToScan",
            "disableXmlReport", "enableAssertions", "encoding", "environmentVariables",
            "excludedGroups", "excludes", "excludesFile", "failIfNoSpecifiedTests",
            "failIfNoTests", "forkCount", "forkMode", "forkedProcessExitTimeoutInSeconds",
            "forkedProcessTimeoutInSeconds", "groups", "includes", "includesFile",
            "junitArtifactName", "junitPlatformArtifactName", "jvm", "objectFactory",
            "parallel", "parallelOptimized", "parallelTestsTimeoutForcedInSeconds", 
            "parallelTestsTimeoutInSeconds", "perCoreThreadCount", "printSummary",
            "properties", "redirectTestOutputToFile", "remoteRepositories", "reportFormat",
            "reportNameSuffix", "reportsDirectory", "rerunFailingTestsCount", "reuseForks",
            "runOrder", "shutdown", "skip", "skipAfterFailureCount", "skipExec", "skipITs",
            "skipTests", "suiteXmlFiles", "systemProperties", "systemPropertiesFile",
            "systemPropertyVariables", "tempDir", "test", "testClassesDirectory",
            "testNGArtifactName", "threadCount", "threadCountClasses", "threadCountMethods",
            "threadCountSuites", "trimStackTrace", "useFile", "useManifestOnlyJar",
            "useModulePath", "useSystemClassLoader", "useUnlimitedThreads", "workingDirectory"
    ));

    // https://maven.apache.org/surefire/maven-failsafe-plugin/verify-mojo.html
    private static final ArrayList<String> VERIFY_PARAMS = new ArrayList<>(Arrays.asList(
            "summaryFile", "basedir", "encoding", "failIfNoTests", "reportsDirectory",
            "skip", "skipExec", "skipITs", "skipTests", "summaryFiles", 
            "testClassesDirectory", "testFailureIgnore"
    ));

    // https://maven.apache.org/surefire/maven-surefire-report-plugin/report-only-mojo.html
    private static final ArrayList<String> REPORT_ONLY_PARAMS = new ArrayList<>(Arrays.asList(
            "outputName", "showSuccess", "aggregate", "alwaysGenerateSurefireReport",
            "description", "linkXRef", "reportsDirectories", "reportsDirectory",
            "skipSurefireReport", "title", "xrefLocation"
    ));

    // https://maven.apache.org/surefire/maven-surefire-report-plugin/failsafe-report-only-mojo.html
    private static final ArrayList<String> FAILSAFE_REPORT_ONLY_PARAMS = REPORT_ONLY_PARAMS;

    private static final ArrayList<String> LIBERTY_COMMON_PARAMS = new ArrayList<>(Arrays.asList(
            "installDirectory", "assemblyArchive", "assemblyArtifact", "libertyRuntimeVersion",
            "install", "licenseArtifact", "serverName", "userDirectory", "outputDirectory",
            "assemblyInstallDirectory", "refresh", "skip"
    // executeMojo can not use alias parameters:
    // "runtimeArchive", "runtimeArtifact", "runtimeInstallDirectory"
    ));

    private static final ArrayList<String> LIBERTY_COMMON_SERVER_PARAMS = new ArrayList<>(
            Arrays.asList("serverXmlFile", "configDirectory", "bootstrapProperties", "bootstrapPropertiesFile",
                    "jvmOptions", "jvmOptionsFile", "serverEnvFile"
            // executeMojo can not use alias parameters:
            // "configFile", "serverEnv"
            ));
    
    private static final ArrayList<String> CREATE_PARAMS;
    static {
        CREATE_PARAMS = new ArrayList<>(Arrays.asList(
                "template", "libertySettingsFolder", "noPassword"
                ));
        CREATE_PARAMS.addAll(LIBERTY_COMMON_PARAMS);
        CREATE_PARAMS.addAll(LIBERTY_COMMON_SERVER_PARAMS);
    }
    
    private static final ArrayList<String> DEPLOY_PARAMS;
    static {
        DEPLOY_PARAMS = new ArrayList<>(Arrays.asList(
                "appsDirectory", "stripVersion", "deployPackages", "timeout", "looseApplication"
                // executeMojo can not use alias parameters:
                // "installAppPackages"
                ));
        DEPLOY_PARAMS.addAll(LIBERTY_COMMON_PARAMS);
        DEPLOY_PARAMS.addAll(LIBERTY_COMMON_SERVER_PARAMS);
    }
    
    private static final ArrayList<String> INSTALL_FEATURE_PARAMS;
    static {
        INSTALL_FEATURE_PARAMS = new ArrayList<>(Arrays.asList("features"));
        INSTALL_FEATURE_PARAMS.addAll(LIBERTY_COMMON_PARAMS);
    }

    private static final Map<String, String> LIBERTY_ALIAS_MAP;
    static {
        Map<String, String>tempMap = new HashMap<String, String>();
        tempMap.put("runtimeArtifact", "assemblyArtifact");
        tempMap.put("runtimeArchive", "assemblyArchive");
        tempMap.put("runtimeInstallDirectory", "assemblyInstallDirectory");
        tempMap.put("configFile", "serverXmlFile");
        tempMap.put("serverEnv", "serverEnvFile");
        tempMap.put("installAppPackages", "deployPackages");
        LIBERTY_ALIAS_MAP = Collections.unmodifiableMap(tempMap);
    }

    /**
     * Given the Plugin get the goal execution configuration.
     *
     * @param plugin
     * @param goal
     * @return configuration for the plugin execution goal
     */
    public static Xpp3Dom getPluginGoalConfig(Plugin plugin, String goal, Log log) {
        Xpp3Dom config = null;
        String execId = "default";
        int numExec = 0;

        List<PluginExecution> executions = plugin.getExecutions();
        if (executions != null) {
            for (PluginExecution e : executions) {
                if (e.getGoals() != null && e.getGoals().contains(goal)) {
                    if (numExec == 0) {
                        // execution configuration is already merged with the common plugin
                        // configuration
                        config = (Xpp3Dom) e.getConfiguration();
                        execId = e.getId();
                    }
                    numExec++;
                }
            }
            if (config == null) {
                config = (Xpp3Dom) plugin.getConfiguration();
            }
        } else {
            config = (Xpp3Dom) plugin.getConfiguration();
        }
        if (numExec > 1) {
            log.warn(plugin.getArtifactId() + ":" + goal 
                    + " goal has multiple execution configurations (default to \"" + execId + "\" execution)");
        }
        
        if (config == null) {
            config = configuration();
        } else {
            config = Xpp3Dom.mergeXpp3Dom(configuration(), config);
            config = validateConfiguration(plugin, goal, config, log);
        }
        log.debug(plugin.getArtifactId() + ":" + goal + " configuration\n" + config);
        return config;
    }
    
    private static Xpp3Dom validateConfiguration(Plugin plugin, String goal, Xpp3Dom config, Log log) {
        Xpp3Dom goalConfig;
        String executionGoal = plugin.getArtifactId() + ":" + goal;
        switch (executionGoal) {
        case "liberty-maven-plugin:create":
            config = convertLibertyAlias(config);
            goalConfig = stripConfigElements(config, CREATE_PARAMS);
            break;
        case "liberty-maven-plugin:deploy":
            config = convertLibertyAlias(config);
            goalConfig = stripConfigElements(config, DEPLOY_PARAMS);
            break;
        case "liberty-maven-plugin:install-feature":
            config = convertLibertyAlias(config);
            goalConfig = stripConfigElements(config, INSTALL_FEATURE_PARAMS);
            break;
        case "maven-compiler-plugin:compile":
            goalConfig = stripConfigElements(config, COMPILE_PARAMS);
            break;
        case "maven-compiler-plugin:testCompile":
            goalConfig = stripConfigElements(config, TEST_COMPILE_PARAMS);
            break;
        case "maven-compiler-plugin:resources":
            goalConfig = stripConfigElements(config, RESOURCES_PARAMS);
            break;
        case "maven-compiler-plugin:testResources":
            goalConfig = stripConfigElements(config, TEST_RESOURCES_PARAMS);
            break;
        case "maven-surefire-plugin:test":
            goalConfig = stripConfigElements(config, TEST_PARAMS);
            break;
        case "maven-failsafe-plugin:integration-test":
            goalConfig = stripConfigElements(config, INTEGRATION_TEST_PARAMS);
            break;
        case "maven-failsafe-plugin:verify":
            goalConfig = stripConfigElements(config, VERIFY_PARAMS);
            break;
        case "maven-surefire-report-plugin:report-only":
            goalConfig = stripConfigElements(config, REPORT_ONLY_PARAMS);
            break;
        case "maven-surefire-report-plugin:failsafe-report-only":
            goalConfig = stripConfigElements(config, FAILSAFE_REPORT_ONLY_PARAMS);
            break;
        default:
            goalConfig = config;
            log.info("skip execution goal configuration validation for " + executionGoal);
            break;
        }
        return goalConfig;
    }

    private static Xpp3Dom convertLibertyAlias(Xpp3Dom config) {
        // convert alias parameter key to actual parameter key
        Xpp3Dom alias;
        for (String key : LIBERTY_ALIAS_MAP.keySet()) {
            alias = config.getChild(key);
            if (alias != null) {
                if ("runtimeArtifact".contentEquals(key)) {
                    Xpp3Dom artifact = new Xpp3Dom(LIBERTY_ALIAS_MAP.get(key));
                    for (Xpp3Dom child : alias.getChildren()) {
                        artifact.addChild(child);
                    }
                    config.addChild(artifact);
                } else {
                    Element e = (element(name(LIBERTY_ALIAS_MAP.get(key)), alias.getValue()));
                    config.addChild(e.toDom());
                }
            }
        }
        return config;
    }

    private static Xpp3Dom stripConfigElements(Xpp3Dom config, ArrayList<String> goalParams) {
        // strip non applicable parameters
        List<Integer> removeChildren = new ArrayList<Integer>();
        for (int i=0; i<config.getChildCount(); i++) {
            if (!goalParams.contains(config.getChild(i).getName().trim())) {
                removeChildren.add(i);
            }
        }
        Collections.reverse(removeChildren);
        for (int child : removeChildren) {
            config.removeChild(child);
        }
        return config;
    }
}
