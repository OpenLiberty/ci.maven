package net.wasdev.wlp.maven.plugins.server;

import java.io.File;
import java.text.MessageFormat;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import net.wasdev.wlp.ant.ServerTask;

/**
 * Package a liberty server
 * 
 * @goal package-server
 * 
 * @phase package
 */
public class PackageServerMojo extends StartDebugMojoSupport {

    /**
     * Locate where server is packaged.
     * 
     * @parameter expression="${packageFile}"
     */
    private File packageFile = null;

    /**
     * @parameter
     */
    private boolean attach;

    /**
     * @parameter expression="${project}"
     * 
     */
    private MavenProject mavenProject;

    @Override
    protected void doExecute() throws Exception {
        if (isInstall) {
            installServerAssembly();
        } else {
            log.info(MessageFormat.format(messages.getString("info.install.type.preexisting"), ""));
            checkServerHomeExists();
            checkServerDirectoryExists();
        }

        // First check server is stopped via server lock file
        log.info(MessageFormat.format(messages.getString("info.server.package.check"), ""));
        File lockFile = new File(serverDirectory, "workarea/.sLock");
        if (lockFile.exists()) {
            log.warn(MessageFormat.format(messages.getString("warn.server.stopped"), ""));
        }

        log.info(MessageFormat.format(messages.getString("info.server.package"), serverName));
        ServerTask serverTask = initializeJava();
        copyConfigFiles();
        serverTask.setOperation("package");
        if (packageFile != null) {
            if (packageFile.isDirectory()) {
                packageFile = new File(packageFile, serverName + ".zip");
            }
        } else {
            packageFile = new File(serverDirectory, serverName + ".zip");
        }
        serverTask.setArchive(packageFile);
        log.info(MessageFormat.format(messages.getString("info.server.package.file.location"), packageFile.getCanonicalPath()));
        serverTask.execute();

        if (attach || (mavenProject != null && "liberty-assembly".equals(mavenProject.getPackaging()))) {
            if (mavenProject == null) {
                throw new MojoFailureException(MessageFormat.format(messages.getString("error.server.package.no.project"), ""));
            }
            project.getArtifact().setFile(packageFile);
        }
    }
}
