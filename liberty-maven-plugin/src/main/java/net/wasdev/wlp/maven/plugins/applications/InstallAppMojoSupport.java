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
import java.util.ArrayList;
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
            String warName = getWarFileName(project);
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
        
        // jar libraries
        List<Dependency> jarModules = getDependentModules(proj, "jar");
        log.debug("InstallAppMojoSupport: installLooseConfigEnterpriseApp() -> Nubmber of Jar modules: "
                + jarModules.size());
        addEarJarModules(jarModules, looseEar);
        
        // EJB modules
        List<Dependency> ejbModules = getDependentModules(proj, "ejb");
        log.debug("InstallAppMojoSupport: installLooseConfigEnterpriseApp() -> Nubmber of EJB modules: "
                + ejbModules.size());
        addEarJarModules(ejbModules, looseEar);
        
        // Web Application modules
        List<Dependency> warModules = getDependentModules(proj, "war");
        log.debug("InstallAppMojoSupport: installLooseConfigEnterpriseApp() -> Nubmber of War modules: "
                + warModules.size());
        addEarWarModules(warModules, looseEar);
        
        // TODO: Application Client module
        
        // TODO: Resource Adapter module
        
        // add Manifest file
        looseEar.addManifestFile(proj, "maven-ear-plugin");
    }
    
    private void addEarJarModules(List<Dependency> jarModules, LooseEarApplication looseEar) throws Exception {
        for (Dependency jarModule : jarModules) {
            MavenProject proj = getMavenProject(jarModule.getGroupId(), jarModule.getArtifactId(),
                    jarModule.getVersion());
            if (proj.getBasedir() != null && proj.getBasedir().exists()) {
                looseEar.addJarModule(proj);
            } else {
                // use the artifact from local m2 repo
                Artifact artifact = resolveArtifact(proj.getArtifact());
                looseEar.addModuleFromM2(artifact, artifact.getFile().getAbsolutePath());
            }
        }
    }
    
    private void addEarWarModules(List<Dependency> warModules, LooseEarApplication looseEar) throws Exception {
        for (Dependency warModule : warModules) {
            MavenProject proj = getMavenProject(warModule.getGroupId(), warModule.getArtifactId(),
                    warModule.getVersion());
            if (proj.getBasedir() != null && proj.getBasedir().exists()) {
                Element warArchive = looseEar.addWarModule(proj, getWarSourceDirectory(proj).getCanonicalPath());
                addWarEmbeddedLib(warArchive, proj, looseEar);
            } else {
                // use the artifact from local .m2 repo
                Artifact artifact = resolveArtifact(proj.getArtifact());
                looseEar.addModuleFromM2(artifact, artifact.getFile().getAbsolutePath());
            }
        }
    }
    
    private void addWarEmbeddedLib(Element parent, MavenProject proj, LooseApplication looseApp) throws Exception {
        List<Dependency> deps = getDependentLibraries(proj);
        for (Dependency dep : deps) {
            MavenProject dependProject = getMavenProject(dep.getGroupId(), dep.getArtifactId(),
                    dep.getVersion());
            if (dependProject.getBasedir() != null && dependProject.getBasedir().exists()) {
                Element archive = looseApp.addArchive(parent, "/WEB-INF/lib/" + dependProject.getBuild().getFinalName() + ".jar");
                looseApp.addOutputDir(archive, dependProject, "/");
                looseApp.addManifestFile(archive, dependProject, "maven-jar-plugin");
            } else {
                looseApp.getConfig().addFile(parent,
                        resolveArtifact(dependProject.getArtifact()).getFile().getAbsolutePath(),
                        "/WEB-INF/lib/" + resolveArtifact(dependProject.getArtifact()).getFile().getName());
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
        return getWarFileName(project) + ".xml";
    }
    
    // get loose application configuration file name for project artifact
    private String getWarFileName(MavenProject project) {
        String name = project.getBuild().getFinalName() + "." + project.getPackaging();
        if (project.getPackaging().equals("liberty-assembly")) {
            name = project.getBuild().getFinalName() + ".war";
        }
        if (stripVersion) {
            name = stripVersionFromName(name, project.getVersion());
        } 
        return name;
    }
    
    private List<Dependency> getDependentModules(MavenProject proj, String type) {
        List<Dependency> dependencies = new ArrayList<Dependency>();
        
        @SuppressWarnings("unchecked")
        List<Dependency> deps = proj.getDependencies();
        for (Dependency dep : deps) {
            if ("compile".equals(dep.getScope()) && type.equals(dep.getType())) {
                dependencies.add(dep);
            }
        }
        return dependencies;
    }
    
    private List<Dependency> getDependentLibraries(MavenProject proj) {
        return getDependentModules(proj, "jar");
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
