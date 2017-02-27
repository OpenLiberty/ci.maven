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
package net.wasdev.wlp.maven.plugins.server;

import net.wasdev.wlp.ant.CleanTask;

/**
 * Clean the logs, workarea, dropins and apps directories.
 * 
 * @goal clean-server
 * 
 */
public class CleanServerMojo extends StartDebugMojoSupport {
    
    /**
     * Clean the logs directory.
     * @parameter property="cleanLogs" default-value="true"
     */
    private boolean logs = true;
    
    /**
     * Clean the workarea directory.
     * @parameter property="cleanWorkarea" default-value="true"
     */
    private boolean workarea = true;
    
    /**
     * Clean the dropins directory.
     * @parameter property="cleanDropins" default-value="false"
     */
    private boolean dropins = false;
    
    /**
     * Clean the apps directory.
     * @parameter property="cleanApps" default-value="false"
     */
    private boolean apps = false;
    
    protected void doExecute() throws Exception {
        if (skip) {
            return;
        }
        
        CleanTask cleanTask = (CleanTask) ant.createTask("antlib:net/wasdev/wlp/ant:clean");
        cleanTask.setInstallDir(installDirectory);
        cleanTask.setServerName(serverName);
        cleanTask.setUserDir(userDirectory);
        cleanTask.setOutputDir(outputDirectory);
        cleanTask.setLogs(logs);
        cleanTask.setWorkarea(workarea);
        cleanTask.setDropins(dropins);
        cleanTask.setApps(apps);
        cleanTask.execute();
    }

}
