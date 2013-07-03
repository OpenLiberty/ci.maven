package net.wasdev.wlp.maven.plugins.server;

import java.text.MessageFormat;

import org.apache.tools.ant.taskdefs.Java;

/**
 * Debug liberty server
 * 
 * @goal debug-server
 * 
 * 
 */
public class DebugServerMojo extends StartDebugMojoSupport {

    /**
     * Flag to control if we background the server or block Maven execution.
     * 
     * @parameter expression="${debugOption}" default-value="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=7777"
     */

    private String debugOption = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=7777";

    protected void doExecute() throws Exception {
        //        final Java java = initializeJava();
        //
        //        log.info(MessageFormat.format(messages.getString("info.server.debug.options"), debugOption));
        //        java.createJvmarg().setValue(debugOption);
        //        java.execute();
    }
}
