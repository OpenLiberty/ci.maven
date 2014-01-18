package net.wasdev.wlp.maven.plugins.server;

import java.text.MessageFormat;

import net.wasdev.wlp.ant.ServerTask;

/**
 * Dump diagnostic information from the server JVM.
 * 
 * @goal javadump-server
 * 
 */
public class JavaDumpServerMojo extends StartDebugMojoSupport {

    /**
     * Type of dump information to collect. 
     * Valid values are "heap", and "system".
     * 
     * @parameter expression="${include}"
     */
    private String include;

    @Override
    protected void doExecute() throws Exception {
        if (isInstall) {
            installServerAssembly();
        } else {
            log.info(MessageFormat.format(messages.getString("info.install.type.preexisting"), ""));
            checkServerHomeExists();
            checkServerDirectoryExists();
        }

        ServerTask serverTask = initializeJava();
        copyConfigFiles();
        serverTask.setOperation("javadump");
        serverTask.setInclude(include);
        serverTask.execute();
    }
}
