package net.wasdev.wlp.maven.plugins.applications;

import java.io.File;
import java.text.MessageFormat;
import java.util.Set;

import net.wasdev.wlp.maven.plugins.BasicSupport;

import org.apache.maven.artifact.Artifact;
import org.apache.tools.ant.taskdefs.Copy;
import org.codehaus.mojo.pluginsupport.util.ArtifactItem;

/**
 * Copy applications to the specified directory of the Liberty server.
 * 
 * @goal install-apps
 * 
 * @requiresDependencyResolution compile
 */
public class InstallAppsMojo extends BasicSupport {

    /**
     * Application directory. 
     * 
     * @parameter expression="${appsDirectory}" default-value="dropins"
     */
    protected String appsDirectory = null;
    
    /**
     * Strip version. 
     * 
     * @parameter expression="${stripVersion}" default-value="false"
     */
    protected boolean stripVersion;
    
    protected void doExecute() throws Exception {
        checkServerHomeExists();
        checkServerDirectoryExists();

        File destDir = new File(serverDirectory, appsDirectory);
        for (Artifact dep : (Set<Artifact>) project.getDependencyArtifacts()) {
            // skip assemblyArtifact if specified as a dependency
            if (assemblyArtifact != null && matches(dep, assemblyArtifact)) {
                continue;
            }
            if (dep.getScope().equals("compile")) {
                log.info(MessageFormat.format(messages.getString("info.install.app"), dep.getFile().getCanonicalPath()));

                Copy copyFile = (Copy) ant.createTask("copy");
                copyFile.setFile(dep.getFile());
                if (stripVersion) {
                    copyFile.setTofile(new File(destDir, dep.getArtifactId() + "." + dep.getType()));
                } else {
                    copyFile.setTodir(destDir);
                }
                copyFile.execute();
            }
        }

    }

    private boolean matches(Artifact dep, ArtifactItem assemblyArtifact) {
        return dep.getGroupId().equals(assemblyArtifact.getGroupId())
                && dep.getArtifactId().equals(assemblyArtifact.getArtifactId())
                && dep.getType().equals(assemblyArtifact.getType());
    }
}
