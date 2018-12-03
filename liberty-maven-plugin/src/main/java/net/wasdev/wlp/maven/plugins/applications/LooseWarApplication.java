package net.wasdev.wlp.maven.plugins.applications;

import java.io.File;


import org.apache.maven.project.MavenProject;

import net.wasdev.wlp.common.plugins.config.LooseConfigData;

public class LooseWarApplication extends LooseApplication {
    
    public LooseWarApplication(MavenProject project, LooseConfigData config) {
        super(project, config);
    }
    
    public void addSourceDir(MavenProject proj) throws Exception {
        File sourceDir = new File(proj.getBasedir().getAbsolutePath(), "src/main/webapp");
        String path = getPluginConfiguration(proj, "org.apache.maven.plugins", "maven-war-plugin", "warSourceDirectory");
        if (path != null) {
            sourceDir = new File(proj.getBasedir().getAbsolutePath(), path);
        } 
        config.addDir(sourceDir.getCanonicalPath(), "/");
    }
}
