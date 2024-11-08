/**
 * (C) Copyright IBM Corporation 2016, 2024.
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
package io.openliberty.tools.maven.applications;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Validate;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.taskdefs.Copy;
import org.w3c.dom.Element;

import io.openliberty.tools.ant.ServerTask;
import io.openliberty.tools.ant.SpringBootUtilTask;
import io.openliberty.tools.maven.server.LooseAppSupport;
import io.openliberty.tools.maven.utils.CommonLogger;
import io.openliberty.tools.maven.utils.MavenProjectUtil;
import io.openliberty.tools.maven.utils.SpringBootUtil;
import io.openliberty.tools.common.plugins.config.ApplicationXmlDocument;
import io.openliberty.tools.common.plugins.config.LooseApplication;
import io.openliberty.tools.common.plugins.config.LooseConfigData;
import io.openliberty.tools.common.plugins.util.DevUtil;
import io.openliberty.tools.common.plugins.util.OSUtil;

/**
 * Support for installing and deploying applications to a Liberty server.
 */
public abstract class DeployMojoSupport extends LooseAppSupport {

    private final String PROJECT_ROOT_TARGET_LIBS = "target/libs";

    /**
     * Timeout to verify deploy successfully, in seconds.
     */
    @Parameter(property = "timeout", defaultValue = "40")
    protected long timeout = 40;
    
    /**
     * When deploying loose applications, the optional directory to which application dependencies are copied.
     */
    @Parameter(property = "copyLibsDirectory")
    protected File copyLibsDirectory;

    protected ApplicationXmlDocument applicationXml = new ApplicationXmlDocument();

    protected void installApp(Artifact artifact) throws MojoExecutionException, IOException {
    
        if (artifact.getFile() == null || artifact.getFile().isDirectory()) {
            String appFileName = getPreDeployAppFileName(project);
            File f = new File(project.getBuild().getDirectory() + "/" + appFileName);
            artifact.setFile(f);
        }

        if (!artifact.getFile().exists()) {
            throw new MojoExecutionException(messages.getString("error.install.app.missing"));
        }

        File destDir = new File(serverDirectory, getAppsDirectory());
        getLog().info(MessageFormat.format(messages.getString("info.install.app"), artifact.getFile().getCanonicalPath()));

        Copy copyFile = (Copy) ant.createTask("copy");
        File fileToCopy = artifact.getFile();
        copyFile.setFile(fileToCopy);
        String originalFileName = fileToCopy.getName();
        File destFile = new File(destDir, originalFileName);
        String destFileName = originalFileName;
        if (stripVersion) {
            destFileName = stripVersionFromName(originalFileName, artifact.getBaseVersion());
            destFile = new File(destDir, destFileName); // fileName should have changed after stripping version so recreate destFile
            copyFile.setTofile(destFile);
        } else {
            copyFile.setTodir(destDir);
        }

        // validate application configuration if appsDirectory="dropins" or inject
        // webApplication
        // to target server.xml if not found for appsDirectory="apps"
        validateAppConfig(destFile.getCanonicalPath(), destFileName, artifact.getArtifactId());

        deleteApplication(serverDirectory, fileToCopy, destFile);
        
        copyFile.execute();

        verifyAppStarted(destFileName);
    }

