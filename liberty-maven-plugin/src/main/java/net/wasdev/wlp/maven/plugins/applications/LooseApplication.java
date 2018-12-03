package net.wasdev.wlp.maven.plugins.applications;

import java.io.File;
import java.io.FileOutputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.w3c.dom.Element;

import net.wasdev.wlp.common.plugins.config.LooseConfigData;

public class LooseApplication {
    private File defaultMF = null;
    
    protected MavenProject project;
    protected LooseConfigData config;
    
    public LooseApplication(MavenProject project, LooseConfigData config) {
        this.project = project;
        this.config = config;
    }
    
    public LooseConfigData getConfig() {
        return config;
    }
    
    public Element getDocumentRoot() {
        return config.getDocumentRoot();
    }
    
    public Element addArchive(Element parent, String target) {
        return config.addArchive(parent, target);
    }
    
    public void addOutputDir(Element parent, MavenProject proj, String target) {
        config.addDir(parent, proj.getBuild().getOutputDirectory(), target);
    }
    
    public void addManifestFile(Element parent, MavenProject proj, String pluginId) throws Exception {
        config.addFile(parent, getManifestFile(proj, "org.apache.maven.plugins", pluginId), "/META-INF/MANIFEST.MF");    
    }

    public void addMetaInfFiles(Element parent, MavenProject proj, String pluginId) throws Exception {
        File metaInfFolder = new File(proj.getBuild().getOutputDirectory() + "/" + "META-INF");
        boolean existsAndHasAdditionalFiles = metaInfFolder.exists() && metaInfFolder.list().length > 0;
        if (existsAndHasAdditionalFiles) {
            // then we should add each file from this folder
            addFiles(parent, metaInfFolder, "/META-INF");
        }
    }

    private void addFiles(Element parent, File file, String targetPrefix) {
        for (File subFile : file.listFiles()) {
            if (subFile.isDirectory()) {
                addFiles(parent, subFile, targetPrefix + "/" + subFile.getName());
            } else {
                config.addFile(parent, subFile.getAbsolutePath(), targetPrefix + "/" + subFile.getName());
            }
        }
    }

    public void addManifestFile(MavenProject proj, String pluginId) throws Exception {
        config.addFile(getManifestFile(proj, "org.apache.maven.plugins", pluginId), "/META-INF/MANIFEST.MF"); 
    }
    
    public String getPluginConfiguration(MavenProject proj, String pluginGroupId, String pluginArtifactId, String key) {
        Xpp3Dom dom = proj.getGoalConfiguration(pluginGroupId, pluginArtifactId, null, null);
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
