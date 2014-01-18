package net.wasdev.wlp.maven.plugins.server;

import java.io.File;
import java.text.MessageFormat;

import net.wasdev.wlp.ant.ServerTask;

/**
 * Dump diagnostic information from the server into an archive.
 * 
 * @goal dump-server
 * 
 */
public class DumpServerMojo extends StartDebugMojoSupport {

    /**
     * Location of the target archive file.
     * 
     * @parameter expression="${archive}"
     */
    private File archive;

    /**
     * Type of dump information to collect. 
     * Valid values are "heap", "system", and "thread".
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
        serverTask.setOperation("dump");
        serverTask.setArchive(archive);
        serverTask.setInclude(include);
        serverTask.execute();
    }
}