    private void setLooseProjectRootForContainer(MavenProject proj, LooseConfigData config) throws MojoExecutionException {
        try {
            // Set up the config to replace the absolute path names with ${variable}/target type references
            String projectRoot = DevUtil.getLooseAppProjectRoot(proj.getBasedir(), multiModuleProjectDirectory).getCanonicalPath();
            config.setProjectRoot(projectRoot);
            config.setSourceOnDiskName("${"+DevUtil.DEVMODE_PROJECT_ROOT+"}");
            if (copyLibsDirectory == null) { // in container mode, copy dependencies from .m2 dir to the target dir to mount in container
                copyLibsDirectory = new File(proj.getBasedir(), PROJECT_ROOT_TARGET_LIBS);
            } else {
                // test the user defined copyLibsDirectory parameter for use in a container
                String copyLibsPath = copyLibsDirectory.getCanonicalPath();
                if (!copyLibsPath.startsWith(projectRoot)) {
                    // Flag an error but allow processing to continue in case dependencies, if any, are not actually referenced by the app.
                    getLog().error("The directory indicated by the copyLibsDirectory parameter must be within the Maven project directory when the container option is specified.");
                }
            }
        } catch (IOException e) {
            // an IOException here should fail the build
            throw new MojoExecutionException("Could not resolve the canonical path of the Maven project or the directory specified in the copyLibsDirectory parameter. Exception message:" + e.getMessage(), e);
        }
    }

    // install war project artifact using loose application configuration file
    protected void installLooseConfigWar(MavenProject proj, LooseConfigData config, boolean container) throws MojoExecutionException, IOException {
        // return error if webapp contains java source but it is not compiled yet.
        File dir = new File(proj.getBuild().getOutputDirectory());
        if (!dir.exists() && containsJavaSource(proj)) {
            throw new MojoExecutionException(
                    MessageFormat.format(messages.getString("error.project.not.compile"), proj.getId()));
        }

        if (container) {
            setLooseProjectRootForContainer(proj, config);
        }

        LooseWarApplication looseWar = new LooseWarApplication(proj, config, getLog());

        if (looseWar.isExploded()) {
        	
        	// Validate maven-war-plugin version
        	Plugin warPlugin = getPlugin("org.apache.maven.plugins", "maven-war-plugin");
            if (!validatePluginVersion(warPlugin.getVersion(), "3.3.2")) {
                getLog().warn(
                        "Exploded WAR functionality is enabled. Please use maven-war-plugin version 3.3.2 or greater for best results.");
        	}

            // Don't especially need to run it exactly here, but in debugger we can see what we have
            runExplodedMojo();

            ////////////////////////////////////
            // The order matters and establishes a well-defined precedence as documented: https://www.ibm.com/docs/en/was-liberty/base?topic=liberty-loose-applications
            //
            // ".. If you have two files with the same target location in the loose archive, the first occurrence of the file is used.
            // The first occurrence is based on a top-down approach to reading the elements of the loose application configuration file..."
            //
            // Because the flow is so complicated we may have cases where we are applying filtering where one location contains a filtered
            // version of a file and another potentially has an unfiltered one, and in such cases we need to make sure the filtered version takes
            // precedence.
            //
            // In certain cases, like step 1. below we avoid writing a location into the loose app XML because we don't want an unfiltered
            // to take precedence and prevent the filtered value from taking effect.
            //
            ////////////////////////////////////

            // 1. Add source paths for the source dir and non-filtered web resources.  Since there could be overlap, i.e. the source dir
            // could also be configured as a web resource, we combine these into a single step.
            //
            // We'll already have the runtime application monitor watching for file changes, and we don't want to set up the more expensive
            // dev mode type of watching.
            looseWar.addNonFilteredSourceAndWebResourcesPaths();

            // 2. target classes - this allows non-deploy mode cases (e.g. non-deploy cases such as `mvn compile` or m2e update in Eclipse)
            // to pick up Java class updates upon compilation.
            looseWar.addOutputDir(looseWar.getDocumentRoot(), new File(proj.getBuild().getOutputDirectory()), "/WEB-INF/classes");

            //////////////////////////
            // 3. Finally add the exploded dir
            //
            // In order to dynamically reflect changes in non-filtered web app source, this needs to go AFTER the unfiltered source entries above, since 
            // changes in these un-monitored directories will NOT cause a new 'exploded' goal execution, so the updated content in the unmonitored source will
            // now be newer than the stale data in the webapp dir folder.
            //
            // Might need more consideration in special case where filteringDD is disabled but also a webResources resource is set up for the war source dir (to get non-DD stuff like beans.xml).
            //
            // Perhaps this is a special case we can document "don't do this"..or perhaps the war source dir (default = src/main/webapp) should ALWAYS be monitored, and only extra web resources directories should
            // be subject to the test of monitoring only if filtering is enabled.
            //////////////////////////
            looseWar.addOutputDir(looseWar.getDocumentRoot(), looseWar.getWebAppDirectory(), "/");

        } else {

            // 1.
            looseWar.addSourceDir();
            // 2.
            looseWar.addOutputDir(looseWar.getDocumentRoot(), new File(proj.getBuild().getOutputDirectory()),
                    "/WEB-INF/classes");

            // 3. retrieve the directories defined as resources in the maven war plugin
            //
            // TODO - It would be cleaner to avoid duplicating the source dir in the case it also appears as a web resource, like we do in the exploded case.
            // If this ever became an issue we could combine this with step 1. above.  However at the moment it doesn't seem worth the risk of making a change
            // in such a key area.
            looseWar.addAllWebResourcesConfigurationPaths();

            // 4. retrieves dependent library jar files
            addEmbeddedLib(looseWar.getDocumentRoot(), proj, looseWar, "/WEB-INF/lib/");

        }

        // add Manifest file
        File manifestFile = MavenProjectUtil.getManifestFile(proj, "maven-war-plugin");
        try {
            looseWar.addManifestFile(manifestFile);
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to install loose application. Error adding manifest file to loose war configuration file.", e);
        }
    }

