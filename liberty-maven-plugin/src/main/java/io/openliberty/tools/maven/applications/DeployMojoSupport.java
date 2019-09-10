/**
 * (C) Copyright IBM Corporation 2016, 2019.
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

import java.io.File;
import java.text.MessageFormat;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.taskdefs.Copy;
import org.codehaus.mojo.pluginsupport.util.ArtifactItem;
import org.w3c.dom.Element;

import io.openliberty.tools.ant.DeployTask;
import io.openliberty.tools.ant.SpringBootUtilTask;
import io.openliberty.tools.maven.server.PluginConfigSupport;
import io.openliberty.tools.maven.utils.MavenProjectUtil;
import io.openliberty.tools.common.plugins.config.ApplicationXmlDocument;
import io.openliberty.tools.common.plugins.config.LooseApplication;
import io.openliberty.tools.common.plugins.config.LooseConfigData;

import io.openliberty.tools.ant.DeployTask;
import io.openliberty.tools.ant.SpringBootUtilTask;
import io.openliberty.tools.common.plugins.config.ApplicationXmlDocument;
import io.openliberty.tools.common.plugins.config.LooseApplication;
import io.openliberty.tools.common.plugins.config.LooseConfigData;

/**
 * Support for installing and deploying applications to a Libert server.
 */
public class DeployMojoSupport extends PluginConfigSupport {
    /**
     * A file which points to a specific module's war | ear | eba | zip archive location
     */
    protected File appArchive;

    /**
     * Maven coordinates of an application to deploy. This is best listed as a dependency,
     * in which case the version can be omitted.
     */
    protected Artifact appArtifact;

    /**
     * Timeout to verify deploy successfully, in seconds.
     */
    @Parameter(property = "timeout", defaultValue = "40")
    protected int timeout = 40;
    
    /**
     *  The file name of the deployed application in the `dropins` directory.
     */
    @Parameter(property = "appDeployName")
    protected String appDeployName;


    protected ApplicationXmlDocument applicationXml = new ApplicationXmlDocument();

    protected void installApp(Artifact artifact) throws Exception {
    
        //The only time this would happen is if the project artifact is null.
        //This method is called with reactor projects when dealing with dependency projects.
        //Dependency projects will not pass artifacts with null files. See installDependencies()...
        if (artifact.getFile() == null || artifact.getFile().isDirectory()) {
            if (appArchive != null) {
                artifact.setFile(appArchive);
            } else if (appArtifact != null) {
                artifact = appArtifact;
            } else {
                String warName = getAppFileName(project);
                File f = new File(project.getBuild().getDirectory() + "/" + warName);
                artifact.setFile(f);
            }
        }

        if (!artifact.getFile().exists()) {
            throw new MojoExecutionException(messages.getString("error.install.app.missing"));
        }

        File destDir = new File(serverDirectory, getAppsDirectory());
        log.info(MessageFormat.format(messages.getString("info.install.app"), artifact.getFile().getCanonicalPath()));

        Copy copyFile = (Copy) ant.createTask("copy");
        copyFile.setFile(artifact.getFile());
        String fileName = artifact.getFile().getName();
        if (stripVersion) {
            fileName = stripVersionFromName(fileName, artifact.getBaseVersion());
            copyFile.setTofile(new File(destDir, fileName));
        } else {
            copyFile.setTodir(destDir);
        }

        // validate application configuration if appsDirectory="dropins" or inject
        // webApplication
        // to target server.xml if not found for appsDirectory="apps"
        validateAppConfig(fileName, artifact.getArtifactId());

        deleteApplication(new File(serverDirectory, "apps"), artifact.getFile());
        deleteApplication(new File(serverDirectory, "dropins"), artifact.getFile());
        // application can be expanded if server.xml configure with <applicationManager
        // autoExpand="true"/>
        deleteApplication(new File(serverDirectory, "apps/expanded"), artifact.getFile());
        copyFile.execute();
    }

    protected void deployApp() throws Exception {
        if (appArtifact != null) {
            appArchive = appArtifact.getFile();
            log.info(MessageFormat.format(messages.getString("info.variable.set"), "artifact based application", appArtifact));
            if (stripVersion) { //Setting deploy name to stripped file name
                appDeployName = stripVersionFromName(appArchive.getName(), appArtifact.getBaseVersion());
            }
        } else if (appArchive != null) { //Don't need to handle stripped version here. Will have been stripped as part of loose app generation
            log.info(MessageFormat.format(messages.getString("info.variable.set"), "non-artifact based application", appArchive));
        } else if (project.getArtifact() != null) {
            appArchive = project.getArtifact().getFile();
            if (stripVersion) {
                appDeployName = stripVersionFromName(appArchive.getName(), project.getArtifact().getBaseVersion());
            }
        } else {
            throw new MojoExecutionException("Could not deploy application. No appArchive or appArtifact set. No project artifact found.");
        }

        if (!appArchive.exists() || appArchive.isDirectory()) {
            throw new MojoExecutionException("Application file does not exist or is a directory: " + appArchive);
        }

        log.info(MessageFormat.format(messages.getString("info.deploy.app"), appArchive.getCanonicalPath()));
        DeployTask deployTask = (DeployTask) ant.createTask("antlib:io/openliberty/tools/ant:deploy");
        if (deployTask == null) {
            throw new IllegalStateException(MessageFormat.format(messages.getString("error.dependencies.not.found"), "deploy"));
        }

        deployTask.setInstallDir(installDirectory);
        deployTask.setServerName(serverName);
        deployTask.setUserDir(userDirectory);
        deployTask.setOutputDir(outputDirectory);
        deployTask.setFile(appArchive);
        deployTask.setDeployName(appDeployName);
        // Convert from seconds to milliseconds
        deployTask.setTimeout(Long.toString(timeout*1000));
        deployTask.execute();
    }

