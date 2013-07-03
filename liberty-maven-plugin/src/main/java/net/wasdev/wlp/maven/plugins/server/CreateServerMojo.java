package net.wasdev.wlp.maven.plugins.server;

import java.text.MessageFormat;

import org.codehaus.plexus.util.FileUtils;

import net.wasdev.wlp.ant.ServerTask;

/**
 * Create a liberty server
 * 
 * @goal create-server
 * 
 * 
 */
public class CreateServerMojo extends StartDebugMojoSupport {

    @Override
    protected void doExecute() throws Exception {
        if (isInstall) {
            installServerAssembly();
        } else {
            log.info(MessageFormat.format(messages.getString("info.install.type.preexisting"), ""));
            checkServerHomeExists();
        }

        boolean createServer = false;

        if (!serverDirectory.exists()) {
            createServer = true;
        } else if (refresh) {
            FileUtils.forceDelete(serverDirectory);
            createServer = true;
        }

        if (createServer) {
            // server does not exist or we are refreshing it - create it
            log.info(MessageFormat.format(messages.getString("info.server.start.create"), serverName));
            ServerTask serverTask = initializeJava();
            serverTask.setOperation("create");
            serverTask.execute();
            // copy files _after_ we create the server
            copyConfigFiles(true);
            log.info(MessageFormat.format(messages.getString("info.server.create.created"), serverName, serverDirectory.getCanonicalPath()));
        } else {
            // server exists - copy files over
            copyConfigFiles();
        }
    }
}
