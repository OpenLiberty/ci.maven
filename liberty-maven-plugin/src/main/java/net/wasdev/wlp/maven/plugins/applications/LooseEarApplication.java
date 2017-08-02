package net.wasdev.wlp.maven.plugins.applications;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
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
    
    public LooseConfigData getConfig() {
        return config;
    }
    
    public Element addJarModule(MavenProject proj) throws Exception {
        String jarArchiveName = "/"
                + getModuleName(proj.getGroupId(), proj.getArtifactId(), proj.getVersion(), proj.getPackaging());
        if (getEarDefaultLibBundleDir() != null && !"ejb".equals(proj.getPackaging())) {
            jarArchiveName = "/" + getEarDefaultLibBundleDir() + jarArchiveName;
        }
        Element jarArchive = config.addArchive(jarArchiveName);
        config.addDir(jarArchive, proj.getBuild().getOutputDirectory(), "/");
        @SuppressWarnings("unchecked")
        List<Resource> resources = proj.getResources();
        for (Resource res : resources) {
            config.addDir(jarArchive, res.getDirectory(), "/");
        }
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
        @SuppressWarnings("unchecked")
        List<Resource> resources = proj.getResources();
        for (Resource res : resources) {
            config.addDir(warArchive, res.getDirectory(), "/WEB-INF/classes");
        }
        // add Manifest file
        addManifestFile(warArchive, proj, "maven-war-plugin");
        return warArchive;
    }
    
    public void addManifestFile(Element e, MavenProject proj, String pluginId) throws Exception {
        config.addFile(e, getManifestFile(proj, "org.apache.maven.plugins", pluginId), "/META-INF/MANIFEST.MF");    
    }
    
    public void addManifestFile(MavenProject proj, String pluginId) throws Exception {
        config.addFile(getManifestFile(proj, "org.apache.maven.plugins", pluginId), "/META-INF/MANIFEST.MF");    
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
    
    public String getManifestFile(MavenProject proj, String pluginGroupId, String pluginArtifactId) throws Exception {
        Xpp3Dom dom = proj.getGoalConfiguration(pluginGroupId, pluginArtifactId, null, null);
        if (dom != null) {
            Xpp3Dom archive = dom.getChild("archive");
            if (archive != null) {
                Xpp3Dom val = archive.getChild("manifestFile");
                if (val != null) {
                    return proj.getBasedir().getAbsolutePath() + "/" + val.getValue();
                }
            }
        }
        return getDefaultManifest().getCanonicalPath();
    }
    
    private File defaultMF = null;
    
    public File getDefaultManifest() throws Exception {
        if (defaultMF == null) {
            defaultMF = new File(
                    project.getBuild().getDirectory() + "/liberty-maven/resources/META-INF/MANIFEST.MF");
            defaultMF.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(defaultMF);
            
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            manifest.write(fos);
            fos.close();
        }
        return defaultMF;
    }
}
