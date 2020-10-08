/**
 * (C) Copyright IBM Corporation 2017, 2019.
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
 * Start a liberty server in debug mode
 */
@Mojo(name = "debug")

public class DebugServerMojo extends StartDebugMojoSupport {

    /**
     * Clean all cached information on server start up.
     */
    @Parameter(property = "clean", defaultValue = "false")
    protected boolean clean;

    @Override
    protected void doExecute() throws Exception {
        if (skip) {
            getLog().info("\nSkipping debug goal.\n");
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
        serverTask.setClean(clean);
        serverTask.setOperation("debug");
        serverTask.execute();        
    }

}
