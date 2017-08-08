package net.wasdev.wlp.maven.plugins.applications;

import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Element;

public class LooseEarApplication extends LooseApplication {
    
    public LooseEarApplication(MavenProject project, LooseConfigData config) {
        super(project, config);
    }
    
    public void addSourceDir() throws Exception {
        File sourceDir = new File(project.getBasedir(), "src/main/application");
        String path = getPluginConfiguration("org.apache.maven.plugins", "maven-ear-plugin", "esarSourceDirectory");
        if (path != null) {
            sourceDir = new File(project.getBasedir(), path);
        } 
        config.addDir(sourceDir.getCanonicalPath(), "/");
    }
    
    public void addApplicationXmlFile() throws Exception {
        File applicationXmlFile = null;
        String path = getPluginConfiguration("org.apache.maven.plugins", "maven-ear-plugin", "applicationXml");
        if (path != null && !path.isEmpty()) {
            applicationXmlFile = new File(project.getBasedir(), path);
            config.addFile(applicationXmlFile.getCanonicalPath(), "/META-INF/application.xml");
        } else if (getPluginConfiguration("org.apache.maven.plugins", "maven-ear-plugin", "generateApplicationXml") == null || 
                getPluginConfiguration("org.apache.maven.plugins", "maven-ear-plugin", "generateApplicationXml").equals("true")) {
            applicationXmlFile = new File(project.getBuild().getDirectory() + "/application.xml");
            config.addFile(applicationXmlFile.getCanonicalPath(), "/META-INF/application.xml");
        }
    }
    
    public Element addJarModule(MavenProject proj) throws Exception {
        String jarArchiveName = "/"
                + getModuleName(proj.getGroupId(), proj.getArtifactId(), proj.getVersion(), proj.getPackaging());
        if (getEarDefaultLibBundleDir() != null && !"ejb".equals(proj.getPackaging())) {
            jarArchiveName = "/" + getEarDefaultLibBundleDir() + jarArchiveName;
        }
        Element jarArchive = config.addArchive(jarArchiveName);
        config.addDir(jarArchive, proj.getBuild().getOutputDirectory(), "/");
        // add manifest.mf
        if ("ejb".equals(proj.getPackaging())) {
            addManifestFile(jarArchive, proj, "maven-ejb-plugin");
        } else {
            addManifestFile(jarArchive, proj, "maven-jar-plugin");
        }
        return jarArchive;
    }
    
    public Element addWarModule(MavenProject proj, String warSourceDir) throws Exception {
        String warArchiveName = "/"
                + getModuleName(proj.getGroupId(), proj.getArtifactId(), proj.getVersion(), proj.getPackaging());
        Element warArchive = config.addArchive(warArchiveName);
        config.addDir(warArchive, warSourceDir, "/");
        config.addDir(warArchive, proj.getBuild().getOutputDirectory(), "/WEB-INF/classes");
        // add Manifest file
        addManifestFile(warArchive, proj, "maven-war-plugin");
        return warArchive;
    }
    
    public void addModuleFromM2(Artifact artifact, String artifactFile) {
        String artifactName = "/" + getModuleName(artifact.getGroupId(), artifact.getArtifactId(),
                artifact.getVersion(), artifact.getType());
        
        if (getEarDefaultLibBundleDir() != null && (artifact.getType() == null || "jar".equals(artifact.getType()))) {
            artifactName = "/" + getEarDefaultLibBundleDir() + artifactName;
        }
        
        config.addFile(artifactFile, artifactName);
    }
    
    public String getModuleName(String groupId, String artifactId, String version, String packaging) {
        String moduleName;
        
        String fileExtension = packaging;
        if ("ejb".equals(fileExtension)) {
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
                moduleName = groupId + "-" + artifactId + "-" + version + "."
                        + fileExtension;
                break;
            default:
                // standard
                moduleName = artifactId + "-" + version + "." + fileExtension;
                break;
        }
        return moduleName;
    }
    
    public String getEarFileNameMapping() {
        // valid values are: standard, no-version, no-version-for-ejb, full
        String fileNameMapping = getPluginConfiguration("org.apache.maven.plugins", "maven-ear-plugin",
                "fileNameMapping");
        if (fileNameMapping == null || fileNameMapping.isEmpty()) {
            fileNameMapping = "standard";
        }
        return fileNameMapping;
    }
    
    public String getEarDefaultLibBundleDir() {
        return getPluginConfiguration("org.apache.maven.plugins", "maven-ear-plugin", "defaultLibBundleDir");
    }
}
