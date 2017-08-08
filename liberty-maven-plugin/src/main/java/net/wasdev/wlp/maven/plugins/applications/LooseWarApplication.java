package net.wasdev.wlp.maven.plugins.applications;

import java.io.File;


import org.apache.maven.project.MavenProject;

public class LooseWarApplication extends LooseApplication {
    
    public LooseWarApplication(MavenProject project, LooseConfigData config) {
        super(project, config);
    }
    
    public void addSourceDir(MavenProject proj) throws Exception {
        File sourceDir = new File(proj.getBasedir().getAbsolutePath(), "src/main/webapp");
        String path = getPluginConfiguration("org.apache.maven.plugins", "maven-war-plugin", "warSourceDirectory");
        if (path != null) {
            sourceDir = new File(proj.getBasedir().getAbsolutePath(), path);
        } 
        config.addDir(sourceDir.getCanonicalPath(), "/");
    }
}