    // install ear project artifact using loose application configuration file
    protected void installLooseConfigEar(MavenProject proj, LooseConfigData config, boolean container) throws MojoExecutionException, IOException {
        if (container) {
            setLooseProjectRootForContainer(proj, config);
        }

        LooseEarApplication looseEar = new LooseEarApplication(proj, config);
        looseEar.addSourceDir();
        looseEar.addApplicationXmlFile();

        Set<Artifact> artifacts = proj.getArtifacts();
        getLog().debug("Number of compile dependencies for " + proj.getArtifactId() + " : " + artifacts.size());

        for (Artifact artifact : artifacts) {
            if ("compile".equals(artifact.getScope()) || "runtime".equals(artifact.getScope())) {
                if (!isReactorMavenProject(artifact)) {
                    if (looseEar.isEarSkinnyWars() && "war".equals(artifact.getType())) {
                        throw new MojoExecutionException(
                                "Unable to create loose configuration for the EAR application with skinnyWars package from "
                                        + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":"
                                        + artifact.getVersion()
                                        + ". Please set the looseApplication configuration parameter to false and try again.");
                    }
                    looseEar.addModuleFromM2(resolveArtifact(artifact));
                } else {
                    MavenProject dependencyProject = getReactorMavenProject(artifact);
                    switch (artifact.getType()) {
                    case "jar":
                        looseEar.addJarModule(dependencyProject, artifact);
                        break;
                    case "ejb":
                        looseEar.addEjbModule(dependencyProject, artifact);
                        break;
                    case "bundle":
                        looseEar.addBundleModule(dependencyProject, artifact);
                        break;
                    case "war":
                        Element warArchive = looseEar.addWarModule(dependencyProject, artifact,
                                        getWarSourceDirectory(dependencyProject));
                        if (looseEar.isEarSkinnyWars()) {
                            // add embedded lib only if they are not a compile dependency in the ear
                            // project.
                            addSkinnyWarLib(warArchive, dependencyProject, looseEar);
                        } else {
                            addEmbeddedLib(warArchive, dependencyProject, looseEar, "/WEB-INF/lib/");
                        }
                        break;
                    case "rar":
                        Element rarArchive = looseEar.addRarModule(dependencyProject, artifact);
                        addEmbeddedLib(rarArchive, dependencyProject, looseEar, "/");
                        break;
                    default:
                        // use the artifact from local .m2 repo
                        looseEar.addModuleFromM2(resolveArtifact(artifact));
                        break;
                    }
                }
            }
        }

        // add Manifest file
        File manifestFile = MavenProjectUtil.getManifestFile(proj, "maven-ear-plugin");
        try {
            looseEar.addManifestFile(manifestFile);
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to install loose application. Error adding manifest file to loose ear configuration file.", e);
        }
    }

