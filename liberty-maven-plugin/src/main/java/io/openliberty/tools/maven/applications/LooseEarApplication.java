/**
 * (C) Copyright IBM Corporation 2017, 2023.
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
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.shared.mapping.MappingUtils;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.w3c.dom.Element;

import io.openliberty.tools.maven.utils.MavenProjectUtil;
import io.openliberty.tools.common.plugins.config.LooseApplication;
import io.openliberty.tools.common.plugins.config.LooseConfigData;

public class LooseEarApplication extends LooseApplication {

    protected final MavenProject project;

    public LooseEarApplication(MavenProject project, LooseConfigData config) {
        super(project.getBuild().getDirectory(), config);
        this.project = project;
    }

    public void addSourceDir() throws IOException {
        File sourceDir = new File(project.getBasedir(), "src/main/application");
        String path = MavenProjectUtil.getPluginConfiguration(project, "org.apache.maven.plugins", "maven-ear-plugin",
                "earSourceDirectory");
        if (path != null) {
            sourceDir = new File(path);
        }
        config.addDir(sourceDir, "/");
    }

    public void addApplicationXmlFile() throws IOException {
        File applicationXmlFile = null;
        String path = MavenProjectUtil.getPluginConfiguration(project, "org.apache.maven.plugins", "maven-ear-plugin",
                "applicationXml");
        if (path != null && !path.isEmpty()) {
            applicationXmlFile = new File(path);
            config.addFile(applicationXmlFile, "/META-INF/application.xml");
        } else if (MavenProjectUtil.getPluginConfiguration(project, "org.apache.maven.plugins", "maven-ear-plugin",
                "generateApplicationXml") == null
                || MavenProjectUtil.getPluginConfiguration(project, "org.apache.maven.plugins", "maven-ear-plugin",
                        "generateApplicationXml").equals("true")) {
            applicationXmlFile = new File(buildDirectory + "/application.xml");
            config.addFile(applicationXmlFile, "/META-INF/application.xml");
        }
    }

    public Element addJarModule(MavenProject proj, Artifact artifact) throws MojoExecutionException, IOException {
        return addModule(proj, artifact, "maven-jar-plugin");
    }

    public Element addEjbModule(MavenProject proj, Artifact artifact) throws MojoExecutionException, IOException {
        return addModule(proj, artifact, "maven-ejb-plugin");
    }

    public Element addModule(MavenProject proj, Artifact artifact, String pluginId) throws MojoExecutionException, IOException {
        File outputDirectory = new File(proj.getBuild().getOutputDirectory());
        Element moduleArchive = config.addArchive(getModuleUri(artifact));
        config.addDir(moduleArchive, outputDirectory, "/");
        // add manifest.mf
        File manifestFile = MavenProjectUtil.getManifestFile(proj, pluginId);

        String mavenProjectTargetDir = proj.getBuild().getDirectory();

        try {
            addManifestFileWithParent(moduleArchive, manifestFile, mavenProjectTargetDir);
            // add meta-inf files if any
            addMetaInfFiles(moduleArchive, outputDirectory);
            return moduleArchive;    
        } catch (Exception e) {
            throw new MojoExecutionException("Error adding manifest or META-INF files for loose ear configuration: "+artifact, e);
        }
    }

    public Element addWarModule(MavenProject proj, Artifact artifact, File warSourceDir) throws MojoExecutionException, IOException {
        Element warArchive = config.addArchive(getModuleUri(artifact));
        config.addDir(warArchive, warSourceDir, "/");
        config.addDir(warArchive, new File(proj.getBuild().getOutputDirectory()), "/WEB-INF/classes");
        
        
        // retrieve the directories defined as resources in the maven war plugin
        List<Xpp3Dom> webResources = LooseWarApplication.getWebResourcesConfigurations(proj);
    	for (Xpp3Dom resource : webResources) {
            Xpp3Dom dir = resource.getChild("directory");
            Xpp3Dom target = resource.getChild("targetPath");
            String targetPath = target==null ? "/" : "/"+target.getValue();
            config.addDir(warArchive, new File(proj.getBasedir().getAbsolutePath(), dir.getValue()), targetPath);
        }

        // add Manifest file
        try {
            addWarManifestFile(warArchive, artifact, proj);
        } catch (Exception e) {
            throw new MojoExecutionException("Error adding manifest file for war module "+artifact, e);
        }
        return warArchive;
    }

    public Element addRarModule(MavenProject proj, Artifact artifact) throws MojoExecutionException, IOException {
        Element rarArchive = config.addArchive(getModuleUri(artifact));
        config.addDir(rarArchive, getRarSourceDirectory(proj), "/");

        // get raXmlFile optional rar plugin parameter
        String path = MavenProjectUtil.getPluginConfiguration(proj, "org.apache.maven.plugins", "maven-rar-plugin",
                "raXmlFile");
        if (path != null && !path.isEmpty()) {
            File raXmlFile = new File(path);
            config.addFile(rarArchive, raXmlFile, "/META-INF/ra.xml");
        }

        String mavenProjectTargetDir = proj.getBuild().getDirectory();

        // add Manifest file
        File manifestFile = MavenProjectUtil.getManifestFile(proj, "maven-rar-plugin");
        try {
            addManifestFileWithParent(rarArchive, manifestFile, mavenProjectTargetDir);
        } catch (Exception e) {
            throw new MojoExecutionException("Error adding manifest file for rar module "+artifact, e);
        }
        return rarArchive;
    }

    public File getRarSourceDirectory(MavenProject proj) {
        String dir = MavenProjectUtil.getPluginConfiguration(proj, "org.apache.maven.plugins", "maven-rar-plugin",
                "rarSourceDirectory");
        if (dir != null) {
            return new File(dir);
        } else {
            return new File(proj.getBasedir(), "src/main/rar");
        }
    }

    public String getModuleUri(Artifact artifact) throws MojoExecutionException {
        String defaultUri = "/" + getModuleName(artifact);
        // both "jar" and "bundle" packaging type project are "jar" type dependencies
        // that will be packaged in the ear lib directory
        String type = artifact.getType();
        if (("jar".equals(type) || "bundle".equals(type)) && getEarDefaultLibBundleDir() != null) {
            defaultUri = "/" + getEarDefaultLibBundleDir() + defaultUri;
        }
        Xpp3Dom dom = project.getGoalConfiguration("org.apache.maven.plugins", "maven-ear-plugin", null, null);
        if (dom != null) {
            Xpp3Dom val = dom.getChild("modules");
            if (val != null) {
                Xpp3Dom[] modules = val.getChildren();
                if (modules != null) {
                    for (int i = 0; i < modules.length; i++) {
                        if (artifact.getGroupId().equals(getConfigValue(modules[i].getChild("groupId")))
                                && artifact.getArtifactId().equals(getConfigValue(modules[i].getChild("artifactId")))) {
                            String uri = getConfigValue(modules[i].getChild("uri"));
                            if (uri != null) {
                                return uri;
                            } else {
                                String bundleDir = getConfigValue(modules[i].getChild("bundleDir"));
                                String bundleFileName = getConfigValue(modules[i].getChild("bundleFileName"));
                                if (bundleDir == null) {
                                    if ("jar".equals(type) && getEarDefaultLibBundleDir() != null) {
                                        bundleDir = "/" + getEarDefaultLibBundleDir();
                                    } else {
                                        bundleDir = "";
                                    }
                                } else {
                                    bundleDir = "/" + bundleDir;
                                }

                                // remove duplicate forward slashes. At this point, we know bundleDir starts
                                // with a slash or is empty
                                if (bundleDir.length() > 1 && bundleDir.charAt(0) == bundleDir.charAt(1)) {
                                    StringBuilder sb = new StringBuilder(bundleDir);
                                    do {
                                        sb.deleteCharAt(0);
                                    } while (sb.length() > 1 && sb.charAt(0) == sb.charAt(1));
                                    bundleDir = sb.toString();
                                    if ("/".equals(bundleDir)) {
                                        bundleDir = "";
                                    }
                                }
                                if (bundleFileName != null) {
                                    return bundleDir + "/" + bundleFileName;
                                } else {
                                    return bundleDir + "/" + getModuleName(artifact);
                                }
                            }
                        }
                    }
                }
            }
        }
        return defaultUri;
    }

    public String getConfigValue(Xpp3Dom element) {
        if (element != null) {
            return element.getValue();
        }
        return null;
    }

    public void addModuleFromM2(Artifact artifact) throws MojoExecutionException, IOException {
        String artifactName = getModuleUri(artifact);
        config.addFile(artifact.getFile(), artifactName);
    }

    public String getModuleName(Artifact artifact) throws MojoExecutionException {
        int earPluginVersion = MavenProjectUtil.getMajorPluginVersion(project, "org.apache.maven.plugins:maven-ear-plugin");
        if (earPluginVersion < 3) {
            return getEarFileNameMappingHelper(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getType());
        }
        return getEarOutputFileNameMapping(artifact);
    }

    public String getEarFileNameMappingHelper(String groupId, String artifactId, String version, String packaging) {
        String moduleName;

        String fileExtension = packaging;
        if ("ejb".equals(fileExtension) || "app-client".equals(fileExtension) || "bundle".equals(fileExtension)) {
            fileExtension = "jar";
        }

        switch (getEarFileNameMapping()) {
        case "no-version":
            moduleName = artifactId + "." + fileExtension;
            break;
        case "no-version-for-ejb":
            if ("ejb".equals(packaging)) {
                moduleName = artifactId + "." + fileExtension;
            } else {
                moduleName = artifactId + "-" + version + "." + fileExtension;
            }
            break;
        case "full":
            moduleName = groupId + "-" + artifactId + "-" + version + "." + fileExtension;
            break;
        default:
            // standard
            moduleName = artifactId + "-" + version + "." + fileExtension;
            break;
        }
        return moduleName;
    }
    
    // Valid for maven-ear-plugin version 3 and greater
    public String getEarOutputFileNameMapping(Artifact artifact) throws MojoExecutionException {
        String outputFileNameMapping = MavenProjectUtil.getPluginConfiguration(project, "org.apache.maven.plugins",
            "maven-ear-plugin", "outputFileNameMapping");
            try {
                String fileNameMapping = MappingUtils.evaluateFileNameMapping( outputFileNameMapping, artifact );
                if (fileNameMapping == null || fileNameMapping.isEmpty()) {
                    // default format if none is specified
                    String defaultFormat = "@{groupId}@-@{artifactId}@-@{version}@@{dashClassifier?}@.@{extension}@";
                    fileNameMapping = MappingUtils.evaluateFileNameMapping( defaultFormat, artifact );
                }
                return fileNameMapping;
        
            } catch (InterpolationException e) {
                throw new MojoExecutionException("Error getting outputFileNameMapping for ear artifact "+artifact, e);
            }
    }

    // Deprecated for maven-ear-plugin version 3 and greater
    public String getEarFileNameMapping() {
        // valid values are: standard, no-version, no-version-for-ejb, full
        String fileNameMapping = MavenProjectUtil.getPluginConfiguration(project, "org.apache.maven.plugins",
                "maven-ear-plugin", "fileNameMapping");
        if (fileNameMapping == null || fileNameMapping.isEmpty()) {
            fileNameMapping = "standard";
        }
        return fileNameMapping;
    }

    public String getEarDefaultLibBundleDir() {
        return MavenProjectUtil.getPluginConfiguration(project, "org.apache.maven.plugins", "maven-ear-plugin",
                "defaultLibBundleDir");
    }

    public Boolean isEarSkinnyWars() {
        String skinnyWars = MavenProjectUtil.getPluginConfiguration(project, "org.apache.maven.plugins",
                "maven-ear-plugin", "skinnyWars");
        if (skinnyWars != null && "true".equals(skinnyWars)) {
            return true;
        } else {
            return false;
        }
    }

    public void addWarManifestFile(Element parent, Artifact artifact, MavenProject proj) throws Exception {
        // the ear plug-in modify the skinnyWar module manifest file in
        // ${project.build.directory}/temp
        File newMf = new File(project.getBuild().getDirectory() + "/temp/" + getModuleUri(artifact) + "/META-INF");
        if (newMf.exists()) { //use new META-INF dir if it exists
            if (isEarSkinnyWars()) {
                config.addDir(parent, newMf, "/META-INF");
            } else {
                File manifestFile = MavenProjectUtil.getManifestFile(proj, "maven-war-plugin");
                addManifestFileWithParent(parent, manifestFile, newMf.getCanonicalPath());
            }
        } else { //if temp  META-INF folder doesn't exist, use reactor project target dir
            String mavenProjectTargetDir = proj.getBuild().getDirectory();
            File manifestFile = MavenProjectUtil.getManifestFile(proj, "maven-war-plugin");
            addManifestFileWithParent(parent, manifestFile, mavenProjectTargetDir);
        }
    }

    public boolean isEarDependency(Artifact artifact) {
        // get all ear project compile dependencies
        Set<Artifact> deps = project.getArtifacts();
        for (Artifact dep : deps) {
            if (("compile".equals(artifact.getScope()) || "runtime".equals(artifact.getScope()))
                    && "jar".equals(dep.getType()) && artifact.getGroupId().equals(dep.getGroupId())
                    && artifact.getArtifactId().equals(dep.getArtifactId())
                    && artifact.getVersion().equals(dep.getVersion())) {
                return true;
            }
        }
        return false;
    }
    
}
