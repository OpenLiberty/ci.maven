package net.wasdev.wlp.maven.plugins.applications;

import java.io.File;


import org.apache.maven.project.MavenProject;

import net.wasdev.wlp.common.plugins.config.LooseApplication;
import net.wasdev.wlp.common.plugins.config.LooseConfigData;
import net.wasdev.wlp.maven.plugins.utils.MavenProjectUtil;

public class LooseWarApplication extends LooseApplication {
    
    protected final MavenProject project;
    
    public LooseWarApplication(MavenProject project, LooseConfigData config) {
        super(project.getBuild().getDirectory(), config);
        this.project = project;
    }
    
    public void addSourceDir(MavenProject proj) throws Exception {
        File sourceDir = new File(proj.getBasedir().getAbsolutePath(), "src/main/webapp");
        String path = MavenProjectUtil.getPluginConfiguration(proj, "org.apache.maven.plugins", "maven-war-plugin", "warSourceDirectory");
        if (path != null) {
            sourceDir = new File(proj.getBasedir().getAbsolutePath(), path);
        } 
        config.addDir(sourceDir, "/");
    }
}
