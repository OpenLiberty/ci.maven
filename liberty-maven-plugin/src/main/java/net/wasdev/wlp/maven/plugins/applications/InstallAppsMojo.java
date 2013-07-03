package net.wasdev.wlp.maven.plugins.applications;

import java.io.File;
import java.text.MessageFormat;
import java.util.Set;

import net.wasdev.wlp.maven.plugins.BasicSupport;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.tools.ant.taskdefs.Copy;
import org.codehaus.mojo.pluginsupport.util.ArtifactItem;

/**
 * Copy applications to liberty server dropins directory
 * So far this doesn't start the server and actually make sure the apps deploy.
 * 
 * @goal install-apps
 * 
 * @requiresDependencyResolution compile
 */
public class InstallAppsMojo extends BasicSupport {

    protected void doExecute() throws Exception {
        if (!serverHome.exists()) {
            throw new MojoExecutionException(MessageFormat.format(messages.getString("error.server.home.noexist"), serverHome));
        }

        // check server directory
        if (!serverDirectory.exists()) {
            throw new MojoExecutionException(MessageFormat.format(messages.getString("error.server.noexist"), serverName));
        }

        File destDir = new File(serverDirectory, "dropins/");
        for (Artifact dep : (Set<Artifact>) project.getDependencyArtifacts()) {
            // skip assemblyArtifact if specified as a dependency
            if (assemblyArtifact != null && matches(dep, assemblyArtifact)) {
                continue;
            }
            if (dep.getScope().equals("compile")) {
                log.info(MessageFormat.format(messages.getString("info.install.app"), dep.getFile().getCanonicalPath()));

                Copy copyFile = (Copy) ant.createTask("copy");
                copyFile.setFile(dep.getFile());
                if ("war".equals(dep.getType())) {
                    copyFile.setTofile(new File(destDir, dep.getArtifactId() + ".war"));
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
