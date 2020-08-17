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
import java.util.Map;
import java.util.HashMap;
import java.util.EnumSet;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.openliberty.tools.ant.ServerTask;

/**
 * Package a liberty server
 */
@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE)
public class PackageServerMojo extends StartDebugMojoSupport {

    private enum PackageFileType {
        JAR("jar"),
        TAR("tar"),
        TARGZ("tar.gz"),
        ZIP("zip");

        private final String value;

        private PackageFileType(final String val) {
            this.value = val;
        }

        private static final Map<String, PackageFileType> lookup = new HashMap<String, PackageFileType>();

        static {
            for (PackageFileType s : EnumSet.allOf(PackageFileType.class)) {
               lookup.put(s.value, s);
            }
        }

        public static PackageFileType getPackageFileType(String input) {
            return lookup.get(input);
        } 

        public String getValue() {
            return this.value;
        }
    }

    private PackageFileType packageFileType = null;
    private File packageFile = null;

    /**
     * Package type. "zip", "jar", "tar", or "tar.gz"
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
     * What to include. One of "all", "usr", "minify", "wlp", "runnable", "all,runnable", or "minify,runnable".
     */
    @Parameter(property = "include")
    private String include;

    /**
     * root server folder in archive
     */
    @Parameter(property = "serverRoot")
    private String serverRoot;


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
        // Set default outputDirectory to liberty-alt-output-dir for package goal.
        if (defaultOutputDirSet) {
            outputDirectory = new File(project.getBuild().getDirectory(), "liberty-alt-output-dir");
        }

        if (skip || skipLibertyPackage) {
            getLog().info("\nSkipping package goal.\n");
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

        setAndCreatePackageFilePath();

        serverTask.setArchive(packageFile);
        serverTask.setInclude(include);
        serverTask.setOs(os);
        serverTask.setServerRoot(serverRoot);
        log.info(MessageFormat.format(messages.getString("info.server.package.file.location"), packageFile.getCanonicalPath()));
        serverTask.execute();

        if ("liberty-assembly".equals(project.getPackaging())) {
            project.getArtifact().setFile(packageFile);
        } else if (attach) {
            if (packageFileType.getValue() != project.getPackaging()) {
                throw new MojoExecutionException("packageType must match project packaging type.");
            }

            project.getArtifact().setFile(packageFile);
        }
    }

    private ArrayList<String> parseInclude() {
        ArrayList<String> includeValues;
        List<String> includeStrings;

        if (include != null && !include.isEmpty()) {
            include = include.trim();
            includeStrings = Arrays.asList(include.split(","));
            includeValues = new ArrayList<String>(includeStrings);
            for (int i = 0; i < includeValues.size(); i++) {
                String value = includeValues.get(i);
                if (value.trim().length() > 0) {
                    includeValues.set(i, value.trim());
                }
            }
        } else {
            includeValues = new ArrayList<String>();
        }
        return includeValues;
    }

    /**
     * Sets `packageFile` and `packageFileType` based on specified/defaulted package type, package dir, and package name. 
     * Validates the include and packageType values before setting the packageFile and packageFileType.
     * Also creates the package directory if it did not already exist.
     * 
     * @throws IOException
     * @throws MojoExecutionException
     */
    private void setAndCreatePackageFilePath() throws IOException, MojoExecutionException {
        setPackageFileType();

        File projectBuildDir = getPackageDirectory();
        createDir(projectBuildDir);
        String projectBuildName = getPackageName();
        packageFile = new File(projectBuildDir, projectBuildName + "." + packageFileType.getValue());
    }
    
    /**
     * Sets `packageFileType` for specified packageType and include values. If packageType is not specified, 
     * and include contains `runnable`, default to PackageFileType.JAR. Otherwise, default 
     * to PackageFileType.ZIP. If packageType is specified, and include contains `runnable`, 
     * then packageType must be `jar`.
     * 
     * @throws MojoExecutionException
     */
    private void setPackageFileType() throws MojoExecutionException {
        ArrayList<String> includeValues = parseInclude();
        if (packageType == null) {
            if (includeValues.contains("runnable")) {
                log.debug("Defaulting `packageType` to `jar` because the `include` value contains `runnable`.");
                packageFileType = PackageFileType.JAR;
            } else {
                log.debug("Defaulting `packageType` to `zip`.");
                packageFileType = PackageFileType.ZIP;
            }
        } else {
            PackageFileType packType = PackageFileType.getPackageFileType(packageType);
            if (packType != null) {
                // if include contains runnable, validate packageType
                if (includeValues.contains("runnable") && packType != PackageFileType.JAR) {
                    throw new MojoExecutionException("The `include` value `runnable` requires a `packageType` value of `jar`.");
                }
                packageFileType = packType;
            } else {
                log.info("The `packageType` value " + packageType + " is not supported. Defaulting to 'zip'.");
                packageFileType = PackageFileType.ZIP;
            }
        }
    }

    /**
     * Returns package name
     * 
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
     * Returns package directory
     * 
     * @return File representing specified package directory, or default ${project.build.directory} (target) if unspecified
     * @throws IOException
     */
    private File getPackageDirectory() throws IOException {
        if (packageDirectory != null && !packageDirectory.isEmpty()) {
            // done: check if path is relative or absolute, convert to canonical
            File dir = new File(packageDirectory);
            if (dir.isAbsolute()) {
                return dir.getCanonicalFile();
            } else { //relative path
                File packageDir = new File(project.getBuild().getDirectory(), packageDirectory);
                return packageDir.getCanonicalFile();
            }
        } else {
            File packageDir = new File(project.getBuild().getDirectory());
            return packageDir.getCanonicalFile();
        }
    }

    private void createDir(File dir) throws MojoExecutionException {
        if (!dir.exists() && !dir.mkdirs()) {
            throw new MojoExecutionException("Unable to create directory "+dir.getPath());
        }
    }



}