    private boolean shouldValidateAppStart() throws MojoExecutionException {
        try {
            return new File(serverDirectory.getCanonicalPath()  + "/workarea/.sRunning").exists();
        } catch (IOException ioe) {
            throw new MojoExecutionException("Could not get the server directory to determine the state of the server.");
        }
    }

    protected void verifyAppStarted(String appFile) throws MojoExecutionException {
        if (shouldValidateAppStart()) {
            String appName = appFile.substring(0, appFile.lastIndexOf('.'));
            if (getAppsDirectory().equals("apps")) {

                File serverXML = new File(serverDirectory, "server.xml");

                try {
                    Map<String, File> libertyDirPropertyFiles = getLibertyDirectoryPropertyFiles();
                    CommonLogger logger = new CommonLogger(getLog());
                    setLog(logger.getLog());
                    scd = getServerConfigDocument(logger, libertyDirPropertyFiles);

                    //appName will be set to a name derived from appFile if no name can be found.
                    appName = scd.findNameForLocation(appFile);
                } catch (Exception e) {
                    getLog().warn(e.getLocalizedMessage());
                    getLog().debug(e);
                } 
            }

            ServerTask serverTask = initializeJava();
            if (serverTask.waitForStringInLog(START_APP_MESSAGE_REGEXP + appName, timeout * 1000, new File(new File(outputDirectory, serverName), "logs/messages.log")) == null) {
                throw new MojoExecutionException(MessageFormat.format(messages.getString("error.deploy.fail"), appName));
            }
        }
    }

    private void addEmbeddedLib(Element parent, MavenProject warProject, LooseApplication looseApp, String dir)
            throws MojoExecutionException, IOException {
        Set<Artifact> artifacts = warProject.getArtifacts();
        getLog().debug("Number of compile dependencies for " + warProject.getArtifactId() + " : " + artifacts.size());

        for (Artifact artifact : artifacts) {
            if ( ("compile".equals(artifact.getScope()) || "runtime".equals(artifact.getScope())) && 
                 ("jar".equals(artifact.getType()) || "jar".equals(artifact.getArtifactHandler().getExtension())) ) {
                addLibrary(parent, looseApp, dir, artifact);
            }
        }
    }

    private void addSkinnyWarLib(Element parent, MavenProject warProject, LooseEarApplication looseEar) throws MojoExecutionException, IOException {
        Set<Artifact> artifacts = warProject.getArtifacts();
        getLog().debug("Number of compile dependencies for " + warProject.getArtifactId() + " : " + artifacts.size());

        for (Artifact artifact : artifacts) {
            // skip the embedded library if it is included in the lib directory of the ear
            // package
            if (("compile".equals(artifact.getScope()) || "runtime".equals(artifact.getScope()))
                    && ("jar".equals(artifact.getType()) || "jar".equals(artifact.getArtifactHandler().getExtension()))
                    && !looseEar.isEarDependency(artifact)) {
                addLibrary(parent, looseEar, "/WEB-INF/lib/", artifact);
            }
        }
    }

