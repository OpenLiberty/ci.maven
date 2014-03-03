package net.wasdev.wlp.maven.plugins.applications;

import java.io.File;
import java.text.MessageFormat;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.mojo.pluginsupport.util.ArtifactItem;

import net.wasdev.wlp.ant.DeployTask;
import net.wasdev.wlp.maven.plugins.BasicSupport;

/**
 * Deploy application to liberty server
 * 
 * @goal deploy
 * 
 * 
 */
public class DeployAppMojo extends BasicSupport {
    /**
     * A file which points to a specific module's war | ear | eba | zip archive location
     * 
     * @parameter expression="${appArchive}"
     */
    protected File appArchive = null;

    /**
     * Maven coordinates of an application to deploy. This is best listed as a dependency,
     * in which case the version can be omitted.
     * 
     * @parameter
     */
    protected ArtifactItem appArtifact;

    /**
     * Timeout to verify deploy successfully
     * 
     * @parameter expression="${timeout}" default-value="40000"
     */
    protected long timeout = 40000;

    @Override
    protected void doExecute() throws Exception {
        if (!serverHome.exists()) {
            throw new MojoExecutionException(MessageFormat.format(messages.getString("error.server.home.noexist"), serverHome));
        }

        // check server directory
        if (!serverDirectory.exists()) {
            throw new MojoExecutionException(MessageFormat.format(messages.getString("error.server.noexist"), serverName));
        }

        if (appArchive != null && appArtifact != null) {
            throw new MojoExecutionException(messages.getString("error.app.set.twice"));
        }

        if (appArtifact != null) {
            Artifact artifact = getArtifact(appArtifact);
            appArchive = artifact.getFile();
            log.info(MessageFormat.format(messages.getString("info.variable.set"), "artifact based application", appArtifact));
        } else {
            log.info(MessageFormat.format(messages.getString("info.variable.set"), "non-artifact based application", appArchive));
        }

        if (appArchive == null || !appArchive.exists() || appArchive.isDirectory()) {
            throw new MojoExecutionException(MessageFormat.format(messages.getString("error.deploy.app.set.validate"), ""));
        }

        if (appArchive.getName().endsWith("war") || appArchive.getName().endsWith("ear")
            || appArchive.getName().endsWith("rar") || appArchive.getName().endsWith("eba")
            || appArchive.getName().endsWith("jar") || appArchive.getName().endsWith("zip")) {

            log.info(MessageFormat.format(messages.getString("info.deploy.app"), appArchive.getCanonicalPath()));
            DeployTask deployTask = (DeployTask) ant.createTask("antlib:net/wasdev/wlp/ant:deploy");
            if (deployTask == null)
                throw new NullPointerException("server task not found");

            deployTask.setInstallDir(serverHome);
            deployTask.setServerName(serverName);
            deployTask.setUserDir(userDirectory);
            deployTask.setFile(appArchive);
            deployTask.setTimeout(Long.toString(timeout));
            deployTask.execute();
        } else {
            throw new MojoExecutionException(MessageFormat.format(messages.getString("error.deploy"), ""));
        }

    }
}
