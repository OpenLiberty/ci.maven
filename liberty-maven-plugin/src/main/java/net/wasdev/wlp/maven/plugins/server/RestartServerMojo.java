package net.wasdev.wlp.maven.plugins.server;

import java.io.File;
import java.text.MessageFormat;

import org.apache.maven.plugin.MojoExecutionException;

import net.wasdev.wlp.maven.plugins.BasicSupport;
import net.wasdev.wlp.maven.plugins.ServerProxy;

/**
 * Restart server
 * 
 * @goal restart-server
 * 
 * 
 */
public class RestartServerMojo extends BasicSupport {

    protected void doExecute() throws Exception {

        if (!serverHome.exists()) {
            throw new MojoExecutionException(MessageFormat.format(messages.getString("error.server.home.noexist"), serverHome));

        }

        // check server directory
        if (!serverDirectory.exists()) {
            throw new MojoExecutionException(MessageFormat.format(messages.getString("error.server.noexist"), serverName));
        }
        // Get mbean server
        File workAreaPath = new File(serverDirectory, "workarea/");
        ServerProxy server = new ServerProxy(workAreaPath);
        server.restartOSGiFramework();

    }

}
