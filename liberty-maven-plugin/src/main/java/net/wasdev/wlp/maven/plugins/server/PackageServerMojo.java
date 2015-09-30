/**
 * (C) Copyright IBM Corporation 2014.
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
import java.text.MessageFormat;

import org.apache.maven.plugin.MojoFailureException;

import net.wasdev.wlp.ant.ServerTask;

/**
 * Package a liberty server
 * 
 * @goal package-server
 * 
 * @phase package
 */
public class PackageServerMojo extends StartDebugMojoSupport {

    /**
     * Locate where server is packaged.
     * 
     * @parameter expression="${packageFile}"
     */
    private File packageFile = null;

    /**
     * Package type. One of "all", "usr", or "minify".
     * 
     * @parameter expression="${include}"
     */
    private String include;

    /**
     * Support for specific OS. Comma-delimited list of values.
     * 
     * @parameter expression="${os}"
     */
    private String os;
    
    /**
     * @parameter
     */
    private boolean attach;

    @Override
    protected void doExecute() throws Exception {
        if (skip) {
            return;
        }
        if (isInstall) {
            installServerAssembly();
        } else {
            log.info(MessageFormat.format(messages.getString("info.install.type.preexisting"), ""));
            checkServerHomeExists();
            checkServerDirectoryExists();
        }

        log.info(MessageFormat.format(messages.getString("info.server.package"), serverName));
        ServerTask serverTask = initializeJava();
        copyConfigFiles();
        serverTask.setOperation("package");
        if (packageFile != null) {
            if (packageFile.isDirectory()) {
                packageFile = new File(packageFile, serverName + ".zip");
            }
        } else {
            packageFile = new File(serverDirectory, serverName + ".zip");
        }
        serverTask.setArchive(packageFile);
        serverTask.setInclude(include);
        serverTask.setOs(os);
        log.info(MessageFormat.format(messages.getString("info.server.package.file.location"), packageFile.getCanonicalPath()));
        serverTask.execute();

        if (attach || (project != null && "liberty-assembly".equals(project.getPackaging()))) {
            if (project == null) {
                throw new MojoFailureException(MessageFormat.format(messages.getString("error.server.package.no.project"), ""));
            }
            project.getArtifact().setFile(packageFile);
        }
    }
}
