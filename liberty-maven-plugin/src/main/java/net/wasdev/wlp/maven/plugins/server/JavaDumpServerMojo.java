package net.wasdev.wlp.maven.plugins.server;

import java.text.MessageFormat;

import net.wasdev.wlp.ant.ServerTask;

/**
 * Dump diagnostic information from the server JVM.
 * 
 * @goal java-dump-server
 * 
 */
public class JavaDumpServerMojo extends StartDebugMojoSupport {

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
        
        return (builder.length() == 0) ? null : builder.toString();
    }
}
