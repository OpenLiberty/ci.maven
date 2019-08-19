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
package io.openliberty.tools.server;

import java.io.File;
import java.text.MessageFormat;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import net.wasdev.wlp.ant.ServerTask;

/**
 * Package a liberty server
 */
@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE)
public class PackageServerMojo extends StartDebugMojoSupport {

    /**
     * Locate where server is packaged.
     */
    @Parameter(property = "packageFile")
    private File packageFile = null;

    /**
     * Package type. One of "all", "usr", or "minify".
     */
    @Parameter(property = "include")
    private String include;

    /**
     * Os supported. Specifies the operating systems that you want the packaged server to support. 
     *     Supply a comma-separated list. The default value is any, indicating that the server is to 
     *     be deployable to any operating system supported by the source. To specify that an operating 
     *     system is not to be supported, prefix it with a minus sign ("-"). For a list of operating system 
     *     values, refer to the OSGi Alliance web site at the following URL: 
     *     http://www.osgi.org/Specifications/Reference#os. 
     *     This option applies only to the package operation, and can be used only with the 
     *     --include=minify option. If you exclude an operating system, you cannot later include it if you 
     *     repeat the minify operation on the archive.
     */
    @Parameter(property = "os")
    private String os;
    
    @Parameter
    private boolean attach;

    /**
     * Skips this goal
     */
    @Parameter(property = "skipLibertyPackage", defaultValue = "false")
    protected boolean skipLibertyPackage = false;
    
    @Override
    protected void doExecute() throws Exception {
        if (skip || skipLibertyPackage) {
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
        String fileType = getPackageFileType(include);
        String projectBuildDir = project.getBuild().getDirectory();
        String projectBuildName = project.getBuild().getFinalName();
        if (packageFile != null) {
            if (packageFile.isDirectory()) {
                packageFile = new File(packageFile, projectBuildName + fileType);
            }
        } else {
            packageFile = new File(projectBuildDir, projectBuildName + fileType);
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
    
    private String getPackageFileType(String include) {
    	if(include != null && include.contains("runnable")) {
    		return ".jar";
    	}
    	return ".zip";
    }
}