    // install war project artifact using loose application configuration file
    protected void installLooseConfigWar(MavenProject proj, LooseConfigData config) throws Exception {
        // return error if webapp contains java source but it is not compiled yet.
        File dir = new File(proj.getBuild().getOutputDirectory());
        if (!dir.exists() && containsJavaSource(proj)) {
            throw new MojoExecutionException(
                    MessageFormat.format(messages.getString("error.project.not.compile"), proj.getId()));
        }

        LooseWarApplication looseWar = new LooseWarApplication(proj, config);
        looseWar.addSourceDir(proj);
        looseWar.addOutputDir(looseWar.getDocumentRoot(), new File(proj.getBuild().getOutputDirectory()),
                "/WEB-INF/classes");

        // retrieves dependent library jar files
        addEmbeddedLib(looseWar.getDocumentRoot(), proj, looseWar, "/WEB-INF/lib/");

        // add Manifest file
        File manifestFile = MavenProjectUtil.getManifestFile(proj, "maven-war-plugin");
        looseWar.addManifestFile(manifestFile);
    }

    // install ear project artifact using loose application configuration file
    protected void installLooseConfigEar(MavenProject proj, LooseConfigData config) throws Exception {
        LooseEarApplication looseEar = new LooseEarApplication(proj, config);
        looseEar.addSourceDir();
        looseEar.addApplicationXmlFile();

        Set<Artifact> artifacts = proj.getArtifacts();
        log.debug("Number of compile dependencies for " + proj.getArtifactId() + " : " + artifacts.size());

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
                        looseEar.addJarModule(dependencyProject);
                        break;
                    case "ejb":
                        looseEar.addEjbModule(dependencyProject);
                        break;
                    case "war":
                        Element warArchive = looseEar.addWarModule(dependencyProject,
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
                        Element rarArchive = looseEar.addRarModule(dependencyProject);
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
        looseEar.addManifestFile(manifestFile);
    }

    private void addEmbeddedLib(Element parent, MavenProject proj, LooseApplication looseApp, String dir)
            throws Exception {
        Set<Artifact> artifacts = proj.getArtifacts();
        log.debug("Number of compile dependencies for " + proj.getArtifactId() + " : " + artifacts.size());

        for (Artifact artifact : artifacts) {
            if (("compile".equals(artifact.getScope()) || "runtime".equals(artifact.getScope()))
                    && "jar".equals(artifact.getType())) {
                addlibrary(parent, looseApp, dir, artifact);
            }
        }
    }

    private void addSkinnyWarLib(Element parent, MavenProject proj, LooseEarApplication looseEar) throws Exception {
        Set<Artifact> artifacts = proj.getArtifacts();
        log.debug("Number of compile dependencies for " + proj.getArtifactId() + " : " + artifacts.size());

        for (Artifact artifact : artifacts) {
            // skip the embedded library if it is included in the lib directory of the ear
            // package
            if (("compile".equals(artifact.getScope()) || "runtime".equals(artifact.getScope()))
                    && "jar".equals(artifact.getType()) && !looseEar.isEarDependency(artifact)) {
                addlibrary(parent, looseEar, "/WEB-INF/lib/", artifact);
            }
        }
    }

    private void addlibrary(Element parent, LooseApplication looseApp, String dir, Artifact artifact) throws Exception {
        {
            if (isReactorMavenProject(artifact)) {
                MavenProject dependProject = getReactorMavenProject(artifact);
                Element archive = looseApp.addArchive(parent, dir + dependProject.getBuild().getFinalName() + ".jar");
                looseApp.addOutputDir(archive, new File(dependProject.getBuild().getOutputDirectory()), "/");
                
                File manifestFile = MavenProjectUtil.getManifestFile(dependProject, "maven-jar-plugin");
                looseApp.addManifestFileWithParent(archive, manifestFile);
            } else {
                resolveArtifact(artifact);
                looseApp.getConfig().addFile(parent, artifact.getFile(), dir + artifact.getFile().getName());
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

    // get loose application configuration file name for project artifact
    protected String getLooseConfigFileName(MavenProject project) {
        return getAppFileName(project) + ".xml";
    }

    // get loose application configuration file name for project artifact
    private String getAppFileName(MavenProject project) {
        String name = project.getBuild().getFinalName() + "." + project.getPackaging();
        if (project.getPackaging().equals("liberty-assembly")) {
            name = project.getBuild().getFinalName() + ".war";
        }
        if (stripVersion) {
            name = stripVersionFromName(name, project.getVersion());
        }
        return name;
    }

    protected void validateAppConfig(String fileName, String artifactId) throws Exception {
        validateAppConfig(fileName, artifactId, false);
    }

    protected void validateAppConfig(String fileName, String artifactId, boolean isSpringBootApp) throws Exception {
        String appsDir = getAppsDirectory();
        if (appsDir.equalsIgnoreCase("apps") && !isAppConfiguredInSourceServerXml(fileName)) {
            // add application configuration
            applicationXml.createApplicationElement(fileName, artifactId, isSpringBootApp);
        } else if (appsDir.equalsIgnoreCase("dropins") && isAppConfiguredInSourceServerXml(fileName))
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
            String thinArchiveTargetLocation, String libIndexCacheTargetLocation) throws Exception {
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

    protected boolean matches(Artifact artifact, ArtifactItem assemblyArtifact) {
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
}
