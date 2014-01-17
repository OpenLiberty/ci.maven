package net.wasdev.wlp.maven.plugins.server;

import java.text.MessageFormat;

import net.wasdev.wlp.ant.ServerTask;

/**
 * Check a liberty server status
 * 
 * @goal server-status
 * 
 * 
 */
public class CheckStatusMojo extends StartDebugMojoSupport {

    protected void doExecute() throws Exception {
        if (isInstall) {
            installServerAssembly();
        } else {
            log.info(MessageFormat.format(messages.getString("info.install.type.preexisting"), ""));
            checkServerHomeExists();
        }

        log.info(MessageFormat.format(messages.getString("info.server.status.check"), ""));

        ServerTask serverTask = initializeJava();
        serverTask.setOperation("status");
        serverTask.execute();
    }
}
