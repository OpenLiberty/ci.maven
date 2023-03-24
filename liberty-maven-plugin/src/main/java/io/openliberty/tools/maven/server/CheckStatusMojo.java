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

import java.io.IOException;
import java.text.MessageFormat;

import org.apache.maven.MavenExecutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

import io.openliberty.tools.ant.ServerTask;

/**
 * Check a liberty server status
 */
@Mojo(name = "status")
public class CheckStatusMojo extends StartDebugMojoSupport {

    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("\nSkipping status goal.\n");
            return;
        }
        if (isInstall) {
            try {
                installServerAssembly();
            } catch (IOException ioException) {
                throw new MojoExecutionException(ioException);
            }

        } else {
            getLog().info(MessageFormat.format(messages.getString("info.install.type.preexisting"), ""));
            checkServerHomeExists();
        }

        getLog().info(MessageFormat.format(messages.getString("info.server.status.check"), ""));

        ServerTask serverTask = initializeJava();
        serverTask.setOperation("status");
        serverTask.execute();
    }
}
