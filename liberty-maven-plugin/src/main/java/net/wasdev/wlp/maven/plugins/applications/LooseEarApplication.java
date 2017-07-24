package net.wasdev.wlp.maven.plugins.applications;

import java.io.IOException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.w3c.dom.Element;

public class LooseEarApplication {
    private MavenProject project;
    private LooseConfigData config;
    
    public LooseEarApplication(MavenProject project, LooseConfigData config) {
        this.project = project;
        this.config = config;
    }
    
    public Element addJarModule(MavenProject proj) throws IOException {
        String jarArchiveName = "/" + getModuleName(proj);
        if (getEarDefaultLibBundleDir() != null) {
            jarArchiveName = "/" + getEarDefaultLibBundleDir() + jarArchiveName;
        }
        Element jarArchive = config.addArchive(jarArchiveName);
        config.addDir(jarArchive, proj.getBasedir().getCanonicalPath() + "/target/classes", "/");
        config.addDir(jarArchive, proj.getBasedir().getCanonicalPath() + "/src/main/resources", "/");
        return jarArchive;
    }
    
    public Element addEjbModule(MavenProject proj) throws IOException {
        String ejbArchiveName = "/" + getModuleName(proj);
        Element ejbArchive = config.addArchive(ejbArchiveName);
        config.addDir(ejbArchive, proj.getBasedir().getCanonicalPath() + "/target/classes", "/");
        config.addDir(ejbArchive, proj.getBasedir().getCanonicalPath() + "/src/main/resources", "/");
        return ejbArchive;
    }
    
    public Element addWarModule(MavenProject proj, String warSourceDir) throws IOException {
        String warArchiveName = "/" + getModuleName(proj);
        Element warArchive = config.addArchive(warArchiveName);
        config.addDir(warSourceDir, "/");
        config.addDir(warArchive, proj.getBasedir().getCanonicalPath() + "/target/classes", "/WEB-INF/classes");
        config.addDir(warArchive, proj.getBasedir().getCanonicalPath() + "/src/main/resources", "/WEB-INF/classes");
        
        return warArchive;
    }
    
    public void addModuleFromM2(Artifact artifact, String artifactFile) {
        String artifactName = "/" + getModuleName(artifact);
        
        if (getEarDefaultLibBundleDir() != null && (artifact.getType() == null || "jar".equals(artifact.getType()))) {
            artifactName = "/" + getEarDefaultLibBundleDir() + artifactName;
        }
        
        config.addFile(artifactFile, artifactName);
    }
    
    public String getModuleName(Artifact artifact) {
        String moduleName;
        
        String fileExtension = artifact.getType();
        if ("ejb".equals(fileExtension)) {
            fileExtension = "jar";
        }
        
        switch (getEarFileNameMapping()) {
            case "no-version":
                moduleName = artifact.getArtifactId() + "." + fileExtension;
                break;
            case "no-version-for-ejb":
                if ("ejb".equals(artifact.getType())) {
                    moduleName = artifact.getArtifactId() + "." + fileExtension;
                } else {
                    moduleName = artifact.getArtifactId() + "-" + artifact.getVersion() + "." + fileExtension;
                }
                break;
            case "full":
                moduleName = artifact.getGroupId() + "-" + artifact.getArtifactId() + "-" + artifact.getVersion() + "."
                        + fileExtension;
                break;
            default:
                // standard
                moduleName = artifact.getArtifactId() + "-" + artifact.getVersion() + "." + fileExtension;
                break;
        }
        return moduleName;
    }
    
    public String getModuleName(MavenProject proj) {
        String moduleName;
        
        String fileExtension = proj.getPackaging();
        if ("ejb".equals(fileExtension)) {
            fileExtension = "jar";
        }
        
        switch (getEarFileNameMapping()) {
            case "no-version":
                moduleName = proj.getArtifactId() + "." + fileExtension;
                break;
            case "no-version-for-ejb":
                if ("ejb".equals(proj.getPackaging())) {
                    moduleName = proj.getArtifactId() + "." + fileExtension;
                } else {
                    moduleName = proj.getArtifactId() + "-" + proj.getVersion() + "." + fileExtension;
                }
                break;
            case "full":
                moduleName = proj.getGroupId() + "-" + proj.getArtifactId() + "-" + proj.getVersion() + "."
                        + fileExtension;
                break;
            default:
                // standard
                moduleName = proj.getArtifactId() + "-" + proj.getVersion() + "." + fileExtension;
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
    
    public String getPluginConfiguration(String pluginGroupId, String pluginArtifactId, String key) {
        Xpp3Dom dom = project.getGoalConfiguration(pluginGroupId, pluginArtifactId, null, null);
        if (dom != null) {
            Xpp3Dom val = dom.getChild(key);
            if (val != null) {
                return val.getValue();
            }
        }
        return null;
    }
}
