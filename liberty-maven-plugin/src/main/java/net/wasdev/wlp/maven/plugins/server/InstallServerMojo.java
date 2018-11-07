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

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Install a liberty server
 */
@Mojo(name = "install-server", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class InstallServerMojo extends PluginConfigSupport {

    /**
     * Directory of custom configuration files
     */
    @Parameter(property = "libertySettingsFolder", defaultValue = "${basedir}/src/main/resources/etc")
    private File libertySettingsFolder;

    @Override
    protected void doExecute() throws Exception {
        if (skip) {
            return;
        }
        log.info(MessageFormat.format(messages.getString("info.variable.set"), "libertySettingsFolder", libertySettingsFolder));

        copyLibertySettings();

        installServerAssembly();
    }

    private void copyLibertySettings() throws MojoExecutionException, IOException {
        if (libertySettingsFolder.isDirectory()) {
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
            throw new MojoExecutionException("The Liberty configuration <libertySettingsFolder> must be an existing directory. Value found: " + libertySettingsFolder.toString());
        }
    }
}
