package net.wasdev.wlp.maven.plugins.server;

import java.io.File;
import java.text.MessageFormat;

import org.apache.maven.plugin.MojoExecutionException;

import net.wasdev.wlp.maven.plugins.BasicSupport;

/**
 * Check a liberty server status
 * 
 * @goal server-status
 * 
 * 
 */
public class CheckStatusMojo extends BasicSupport {

    protected void doExecute() throws Exception {

        if (!serverHome.exists()) {
            throw new MojoExecutionException(MessageFormat.format(messages.getString("error.server.home.noexist"), serverHome));
        }

        log.info(MessageFormat.format(messages.getString("info.server.status.check"), ""));

        // check server directory
        if (!serverDirectory.exists()) {
            log.info(MessageFormat.format(messages.getString("error.server.noexist"), serverName));
            return;
        }

        // check server status via server lock file.
        File lockFile = new File(serverDirectory, "workarea/.sLock");
        if (lockFile.exists()) {
            log.info(MessageFormat.format(messages.getString("info.server.status.running"), serverName));
        } else {
            log.info(MessageFormat.format(messages.getString("info.server.status.stopped"), serverName));
        }
    }
}
