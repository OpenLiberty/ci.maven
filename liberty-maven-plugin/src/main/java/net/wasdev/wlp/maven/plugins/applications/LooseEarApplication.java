package net.wasdev.wlp.maven.plugins.applications;

import java.io.File;
import java.io.IOException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.w3c.dom.Element;

public class LooseEarApplication {
    private MavenProject project;
    private LooseConfigData config;
    private Log log;

    public LooseEarApplication(MavenProject project, LooseConfigData config, Log log) {
        this.project = project;
        this.config = config;
        this.log = log;
    }
    
    public void addJarModule(Artifact jarModule) throws IOException {
        String jarArchiveName = "/" + getModuleName(jarModule);

        if (getEarDefaultLibBundleDir() != null) {
            jarArchiveName = "/" + getEarDefaultLibBundleDir() + jarArchiveName;
        }
        
        File jarModuleProjectDir = new File(project.getBasedir(), "../" + jarModule.getArtifactId());
        if (jarModuleProjectDir.exists()){
            Element ejbArchive = config.addArchive(jarArchiveName);
            config.addDir(ejbArchive, jarModuleProjectDir.getCanonicalPath() + "/target/classes", "/");
            config.addDir(ejbArchive, jarModuleProjectDir.getCanonicalPath() + "src/main/resources", "/");
        } else {
            log.debug("InstallAppMojoSupport: installLooseConfigEnterpriseApp() -> No project source for Jar module, " + jarModule.getArtifactId());
            config.addFile(jarModule.getFile().getCanonicalPath(), jarArchiveName);
        }
    }
    
    public void addEjbModule(Artifact ejbModule) throws IOException {
        String ejbArchiveName = "/" + getModuleName(ejbModule);
        
        File ejbModuleProjectDir = new File(project.getBasedir(), "../" + ejbModule.getArtifactId());
        if (ejbModuleProjectDir.exists()){
            Element ejbArchive = config.addArchive(ejbArchiveName);
            config.addDir(ejbArchive, ejbModuleProjectDir.getCanonicalPath() + "/target/classes", "/");
            config.addDir(ejbArchive, ejbModuleProjectDir.getCanonicalPath() + "src/main/resources", "/");
        } else {
            log.debug("InstallAppMojoSupport: installLooseConfigEnterpriseApp() -> No project source for EJB module, " + ejbModule.getArtifactId());
            config.addFile(ejbModule.getFile().getCanonicalPath(), ejbArchiveName);
        }
    }
    
    public void addWebModule(Artifact webModule) throws IOException {
        String warArchiveName = "/" + getModuleName(webModule);
        /*
        String warArchiveName = "/" + webModule.getArtifactId() + "-" + webModule.getVersion() + ".war";
        if (getEarFileNameMapping().equals("no-version")) {
            warArchiveName = "/" + webModule.getArtifactId() + ".war";
        } else if (getEarFileNameMapping().equals("full")){
            warArchiveName = "/" + webModule.getGroupId() + "-" +  webModule.getArtifactId() + "-" + webModule.getVersion() + ".war";
        }
        */
        File webModuleProjectDir = new File(project.getBasedir(), "../" + webModule.getArtifactId());
        if (webModuleProjectDir.exists()){
            Element warArchive = config.addArchive(warArchiveName);
            config.addDir(warArchive, webModuleProjectDir.getCanonicalPath() + "/src/main/webapp", "/");
            config.addDir(warArchive, webModuleProjectDir.getCanonicalPath() + "/target/classes", "/WEB-INF/classes");
            config.addDir(warArchive, webModuleProjectDir.getCanonicalPath() + "src/main/resources", "/WEB-INF/classes");
            //TDOD: emmbedded util jar in /WEB-INF/lib 
        } else {
            log.debug("InstallAppMojoSupport: installLooseConfigEnterpriseApp() -> No project source for WAR module, " + webModule.getArtifactId());
            config.addFile(webModule.getFile().getCanonicalPath(), warArchiveName);
        }
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
                moduleName = artifact.getGroupId() + "-" +  artifact.getArtifactId() + "-" + artifact.getVersion() + "." +fileExtension;
                break;
            default:
                // standard
                moduleName = artifact.getArtifactId() + "-" + artifact.getVersion() + "." + fileExtension;
        }
        return moduleName;
    }
    
    public String getEarFileNameMapping() {
        // valid values are: standard, no-version, no-version-for-ejb, full
        String fileNameMapping = getPluginConfiguration("org.apache.maven.plugins", "maven-ear-plugin", "fileNameMapping");
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

