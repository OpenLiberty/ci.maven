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

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;

import io.openliberty.tools.ant.ServerTask;

/**
 * Create a liberty server
  */
@Mojo(name = "create")
public class CreateServerMojo extends PluginConfigSupport {

    /**
     * Name of the template to use when creating a server.
     */
    @Parameter(property = "template")
    private String template;

    /**
     * Directory of custom configuration files
     */
    @Parameter(property = "libertySettingsFolder", defaultValue = "${basedir}/src/main/resources/etc")
    private File libertySettingsFolder;
    
    /**
     * Specifies the --no-password option
     */
    @Parameter(property = "noPassword", defaultValue = "false")
    private boolean noPassword;
    
    @Override
    protected void doExecute() throws Exception {
        if (skip) {
            getLog().info("\nSkipping create goal.\n");
            return;
        }
        if (isInstall) {
            installServerAssembly();
        } else {
            log.info(MessageFormat.format(messages.getString("info.install.type.preexisting"), ""));
            checkServerHomeExists();
        }

        boolean createServer = false;

        if (!serverDirectory.exists()) {
            createServer = true;
        } else if (refresh) {
            FileUtils.forceDelete(serverDirectory);
            createServer = true;
        }

        if (createServer) {
            // server does not exist or we are refreshing it - create it
            log.info(MessageFormat.format(messages.getString("info.server.start.create"), serverName));
            ServerTask serverTask = initializeJava();
            serverTask.setOperation("create");
            serverTask.setTemplate(template);
            serverTask.setNoPassword(noPassword);
            serverTask.execute();
            log.info(MessageFormat.format(messages.getString("info.server.create.created"), serverName, serverDirectory.getCanonicalPath()));
        }
        
        // copy files _after_ we create the server
        copyConfigFiles();

        copyLibertySettings();
    }

    private void copyLibertySettings() throws MojoExecutionException, IOException {
        if (libertySettingsFolder.exists()) {
            if (!libertySettingsFolder.isDirectory()) {
                throw new MojoExecutionException("The Liberty configuration <libertySettingsFolder> must be a directory. Value found: " + libertySettingsFolder.toString());
            }

            log.info(MessageFormat.format(messages.getString("info.variable.set"), "libertySettingsFolder", libertySettingsFolder));

            // copy config files to <install directory>/etc
            File[] files = libertySettingsFolder.listFiles();
            if (files != null && files.length > 0) {
                File installDir = new File(installDirectory + "/etc");
                if (!installDir.exists()) {
                    installDir.mkdirs();
                }

                log.info("Copying " + files.length + " file" + ((files.length == 1) ? "":"s") + " to " + installDir.getCanonicalPath());
                FileUtils.copyDirectory(libertySettingsFolder, installDir);
            } else {
                log.info("No custom Liberty configuration files found.");
            }
        } else {
            log.debug("No custom Liberty configuration folder found.");
        }
    }
}
