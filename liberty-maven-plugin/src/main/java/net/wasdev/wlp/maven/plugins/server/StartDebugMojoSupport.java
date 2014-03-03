package net.wasdev.wlp.maven.plugins.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.tools.ant.taskdefs.Copy;

import net.wasdev.wlp.ant.ServerTask;
import net.wasdev.wlp.maven.plugins.BasicSupport;

/**
 * Start/Debug server support
 * 
 * 
 * 
 */
public class StartDebugMojoSupport extends BasicSupport {

    /**
     * Location of customized configuration file server.xml
     * 
     * @parameter expression="${configFile}"
     *            default-value="${basedir}/src/test/resources/server.xml"
     */
    protected File configFile;

    /**
     * 
     * Location of customized boot property file bootstrap.properties
     * 
     * @parameter expression="${bootProps}"
     *            default-value="${basedir}/src/test/resources/bootstrap.properties"
     */
    protected File bootProps;

    /**
     * 
     * Location of jvm options file jvm.options
     * 
     * @parameter expression="${jvmOptions}"
     *            default-value="${basedir}/src/test/resources/jvm.options"
     */
    protected File jvmOptions;

    /**
     * 
     * Location of customized server environment file server.env
     * 
     * @parameter expression="${serverEnv}"
     *            default-value="${basedir}/src/test/resources/server.env"
     */
    protected File serverEnv;

    /**
     * Overwrite existing configuration files even if they are newer.
     * 
     * @parameter expression="${overwrite}"
     *            default-value="true"
     */
    protected boolean overwrite;

    protected ServerTask initializeJava() throws Exception {
        ServerTask serverTask = (ServerTask) ant.createTask("antlib:net/wasdev/wlp/ant:server");
        if (serverTask == null) {
            throw new NullPointerException("server task not found");
        }
        serverTask.setInstallDir(serverHome);
        serverTask.setServerName(serverName);
        serverTask.setUserDir(userDirectory);
        return serverTask;
    }

    protected void copyConfigFiles() throws IOException {
        copyConfigFiles(overwrite);
    }

    /**
     * @param serverTask
     * @throws IOException
     * @throws FileNotFoundException
     */
    protected void copyConfigFiles(boolean overwrite) throws IOException {
        // copy jvm options file to server directory.
        if (jvmOptions != null && jvmOptions.exists()) {
            Copy copy = (Copy) ant.createTask("copy");
            copy.setFile(jvmOptions);
            copy.setTofile(new File(serverDirectory, "jvm.options"));
            copy.setOverwrite(overwrite);
            copy.execute();

            log.info(MessageFormat.format(messages.getString("info.server.start.update.config"), "jvm.options", jvmOptions.getCanonicalPath()));
        }

        // copy configuration file to server directory if end-user set it.
        if (serverEnv != null && serverEnv.exists()) {
            Copy copy = (Copy) ant.createTask("copy");
            copy.setFile(serverEnv);
            copy.setTofile(new File(serverDirectory, "server.env"));
            copy.setOverwrite(overwrite);
            copy.execute();

            log.info(MessageFormat.format(messages.getString("info.server.start.update.config"), "server.env", serverEnv.getCanonicalPath()));
        }

        // copy configuration file to server directory if end-user set it.
        if (configFile != null && configFile.exists()) {
            Copy copy = (Copy) ant.createTask("copy");
            copy.setFile(configFile);
            copy.setTofile(new File(serverDirectory, "server.xml"));
            copy.setOverwrite(overwrite);
            copy.execute();

            log.info(MessageFormat.format(messages.getString("info.server.start.update.config"), "server.xml", configFile.getCanonicalPath()));
        }

        // copy boot properties file to server directory if end-user set it.
        if (bootProps != null && bootProps.exists()) {
            Copy copy = (Copy) ant.createTask("copy");
            copy.setFile(bootProps);
            copy.setTofile(new File(serverDirectory, "bootstrap.properties"));
            copy.setOverwrite(overwrite);
            copy.execute();

            log.info(MessageFormat.format(messages.getString("info.server.start.update.config"), "bootstrap.properties", bootProps.getCanonicalPath()));
        }
    }

    protected void checkServerHomeExists() throws MojoExecutionException {
        if (!serverHome.exists()) {
            throw new MojoExecutionException(MessageFormat.format(messages.getString("error.server.home.noexist"), serverHome));
        }
    }

    protected void checkServerDirectoryExists() throws MojoExecutionException {
        if (!serverDirectory.exists()) {
            throw new MojoExecutionException(MessageFormat.format(messages.getString("error.server.noexist"), serverName));
        }
    }

}
