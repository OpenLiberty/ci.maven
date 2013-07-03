package net.wasdev.wlp.maven.plugins.server;

import java.text.MessageFormat;

import net.wasdev.wlp.ant.ServerTask;

/**
 * Stop a liberty server
 * 
 * @goal stop-server
 * 
 * 
 */
public class StopServerMojo extends StartDebugMojoSupport {

    /**
     * Timeout to verify stop successfully
     * 
     * @parameter expression="${timeout}" default-value="40000"
     */
    protected long timeout = 40000;

    @Override
    protected void doExecute() throws Exception {
        if (isInstall) {
            installServerAssembly();
        } else {
            log.info(MessageFormat.format(messages.getString("info.install.type.preexisting"), ""));
            checkServerHomeExists();
            checkServerDirectoryExists();
        }

        log.info(MessageFormat.format(messages.getString("info.server.stopping"), serverName));
        ServerTask serverTask = initializeJava();
        serverTask.setTimeout(Long.toString(timeout));
        serverTask.setOperation("stop");
        serverTask.execute();
    }
}
