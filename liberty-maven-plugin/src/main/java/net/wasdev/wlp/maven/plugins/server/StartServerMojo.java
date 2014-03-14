package net.wasdev.wlp.maven.plugins.server;

import java.text.MessageFormat;

import org.apache.maven.plugin.MojoExecutionException;

import net.wasdev.wlp.ant.ServerTask;

/**
 * Start a liberty server
 * 
 * @goal start-server
 */
public class StartServerMojo extends StartDebugMojoSupport {

    /**
     * Time in seconds to wait while verifying that the server has started.
     * 
     * @parameter expression="${verifyTimeout}" default-value="30"
     */
    private int verifyTimeout = 30;

    /**
     * Time in seconds to wait while verifying that the server has started.
     * 
     * @parameter expression="${serverStartTimeout}" default-value="30"
     */
    private int serverStartTimeout = 30;

    /**
     * comma separated list of app names to wait for
     * 
     * @parameter expression="${applications}"
     */
    private String applications;

    /**
     * Clean all cached information on server start up.
     * 
     * @parameter expression="${clean}" default-value="false"
     */
    protected boolean clean;

    @Override
    protected void doExecute() throws Exception {
        if (isInstall) {
            installServerAssembly();
        } else {
            log.info(MessageFormat.format(messages.getString("info.install.type.preexisting"), ""));
            checkServerHomeExists();
        }

        ServerTask serverTask = initializeJava();
        copyConfigFiles();
        serverTask.setClean(clean);
        serverTask.setOperation("start");
        // Set server start timeout
        if (serverStartTimeout < 0) {
            serverStartTimeout = 30;
        }
        serverTask.setTimeout(Long.toString(serverStartTimeout * 1000));
        serverTask.execute();

        if (verifyTimeout < 0) {
            verifyTimeout = 30;
        }
        long timeout = verifyTimeout * 1000;
        long endTime = System.currentTimeMillis() + timeout;
        if (applications != null) {
            String[] apps = applications.split("[,\\s]+");
            for (String archiveName : apps) {
                String startMessage = serverTask.waitForStringInLog(START_APP_MESSAGE_REGEXP + archiveName, timeout, serverTask.getLogFile());
                if (startMessage == null) {
                    stopServer();
                    throw new MojoExecutionException(MessageFormat.format(messages.getString("error.server.start.verify"), verifyTimeout));
                }
                timeout = endTime - System.currentTimeMillis();
            }
        }
    }

    private void stopServer() {
        try {
            ServerTask serverTask = initializeJava();
            serverTask.setOperation("stop");
            serverTask.execute(); 
        } catch (Exception e) {
            // ignore
            log.debug("Error stopping server", e);
        }
    }
}
