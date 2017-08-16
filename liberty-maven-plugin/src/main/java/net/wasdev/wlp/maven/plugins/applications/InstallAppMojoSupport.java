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
import java.text.MessageFormat;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
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
    
    protected void installApp(Artifact artifact) throws Exception {
        
        if (artifact.getFile() == null || artifact.getFile().isDirectory()) {
            String warName = getAppFileName(project);
            File f = new File(project.getBuild().getDirectory() + "/" + warName);
            artifact.setFile(f);
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
            fileName = stripVersionFromName(fileName, artifact.getVersion());
            copyFile.setTofile(new File(destDir, fileName));
        } else {
            copyFile.setTodir(destDir);
        }
        
        // validate application configuration if appsDirectory="dropins" or inject webApplication
        // to target server.xml if not found for appsDirectory="apps"
        validateAppConfig(fileName, artifact.getArtifactId());
        
        deleteApplication(new File(serverDirectory, "apps"), artifact.getFile());
        deleteApplication(new File(serverDirectory, "dropins"), artifact.getFile());
        // application can be expanded if server.xml configure with <applicationManager autoExpand="true"/>
        deleteApplication(new File(serverDirectory, "apps/expanded"), artifact.getFile());
        copyFile.execute();
    }
    
    // install war project artifact using loose application configuration file
    protected void installLooseConfigWar(MavenProject proj, LooseConfigData config) throws Exception {
        // return error if webapp contains java source but it is not compiled yet.
        File dir = new File(proj.getBuild().getOutputDirectory());
        if (!dir.exists() && containsJavaSource(proj)) {
            throw new MojoExecutionException(MessageFormat.format(messages.getString("error.project.not.compile"),
                    proj.getId()));
        }
        
        LooseWarApplication looseWar = new LooseWarApplication(proj, config);
        looseWar.addSourceDir(proj);
        looseWar.addOutputDir(looseWar.getDocumentRoot(), proj, "/WEB-INF/classes");
        
        // retrieves dependent library jar files
        addWarEmbeddedLib(looseWar.getDocumentRoot(), proj, looseWar);
        
        // add Manifest file
        looseWar.addManifestFile(proj, "maven-war-plugin");
    }
    
    // install ear project artifact using loose application configuration file
    protected void installLooseConfigEar(MavenProject proj, LooseConfigData config) throws Exception {
        LooseEarApplication looseEar = new LooseEarApplication(proj, config);
        looseEar.addSourceDir();
        looseEar.addApplicationXmlFile();
        
        @SuppressWarnings("unchecked")
        List<Dependency> deps = proj.getDependencies();
        for (Dependency dep : deps) {
            if ("compile".equals(dep.getScope())) {
                MavenProject dependencyProject = getMavenProject(dep.getGroupId(), dep.getArtifactId(),
                        dep.getVersion());
                if (dependencyProject.getBasedir() == null || !dependencyProject.getBasedir().exists()) {
                    looseEar.addModuleFromM2(dependencyProject, resolveArtifact(dependencyProject.getArtifact()));
                } else {
                    switch (dep.getType()) {
                        case "jar":
                            looseEar.addJarModule(dependencyProject);
                            break;
                        case "ejb":
                            looseEar.addEjbModule(dependencyProject);
                            break;
                        case "war":
                            Element warArchive = looseEar.addWarModule(dependencyProject,
                                    getWarSourceDirectory(dependencyProject).getCanonicalPath());
                            addWarEmbeddedLib(warArchive, dependencyProject, looseEar);
                            break;
                        case "app-client":
                            // TODO: Client module
                        case "rar":
                            // TODO: Resource Adapter module
                        default:
                            // use the artifact from local .m2 repo
                            looseEar.addModuleFromM2(dependencyProject,
                                    resolveArtifact(dependencyProject.getArtifact()));
                            break;
                    }
                }
            }
        }
        
        // add Manifest file
        looseEar.addManifestFile(proj, "maven-ear-plugin");
    }
        
    private void addWarEmbeddedLib(Element parent, MavenProject proj, LooseApplication looseApp) throws Exception {
        @SuppressWarnings("unchecked")
        List<Dependency> deps = proj.getDependencies();
        for (Dependency dep : deps) {
            if ("compile".equals(dep.getScope()) && "jar".equals(dep.getType())) {
                MavenProject dependProject = getMavenProject(dep.getGroupId(), dep.getArtifactId(), dep.getVersion());
                if (dependProject.getBasedir() != null && dependProject.getBasedir().exists()) {
                    Element archive = looseApp.addArchive(parent,
                            "/WEB-INF/lib/" + dependProject.getBuild().getFinalName() + ".jar");
                    looseApp.addOutputDir(archive, dependProject, "/");
                    looseApp.addManifestFile(archive, dependProject, "maven-jar-plugin");
                } else {
                    looseApp.getConfig().addFile(parent,
                            resolveArtifact(dependProject.getArtifact()).getFile().getAbsolutePath(),
                            "/WEB-INF/lib/" + resolveArtifact(dependProject.getArtifact()).getFile().getName());
                }
            }
        }
    }
    
    private boolean containsJavaSource(MavenProject proj) {
        @SuppressWarnings("unchecked")
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
        String appsDir = getAppsDirectory();
        if (appsDir.equalsIgnoreCase("apps") && !isAppConfiguredInSourceServerXml(fileName)) {
            // add application configuration
            applicationXml.createApplicationElement(fileName, artifactId);
        }
        else if (appsDir.equalsIgnoreCase("dropins") && isAppConfiguredInSourceServerXml(fileName))
            throw new MojoExecutionException(messages.getString("error.install.app.dropins.directory"));
    }
}
