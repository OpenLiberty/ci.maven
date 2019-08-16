/**
 * (C) Copyright IBM Corporation 2016, 2017.
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
package io.openliberty.tools.server;

import net.wasdev.wlp.ant.CleanTask;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Clean the logs, workarea, dropins and apps directories.
 */
@Mojo(name = "clean-server")
public class CleanServerMojo extends StartDebugMojoSupport {

    /**
     * Clean the logs directory.
     */
    @Parameter(alias = "logs", property = "cleanLogs", defaultValue = "true")
    private boolean cleanLogs = true;

    /**
     * Clean the workarea directory.
     */
    @Parameter(alias = "workarea", property = "cleanWorkarea", defaultValue = "true")
    private boolean cleanWorkarea = true;

    /**
     * Clean the dropins directory.
     */
    @Parameter(alias = "dropins", property = "cleanDropins", defaultValue = "false")
    private boolean cleanDropins = false;

    /**
     * Clean the apps directory.
     */
    @Parameter(alias = "apps", property = "cleanApps", defaultValue = "false")
    private boolean cleanApps = false;

    protected void doExecute() throws Exception {
        if (skip) {
            return;
        }
        
        CleanTask cleanTask = (CleanTask) ant.createTask("antlib:net/wasdev/wlp/ant:clean");
        cleanTask.setInstallDir(installDirectory);
        cleanTask.setServerName(serverName);
        cleanTask.setUserDir(userDirectory);
        cleanTask.setOutputDir(outputDirectory);
        cleanTask.setLogs(cleanLogs);
        cleanTask.setWorkarea(cleanWorkarea);
        cleanTask.setDropins(cleanDropins);
        cleanTask.setApps(cleanApps);
        cleanTask.execute();
    }
}
