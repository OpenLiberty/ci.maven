/**
 * (C) Copyright IBM Corporation 2016, 2017.
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
package net.wasdev.wlp.maven.plugins.applications;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.taskdefs.Copy;
import org.w3c.dom.Element;

import net.wasdev.wlp.maven.plugins.ApplicationXmlDocument;
import net.wasdev.wlp.maven.plugins.server.PluginConfigSupport;

/**
 * Install artifact into Liberty server support.
 */
public class InstallAppMojoSupport extends PluginConfigSupport {

    protected ApplicationXmlDocument applicationXml = new ApplicationXmlDocument();

    protected void installApp(Artifact artifact, boolean thin) throws Exception {
        File destDir = new File(serverDirectory, getAppsDirectory());

        if (artifact.getFile() == null || artifact.getFile().isDirectory() || thin) {
            String warName = getAppFileName(project);
            if (thin) {
                warName = "thin-" + warName;
                // When the apps directory is "dropins" place the thin application in
                // dropins/spring folder and
                // when the apps directory is "apps" place the thin application in apps folder
                if (destDir.getName().equalsIgnoreCase("dropins")) {
                    destDir = new File(destDir, "spring");
                }
                copyLibIndexCache();
            }
            File f = new File(project.getBuild().getDirectory() + "/" + warName);
            artifact.setFile(f);
        }

        if (!artifact.getFile().exists()) {
            throw new MojoExecutionException(messages.getString("error.install.app.missing"));
        }

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
        validateAppConfig(fileName, artifact.getArtifactId(), thin);

        deleteApplication(new File(serverDirectory, "apps"), artifact.getFile());
        deleteApplication(new File(serverDirectory, "dropins"), artifact.getFile());
        deleteApplication(new File(serverDirectory, "dropins/spring"), artifact.getFile());
        // application can be expanded if server.xml configure with <applicationManager
        // autoExpand="true"/>
        deleteApplication(new File(serverDirectory, "apps/expanded"), artifact.getFile());
        copyFile.execute();
    }

    private void copyLibIndexCache() throws IOException {
        String libIndexCacheName = "lib.index.cache";
        File sourceLibDir = new File(project.getBuild().getDirectory(), libIndexCacheName);
        File destLibDir = new File(workareaSpringDirectory, libIndexCacheName);
        FileUtils.copyDirectory(sourceLibDir, destLibDir);
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
        looseWar.addOutputDir(looseWar.getDocumentRoot(), proj, "/WEB-INF/classes");

        // retrieves dependent library jar files
        addEmbeddedLib(looseWar.getDocumentRoot(), proj, looseWar, "/WEB-INF/lib/");

        // add Manifest file
        looseWar.addManifestFile(proj, "maven-war-plugin");
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
                                getWarSourceDirectory(dependencyProject).getCanonicalPath());
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
        looseEar.addManifestFile(proj, "maven-ear-plugin");
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
                looseApp.addOutputDir(archive, dependProject, "/");
                looseApp.addManifestFile(archive, dependProject, "maven-jar-plugin");
            } else {
                resolveArtifact(artifact);
                looseApp.getConfig().addFile(parent, artifact.getFile().getAbsolutePath(),
                        dir + artifact.getFile().getName());
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
        String name = project.getBuild().getFinalName();
        if (project.getPackaging().equals("liberty-assembly")) {
            name = name + ".war";
        } else {
            name = name + "." + project.getPackaging();
        }
        if (stripVersion) {
            name = stripVersionFromName(name, project.getVersion());
        }
        return name;
    }

    protected void validateAppConfig(String fileName, String artifactId, boolean thin) throws Exception {
        String appsDir = getAppsDirectory();
        if (appsDir.equalsIgnoreCase("apps") && !isAppConfiguredInSourceServerXml(fileName)) {
            // add application configuration
            applicationXml.createApplicationElement(fileName, artifactId, thin);
        } else if (appsDir.equalsIgnoreCase("dropins") && isAppConfiguredInSourceServerXml(fileName))
            throw new MojoExecutionException(messages.getString("error.install.app.dropins.directory"));
    }
}
