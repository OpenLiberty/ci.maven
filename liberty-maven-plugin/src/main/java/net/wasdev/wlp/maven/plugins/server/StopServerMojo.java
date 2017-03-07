/**
 * (C) Copyright IBM Corporation 2014, 2017.
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
package net.wasdev.wlp.maven.plugins.server;

import java.text.MessageFormat;

import net.wasdev.wlp.ant.ServerTask;

/**
 * Stop a liberty server
 * 
 * @goal stop-server
 * 
 * 
 */
public class StopServerMojo extends StartDebugMojoSupport {

    /**
     * Timeout to verify stop successfully
     * 
     * @parameter expression="${serverStopTimeout}" default-value="30"
     */
    protected long serverStopTimeout = 30;

    @Override
    protected void doExecute() throws Exception {
        if (skip) {
            return;
        }
        
        log.info(MessageFormat.format(messages.getString("info.server.stopping"), serverName));
        
        if (serverDirectory.exists()) {
            ServerTask serverTask = initializeJava();
            serverTask.setTimeout(Long.toString(serverStopTimeout * 1000));
            serverTask.setOperation("stop");
            serverTask.execute();
        }
        else {
            log.info(MessageFormat.format(messages.getString("info.server.stop.noexist"), serverName));
        }
    }
}
