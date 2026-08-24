/**
 * (C) Copyright IBM Corporation 2026.
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
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Commandline.Argument;

import io.openliberty.tools.common.plugins.util.InstallFeatureUtil;
import io.openliberty.tools.common.plugins.util.InstallFeatureUtil.ProductProperties;
import io.openliberty.tools.common.plugins.util.PluginExecutionException;

/**
 * Apply WebSphere Liberty iFix JAR files to the Liberty runtime installation.
 */
@Mojo(name = "apply-ifix", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class ApplyIfixMojo extends PluginConfigSupport {

    private static final String WAS_RUNTIME_GROUP_ID = "com.ibm.websphere.appserver.runtime";
    private static final String APPLIES_TO_ATTRIBUTE = "Applies-To";
    private static final Pattern PRODUCT_VERSION_PATTERN = Pattern.compile("productVersion=([^;\\s]+)");
    /** productId stored in lib/versions/*.properties for WebSphere Liberty */
    private static final String WAS_PRODUCT_ID = "com.ibm.websphere.appserver";

    /**
     * Whether to apply iFix JAR files to the Liberty runtime.
     * When set to {@code true}, the Mojo will look for iFix JAR files in
     * {@code libertyifixDir} and apply each one to the Liberty installation.
     * Defaults to {@code false} — no iFix processing is performed unless explicitly enabled.
     */
    @Parameter(property = "applyLibertyiFix", defaultValue = "false")
    private boolean applyLibertyiFix;

    /**
     * Directory containing the iFix JAR files to apply.
     * Each {@code *.jar} file in this directory is treated as an iFix archive and
     * executed with {@code java -jar <file> --installLocation <installDirectory>}.
     * Defaults to {@code ${project.basedir}/src/main/liberty/ifixes}.
     */
    @Parameter(property = "libertyifixDir", defaultValue = "${project.basedir}/src/main/liberty/ifixes")
    private File libertyifixDir;

    /**
     * Whether to fail the build when an iFix cannot be applied.
     * When {@code true} (the default), any error during iFix processing causes a
     * {@link MojoExecutionException} and stops the build.
     * When {@code false}, errors are reported as warnings and the build continues.
     */
    @Parameter(property = "stopOniFixApplyError", defaultValue = "true")
    private boolean stopOniFixApplyError;

    @Override
    public void execute() throws MojoExecutionException {
        if (!applyLibertyiFix) {
            getLog().debug("applyLibertyiFix is false. Skipping apply-ifix goal.");
            return;
        }

        init();

        if (skip) {
            getLog().info("\nSkipping apply-ifix goal.\n");
            return;
        }

        doApplyiFix();
    }

    private void doApplyiFix() throws MojoExecutionException {
        // Validate that runtimeArtifact is WebSphere Liberty
        if (assemblyArtifact == null) {
            handleError("The apply-ifix goal requires a runtimeArtifact to be specified. "
                    + "Direct installDirectory configuration is not supported. "
                    + "Please configure <runtimeArtifact> with groupId '"
                    + WAS_RUNTIME_GROUP_ID + "'.");
            return;
        }

        String groupId = assemblyArtifact.getGroupId();
        if (!WAS_RUNTIME_GROUP_ID.equals(groupId)) {
            handleError("The apply-ifix goal only supports WebSphere Liberty (groupId '"
                    + WAS_RUNTIME_GROUP_ID + "'). "
                    + "Found groupId: '" + groupId + "'. "
                    + "iFix application is not supported for this runtime.");
            return;
        }

        // Collect iFix JAR files
        if (!libertyifixDir.exists() || !libertyifixDir.isDirectory()) {
            handleError("The libertyifixDir '" + libertyifixDir.getAbsolutePath()
                    + "' does not exist or is not a directory. "
                    + "No iFix files can be applied.");
            return;
        }

        File[] ifixJars = libertyifixDir.listFiles(
                (dir, name) -> name.toLowerCase().endsWith(".jar"));

        if (ifixJars == null || ifixJars.length == 0) {
            handleError("No iFix JAR files found in '" + libertyifixDir.getAbsolutePath()
                    + "'. Set applyLibertyiFix=false if no iFix files are to be applied.");
            return;
        }

        // Determine the installed Liberty version
        String installedVersion = getInstalledLibertyVersion();
        if (installedVersion == null) {
            handleError("Unable to determine the installed WebSphere Liberty version from '"
                    + installDirectory.getAbsolutePath() + "'.");
            return;
        }

        getLog().info("Applying iFix files to WebSphere Liberty " + installedVersion
                + " at: " + installDirectory.getAbsolutePath());

        // Apply each iFix JAR
        for (File ifixJar : ifixJars) {
            applyIfix(ifixJar, installedVersion);
        }

        getLog().info("iFix application complete.");
    }

    /**
     * Read the installed Liberty product version from the lib/versions/*.properties
     * file under the install directory.
     * Returns the version for the WebSphere Liberty product entry (productId = {@code com.ibm.websphere.appserver}).
     */
    private String getInstalledLibertyVersion() throws MojoExecutionException {
        try {
            List<ProductProperties> propertiesList = InstallFeatureUtil.loadProperties(installDirectory);
            for (ProductProperties props : propertiesList) {
                if (WAS_PRODUCT_ID.equals(props.getId())) {
                    return props.getVersion();
                }
            }
        } catch (PluginExecutionException e) {
            throw new MojoExecutionException(
                    "Error reading Liberty product properties from '" + installDirectory.getAbsolutePath() + "': "
                            + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Read the Applies-To productVersion from the iFix JAR manifest and apply
     * the iFix if the version matches the installed Liberty version.
     */
    private void applyIfix(File ifixJar, String installedVersion) throws MojoExecutionException {
        getLog().info("Processing iFix: " + ifixJar.getName());

        // Read Applies-To from MANIFEST.MF
        String ifixVersion = readAppliestoVersion(ifixJar);
        if (ifixVersion == null) {
            handleError("Unable to read 'Applies-To' attribute from iFix JAR manifest: "
                    + ifixJar.getName() + ". Skipping this iFix.");
            return;
        }

        // Version check
        if (!installedVersion.equals(ifixVersion)) {
            handleError("iFix '" + ifixJar.getName() + "' targets Liberty version '" + ifixVersion
                    + "' but the installed version is '" + installedVersion + "'. "
                    + "This iFix cannot be applied.");
            return;
        }

        // Execute: java -jar <ifixJar> --installLocation <installDirectory>
        try {
            getLog().info("Applying iFix '" + ifixJar.getName() + "' to Liberty at '"
                    + installDirectory.getCanonicalPath() + "'...");

            Java applyIfixTask = (Java) ant.createTask("java");
            applyIfixTask.setJar(ifixJar);
            Argument args = applyIfixTask.createArg();
            args.setLine("--installLocation " + installDirectory.getCanonicalPath());
            applyIfixTask.setTimeout(300000L);
            applyIfixTask.setFork(true);
            int rc = applyIfixTask.executeJava();

            if (rc != 0) {
                handleError("iFix '" + ifixJar.getName() + "' failed with return code " + rc + ".");
            } else {
                getLog().info("Successfully applied iFix: " + ifixJar.getName());
            }
        } catch (IOException e) {
            handleError("Error applying iFix '" + ifixJar.getName() + "': " + e.getMessage());
        }
    }

    /**
     * Extract the productVersion from the iFix JAR's {@code Applies-To} manifest attribute.
     * Example: {@code Applies-To: io.openliberty; productVersion=25.0.0.12; productInstallType=Archive}
     *
     * @param ifixJar the iFix JAR file
     * @return the productVersion string, or {@code null} if not found
     */
    private String readAppliestoVersion(File ifixJar) {
        try (JarFile jarFile = new JarFile(ifixJar)) {
            Manifest manifest = jarFile.getManifest();
            if (manifest == null) {
                return null;
            }
            Attributes mainAttrs = manifest.getMainAttributes();
            String appliesTo = mainAttrs.getValue(APPLIES_TO_ATTRIBUTE);
            if (appliesTo == null || appliesTo.isEmpty()) {
                return null;
            }
            getLog().debug("Applies-To: " + appliesTo);
            Matcher m = PRODUCT_VERSION_PATTERN.matcher(appliesTo);
            if (m.find()) {
                return m.group(1).trim();
            }
        } catch (IOException e) {
            getLog().debug("Error reading manifest from iFix JAR '" + ifixJar.getName() + "': " + e.getMessage());
        }
        return null;
    }

    /**
     * Handle an error according to the {@code stopOniFixApplyError} setting.
     * If {@code true}, throws {@link MojoExecutionException}.
     * If {@code false}, logs a warning and returns normally.
     */
    private void handleError(String message) throws MojoExecutionException {
        if (stopOniFixApplyError) {
            throw new MojoExecutionException(message);
        } else {
            getLog().warn(message);
        }
    }
}
