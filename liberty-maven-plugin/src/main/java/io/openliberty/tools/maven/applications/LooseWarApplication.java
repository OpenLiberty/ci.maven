package io.openliberty.tools.maven.applications;

import java.nio.file.Path;

import org.apache.maven.project.MavenProject;

import io.openliberty.tools.common.plugins.config.LooseApplication;
import io.openliberty.tools.common.plugins.config.LooseConfigData;
import io.openliberty.tools.maven.utils.MavenProjectUtil;

public class LooseWarApplication extends LooseApplication {
    
    protected final MavenProject project;
    
    public LooseWarApplication(MavenProject project, LooseConfigData config) {
        super(project.getBuild().getDirectory(), config);
        this.project = project;
    }
    
    public void addSourceDir(MavenProject proj) throws Exception {
        Path warSourceDir = MavenProjectUtil.getWarSourceDirectory(proj);
        config.addDir(warSourceDir.toFile(), "/");
    }
}
