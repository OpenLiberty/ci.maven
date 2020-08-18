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

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.openliberty.tools.ant.ServerTask;

/**
 * Stop a liberty server
 */
@Mojo(name = "stop")
public class StopServerMojo extends StartDebugMojoSupport {
    
    /**
     * Stop the server in embedded mode
     */
    @Parameter(property = "embedded", defaultValue = "false")
    private boolean embedded;

    @Override
    protected void doExecute() throws Exception {
        if (skip) {
            getLog().info("\nSkipping stop goal.\n");
            return;
        }
        
        log.info(MessageFormat.format(messages.getString("info.server.stopping"), serverName));
        
        if (serverDirectory.exists()) {
            try {
                ServerTask serverTask = initializeJava();
                serverTask.setUseEmbeddedServer(embedded);
                serverTask.setOperation("stop");
                serverTask.execute();
            } catch (Exception e) {
                // Most often when server stop fails, it is because the server does
                // not fully exist in the file structure and is not running anyway.
                // Continue with a warning rather than ending with hard failure especially
                // as we have bound stop-server to a clean.
                log.warn(MessageFormat.format(messages.getString("warn.server.stopped"), serverName));
            }
        }
        else {
            log.info(MessageFormat.format(messages.getString("info.server.stop.noexist"), serverName));
        }
    }
}