    private void addLibrary(Element parent, LooseApplication looseApp, String dir, Artifact artifact) throws MojoExecutionException, IOException {
        {
            if (isReactorMavenProject(artifact)) {
                MavenProject dependProject = getReactorMavenProject(artifact);
                String artifactFileName = getPreDeployAppFileName(dependProject);
                Element archive = looseApp.addArchive(parent, dir + artifactFileName);
                looseApp.addOutputDir(archive, new File(dependProject.getBuild().getOutputDirectory()), "/");

                //Check if reactor project generates an ejb, bundle or jar 
                String archivePlugin = "maven-jar-plugin";
                String packaging = dependProject.getPackaging();
                if (packaging.equalsIgnoreCase("ejb")) {
                    archivePlugin = "maven-ejb-plugin";
                } else if (packaging.equalsIgnoreCase("bundle")) {
                    archivePlugin = "maven-bundle-plugin";
                }

                File manifestFile = MavenProjectUtil.getManifestFile(dependProject, archivePlugin);

                String dependProjectTargetDir = dependProject.getBuild().getDirectory();

                try {
                    looseApp.addManifestFileWithParent(archive, manifestFile, dependProjectTargetDir);
                } catch (Exception e) {
                    throw new MojoExecutionException("Error adding manifest file with parent: "+manifestFile.getCanonicalPath(), e);
                }
            } else {
                resolveArtifact(artifact);
                if(copyLibsDirectory != null) {
                    if(!copyLibsDirectory.exists()) {
                        copyLibsDirectory.mkdirs();
                    }
                    if(!copyLibsDirectory.isDirectory()) {
                        throw new MojoExecutionException("copyLibsDirectory must be a directory");
                    }
                    else {
                        looseApp.getConfig().addFile(parent, artifact.getFile(), dir + artifact.getFile().getName(), copyLibsDirectory);
                    }
                }
                else {
                    looseApp.getConfig().addFile(parent, artifact.getFile(), dir + artifact.getFile().getName());
                }
            }
        }
    }

