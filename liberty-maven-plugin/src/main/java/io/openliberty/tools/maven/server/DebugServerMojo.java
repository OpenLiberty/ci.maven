/**
 * (C) Copyright IBM Corporation 2017, 2023.
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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import io.openliberty.tools.ant.ServerTask;

/**
 * Start a liberty server in debug mode
 */
@Mojo(name = "debug", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)

public class DebugServerMojo extends StartDebugMojoSupport {

    /**
     * Clean all cached information on server start up.
     */
    @Parameter(property = "clean", defaultValue = "false")
    protected boolean clean;
    
    @Override
    public void execute() throws MojoExecutionException {
        init();

        if (skip) {
            getLog().info("\nSkipping debug goal.\n");
            return;
        }

        doDebug();
    }

    private void doDebug() throws MojoExecutionException {
        if (isInstall) {
            try {
                installServerAssembly();
            } catch (IOException e) {
                throw new MojoExecutionException("Error installing the Liberty server.", e);
            }
        } else {
            getLog().info(MessageFormat.format(messages.getString("info.install.type.preexisting"), ""));
            checkServerHomeExists();
        }

        ServerTask serverTask = initializeJava();
        try {
            copyConfigFiles();
        } catch (IOException e) {
            throw new MojoExecutionException("Error copying configuration files to Liberty server directory.", e);
        }
        serverTask.setClean(clean);
        serverTask.setOperation("debug");
        serverTask.execute();    
    }

}
