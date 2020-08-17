/**
 * (C) Copyright IBM Corporation 2014, 2019.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openliberty.tools.maven.server;

import java.text.MessageFormat;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.openliberty.tools.ant.ServerTask;

/**
 * Start a liberty server
 */
@Mojo(name = "start")

public class StartServerMojo extends StartDebugMojoSupport {

    /**
     * Time in seconds to wait while verifying that the server has started.
     */
    @Parameter(property = "verifyTimeout", defaultValue = "30")
    private int verifyTimeout = 30;

    /**
     * Time in seconds to wait while verifying that the server has started.
     */
    @Parameter(property = "serverStartTimeout", defaultValue = "30")
    private int serverStartTimeout = 30;

    /**
     * comma separated list of app names to wait for
     */
    @Parameter(property = "applications")
    private String applications;

    /**
     * Clean all cached information on server start up.
     */
    @Parameter(property = "clean", defaultValue = "false")
    protected boolean clean;
    
    /**
     * Start the server in embedded mode
     */
    @Parameter(property = "embedded", defaultValue = "false")
    private boolean embedded;

    @Override
    protected void doExecute() throws Exception {
        if (skip) {
            getLog().info("\nSkipping start goal.\n");
            return;
        }
        if (isInstall) {
            installServerAssembly();
        } else {
            log.info(MessageFormat.format(messages.getString("info.install.type.preexisting"), ""));
            checkServerHomeExists();
        }

        ServerTask serverTask = initializeJava();
        copyConfigFiles();
        serverTask.setUseEmbeddedServer(embedded);
        serverTask.setClean(clean);
        serverTask.setOperation("start");
        // Set server start timeout
        if (serverStartTimeout < 0) {
            serverStartTimeout = 30;
        }
        serverTask.setTimeout(Long.toString(serverStartTimeout * 1000));
        serverTask.execute();

        if (verifyTimeout < 0) {
            verifyTimeout = 30;
        }
        long timeout = verifyTimeout * 1000;
        long endTime = System.currentTimeMillis() + timeout;
        if (applications != null) {
            String[] apps = applications.split("[,\\s]+");
            for (String archiveName : apps) {
                String startMessage = serverTask.waitForStringInLog(START_APP_MESSAGE_REGEXP + archiveName, timeout, serverTask.getLogFile());
                if (startMessage == null) {
                    stopServer();
                    throw new MojoExecutionException(MessageFormat.format(messages.getString("error.server.start.verify"), verifyTimeout));
                }
                timeout = endTime - System.currentTimeMillis();
            }
        }
    }

    private void stopServer() {
        try {
            ServerTask serverTask = initializeJava();
            serverTask.setOperation("stop");
            serverTask.execute(); 
        } catch (Exception e) {
            // ignore
            log.debug("Error stopping server", e);
        }
    }
}