    private boolean containsJavaSource(MavenProject proj) {
        List<String> srcDirs = proj.getCompileSourceRoots();
        for (String dir : srcDirs) {
            File javaSourceDir = new File(dir);
            if (javaSourceDir.exists() && javaSourceDir.isDirectory() && containsJavaSource(javaSourceDir)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsJavaSource(File dir) {
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isFile() && file.getName().toLowerCase().endsWith(".java")) {
                return true;
            } else if (file.isDirectory()) {
                return containsJavaSource(file);
            }
        }
        return false;
    }

    protected void validateAppConfig(String fullyQualifiedFileName, String fileName, String artifactId) throws MojoExecutionException {
        validateAppConfig(fullyQualifiedFileName, fileName, artifactId, false);
    }

    protected void validateAppConfig(String fullyQualifiedFileName, String fileName, String artifactId, boolean isSpringBootApp) throws MojoExecutionException {
        String appsDir = getAppsDirectory();
        if (appsDir.equalsIgnoreCase("apps") && !isAppConfiguredInSourceServerXml(fullyQualifiedFileName, fileName)) {
            // add application configuration
            getLog().info("Could not find application "+fileName+" in server.xml locations.");
            applicationXml.createApplicationElement(fileName, artifactId, isSpringBootApp);
        } else if (appsDir.equalsIgnoreCase("dropins") && isAppConfiguredInSourceServerXml(fullyQualifiedFileName, fileName))
            throw new MojoExecutionException(messages.getString("error.install.app.dropins.directory"));
    }

    /**
     * Executes the SpringBootUtilTask to thin the Spring Boot application
     * executable archive and place the thin archive and lib.index.cache in the
     * specified location.
     * 
     * @param installDirectory
     *            : Installation directory of Liberty profile.
     * @param fatArchiveSrcLocation
     *            : Source Spring Boot FAT executable archive location.
     * @param thinArchiveTargetLocation
     *            : Target thin archive location.
     * @param libIndexCacheTargetLocation
     *            : Library cache location.
     */
    protected void invokeSpringBootUtilCommand(File installDirectory, String fatArchiveSrcLocation,
            String thinArchiveTargetLocation, String libIndexCacheTargetLocation) throws MojoExecutionException, IOException {
        SpringBootUtilTask springBootUtilTask = (SpringBootUtilTask) ant
                .createTask("antlib:io/openliberty/tools/ant:springBootUtil");
        if (springBootUtilTask == null) {
            throw new IllegalStateException(
                    MessageFormat.format(messages.getString("error.dependencies.not.found"), "springBootUtil"));
        }

        Validate.notNull(fatArchiveSrcLocation, "Spring Boot source archive location cannot be null");
        Validate.notNull(thinArchiveTargetLocation, "Target thin archive location cannot be null");
        Validate.notNull(libIndexCacheTargetLocation, "Library cache location cannot be null");

        springBootUtilTask.setInstallDir(installDirectory);
        springBootUtilTask.setTargetThinAppPath(thinArchiveTargetLocation);
        springBootUtilTask.setSourceAppPath(fatArchiveSrcLocation);
        springBootUtilTask.setTargetLibCachePath(libIndexCacheTargetLocation);
        springBootUtilTask.execute();
    }

    protected boolean isUtilityAvailable(File installDirectory, String utilityName) {
            String utilFileName = OSUtil.isWindows() ? utilityName+".bat" : utilityName;
            File installUtil = new File(installDirectory, "bin/"+utilFileName);
            return installUtil.exists();
    }

    protected void installSpringBootFeatureIfNeeded(File installDirectory) throws MojoExecutionException {
        Process pr = null;
        BufferedReader in = null;
        try {
            if (!isUtilityAvailable(installDirectory, "springBootUtility") && isUtilityAvailable(installDirectory, "featureUtility")) {
                String fileSuffix = OSUtil.isWindows() ? ".bat" : "";
                File installUtil = new File(installDirectory, "bin/featureUtility"+fileSuffix);

                // only install springBoot feature that matches required version
                int springBootMajorVersion = SpringBootUtil.getSpringBootMavenPluginVersion(project);
                String sbFeature = SpringBootUtil.getLibertySpringBootFeature(springBootMajorVersion);
                if (sbFeature != null) {
                    getLog().info("Required springBootUtility not found in Liberty installation. Installing feature "+sbFeature+" to enable it.");
                    StringBuilder sb = new StringBuilder();
                    String installUtilCmd;
                    if (OSUtil.isWindows()) {
                        installUtilCmd = "\"" + installDirectory + "\\bin\\featureUtility.bat\"";
                    } else {
                        installUtilCmd = installDirectory + "/bin/featureUtility";
                    }
                    ProcessBuilder pb = new ProcessBuilder(installUtilCmd, "installFeature", sbFeature, "--acceptLicense");
                    pr = pb.start();

                    in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
                    String line;
                    while ((line = in.readLine()) != null) {
                        sb.append(line).append(System.lineSeparator());
                    }

                    boolean exited = pr.waitFor(300, TimeUnit.SECONDS);
                    if(!exited) { // Command did not exit in time
                        throw new MojoExecutionException("featureUtility command timed out");
                    }

                    int exitValue = pr.exitValue();
                    if (exitValue != 0) {
                        throw new MojoExecutionException("featureUtility exited with return code " + exitValue +". The featureUtility command run was `"+installUtil+" installFeature "+sbFeature+" --acceptLicense`");
                    }
                }
            }
        } catch (IOException ex) {
            throw new MojoExecutionException("featureUtility error: " + ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new MojoExecutionException("featureUtility error: " + ex);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
            if (pr != null) {
                pr.destroy();
            }
        }
    }

    protected boolean matches(Artifact artifact, Dependency assemblyArtifact) {
        return artifact.getGroupId().equals(assemblyArtifact.getGroupId())
                && artifact.getArtifactId().equals(assemblyArtifact.getArtifactId())
                && artifact.getType().equals(assemblyArtifact.getType());
    }
    
    protected boolean isSupportedType(String type) {
        boolean supported = false;
        switch (type) {
            case "ear":
            case "war":
            case "rar":
            case "eba":
            case "esa":
            case "liberty-assembly":
                supported = true;
                break;
            default:
                break;
        }
        return supported;
    }

    public static boolean isSupportedLooseAppType(String type) {
        boolean supported = false;
        switch (type) {
            case "ear":
            case "war":
            case "liberty-assembly":
            case "pom":
                supported = true;
                break;
            default:
                break;
        }
        return supported;
    }

}
