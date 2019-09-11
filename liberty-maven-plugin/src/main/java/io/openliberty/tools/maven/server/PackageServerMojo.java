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

import java.util.List;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.openliberty.tools.ant.ServerTask;

/**
 * Package a liberty server
 */
@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE)
public class PackageServerMojo extends StartDebugMojoSupport {

    private File packageFile = null;

    /**
     * Package type. "zip" or "jar"
     */
    @Parameter(property= "packageType")
    private String packageType;

    /**
     * Package name. defaults to ${project.build.finalName}
     */
    @Parameter(property = "packageName")
    private String packageName;

    /**
     * Package directory. defaults to target folder
     */
    @Parameter(property= "packageDirectory")
    private String packageDirectory;

    /**
     * What to include. One of "all", "usr", "minify", or "wlp".
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

        setPackageFilePath();

        serverTask.setArchive(packageFile);
        validateInclude();
        serverTask.setInclude(include);
        serverTask.setOs(os);
        log.info(MessageFormat.format(messages.getString("info.server.package.file.location"), packageFile.getCanonicalPath()));
        serverTask.execute();

        if ("liberty-assembly".equals(project.getPackaging())) {
            project.getArtifact().setFile(packageFile);
        } else if (attach) {
            if (packageType != project.getPackaging()) {
                throw new MojoFailureException("packageType must match project packaging type.");
            }

            project.getArtifact().setFile(packageFile);
        }
    }

    private void validateInclude() throws MojoFailureException {
        ArrayList<String> includeValues;
        List<String> includeStrings;

        if (include != null && !include.isEmpty()) {
            include = include.trim();
            includeStrings = Arrays.asList(include.split(","));
            includeValues = new ArrayList<String>(includeStrings);
            for (int i = 0; i < includeValues.size(); i++) {
                String value = includeValues.get(i);
                includeValues.set(i, value.trim());
            }
        } else {
            includeValues = new ArrayList<String>();
        }

        // if jar, validate include options, and add runnable
        if (packageType.equals("jar")) {
            if (includeValues.contains("usr") || includeValues.contains("wlp")) {
                throw new MojoFailureException("Package type jar cannot be used with `usr` or `wlp`.");
            }

            if (!includeValues.contains("runnable")) {
                includeValues.add("runnable");
            }
        }

        if (includeValues.size() > 0) {
            include = String.join(",", includeValues);
        }
    }

    /**
     * Sets `packageFile` based on specified file type, package dir, and package name
     * Sets default values for unspecified file type, package dir, and package name
     * 
     * @throws MojoFailureException
     * @throws IOException
     */
    private void setPackageFilePath() throws MojoFailureException, IOException {
        String projectFileType = getPackageFileType();
        String projectBuildDir = getPackageDirectory();
        String projectBuildName = getPackageName();
        packageFile = new File(projectBuildDir, projectBuildName + projectFileType);
    }
    
    /**
     * Returns file extension for specified package type
     * 
     * @param packageType "jar" or "zip"
     * @param include parameter, for checking if "jar" is valid for the include type
     * @return package file extension, or default to "zip"
     * @throws MojoFailureException
     */
    private String getPackageFileType() throws MojoFailureException {
    	if (packageType != null && packageType.equals("jar")) {
            if (include == null || include.isEmpty() || include.equals("all") || include.equals("minify")) {
                return ".jar";
            } else {
                throw new MojoFailureException("The jar packageType requires `all` or `minify` in the `include` parameter");
            }
    	} else {
            if (packageType != null && !packageType.equals("zip")) {
                log.info(packageType + " not supported. Defaulting to 'zip'");
            }
            packageType = "zip";
            return ".zip";
        }
    }

    /**
     * Returns package name
     * 
     * @param packageName
     * @return specified package name, or default ${project.build.finalName} if unspecified
     */
    private String getPackageName() {
        if (packageName != null && !packageName.isEmpty()) {
            return packageName;
        }
        packageName = project.getBuild().getFinalName();
        return packageName;
    }

    /**
     * Returns canonical path to package directory
     * 
     * @param packageDirectory
     * @return canonical path to specified package directory, or default ${project.build.directory} (target) if unspecified
     * @throws IOException
     */
    private String getPackageDirectory() throws IOException {
        if (packageDirectory != null && !packageDirectory.isEmpty()) {
            // done: check if path is relative or absolute, convert to canonical
            File dir = new File(packageDirectory);
            if (dir.isAbsolute()) {
                return dir.getCanonicalPath();
            } else { //relative path
                return new File(project.getBuild().getDirectory(), packageDirectory).getCanonicalPath();
            }
        } else {
            packageDirectory = project.getBuild().getDirectory();
            return packageDirectory;
        }
    }

}
