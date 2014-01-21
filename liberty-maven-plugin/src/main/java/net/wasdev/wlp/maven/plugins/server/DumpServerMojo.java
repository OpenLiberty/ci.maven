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
     * Include heap dump information. 
     * 
     * @parameter expression="${heapDump}"
     */
    private boolean heapDump;
    
    /**
     * Include system dump information. 
     * 
     * @parameter expression="${systemDump}"
     */
    private boolean systemDump;
    
    /**
     * Include thread dump information. 
     * 
     * @parameter expression="${threadDump}"
     */
    private boolean threadDump;

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
        serverTask.setInclude(generateInclude());
        serverTask.execute();
    }
    
    private String generateInclude() {
        StringBuilder builder = new StringBuilder();
        
        if (heapDump) {
            builder.append("heap");
        } 
        if (systemDump) {
            if (builder.length() != 0) {
                builder.append(",");
            }
            builder.append("system");
        }
        if (threadDump) {
            if (builder.length() != 0) {
                builder.append(",");
            }
            builder.append("thread");
        }
        
        return (builder.length() == 0) ? null : builder.toString();
    }
}
