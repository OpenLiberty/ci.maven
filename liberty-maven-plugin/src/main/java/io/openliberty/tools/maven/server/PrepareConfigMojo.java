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
import java.text.MessageFormat;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import io.openliberty.tools.common.plugins.util.PrepareConfigUtil;

/**
 * Prepare Liberty configuration and generate liberty-plugin-config.xml without
 * creating the server. This lightweight goal evaluates project configuration
 * and generates metadata needed by IDE tools and language servers.
 *
 * <p>
 * This goal is designed to be fast and non-invasive, making it suitable for
 * automatic execution when Liberty configuration files are opened in an IDE.
 * </p>
 *
 * <p>
 * The generated liberty-plugin-config.xml file contains:
 * </p>
 * <ul>
 * <li>Project structure information (directories, dependencies)</li>
 * <li>Server configuration file locations</li>
 * <li>Liberty installation paths (when Liberty is already installed)</li>
 * </ul>
 *
 * <p>
 * <b>Note:</b> This goal does NOT install Liberty or create a server. If you need
 * Liberty installed for full variable resolution in language servers, use the
 * liberty:create or liberty:dev goals first.
 * </p>
 *
 * <p>
 * <b>Usage Examples:</b>
 * </p>
 *
 * <pre>
 * {@code
 * <!-- Generate Liberty configuration -->
 * mvn liberty:prepare-config
 *
 * <!-- Use custom temporary directory -->
 * mvn liberty:prepare-config -DprepareConfigTempDir=my-temp
 *
 * <!-- For full language server features, install Liberty first -->
 * mvn liberty:create
 * mvn liberty:prepare-config
 * }
 * </pre>
 */
@Mojo(name = "prepare-config", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class PrepareConfigMojo extends PluginConfigSupport {

    /**
     * Name of the temporary directory used for mock Liberty server structures.
     * This directory is created under the build output directory (target/).
     * Default value is "tmp/liberty-var-cache".
     *
     * <p>Example: If set to "my-temp", the mock server will be created at:
     * target/my-temp/wlp/usr/servers/{serverName}</p>
     */
    @Parameter(property = "prepareConfigTempDir", defaultValue = "tmp/liberty-var-cache")
    private String prepareConfigTempDir;

    @Override
    public void execute() throws MojoExecutionException {
        // Set flag to skip server config setup in init() to avoid Liberty runtime download
        skipServerConfigSetup = true;
        init();

        if (skip) {
            getLog().info("\nSkipping prepare-config goal.\n");
            return;
        }

        // Set up minimal required fields that were skipped by skipServerConfigSetup
        initializeMinimalServerConfig();

        doPrepareConfig();
    }

    /**
     * Initialize minimal server configuration needed for prepare-config goal.
     * This sets up mock paths without downloading Liberty runtime.
     */
    private void initializeMinimalServerConfig() {
        // Set server name if not already set
        if (serverName == null) {
            serverName = "defaultServer";
        }

        // Validate and use the configured temp directory name
        String tempDirName = (prepareConfigTempDir != null && !prepareConfigTempDir.trim().isEmpty())
            ? prepareConfigTempDir.trim()
            : PrepareConfigUtil.DEFAULT_TEMP_DIR_NAME;

        // Set up mock Liberty directory structure
        File buildDirectory = new File(project.getBuild().getDirectory());
        installDirectory = PrepareConfigUtil.getMockInstallDirectory(buildDirectory, tempDirName);
        userDirectory = PrepareConfigUtil.getMockUserDirectory(buildDirectory, tempDirName);
        File serversDirectory = PrepareConfigUtil.getMockServersDirectory(buildDirectory, tempDirName);
        serverDirectory = new File(serversDirectory, serverName);
        outputDirectory = serversDirectory;

        getLog().debug("prepare-config: Using mock Liberty paths (no runtime download)");
        getLog().debug("  tempDirectory: " + tempDirName);
        getLog().debug("  installDirectory: " + installDirectory);
        getLog().debug("  serverDirectory: " + serverDirectory);
    }

    private void doPrepareConfig() throws MojoExecutionException {
        getLog().info("Preparing Liberty configuration...");

        try {
            // Validate and use the configured temp directory name
            String tempDirName = (prepareConfigTempDir != null && !prepareConfigTempDir.trim().isEmpty())
                ? prepareConfigTempDir.trim()
                : PrepareConfigUtil.DEFAULT_TEMP_DIR_NAME;

            // Create mock Liberty server structure using common utility
            File buildDirectory = new File(project.getBuild().getDirectory());
            File mockServerDir = PrepareConfigUtil.createMockLibertyServerStructure(buildDirectory, serverName, tempDirName);
            
            // Temporarily set serverDirectory to mock location for config file copying
            File originalServerDir = serverDirectory;
            try {
                serverDirectory = mockServerDir;
                
                // Use parent's copyConfigFiles which handles:
                // - configDirectory, serverXmlFile, jvmOptionsFile, bootstrapPropertiesFile, serverEnvFile overrides
                // - inline configurations, mergeServerEnv logic, Maven property resolution
                super.copyConfigFiles();
                
            } finally {
                // Restore original serverDirectory
                serverDirectory = originalServerDir;
            }
            
            // Generate liberty-plugin-config.xml with paths pointing to mock server
            // Always include server info (server.xml, bootstrap.properties, etc.)
            File configFile = exportParametersToXml(true);
            getLog().info(MessageFormat.format("Liberty configuration file generated: {0}",
                configFile.getAbsolutePath()));
            getLog().info(MessageFormat.format("Mock Liberty server structure created: {0}",
                mockServerDir.getAbsolutePath()));

        } catch (IOException | ParserConfigurationException | TransformerException e) {
            throw new MojoExecutionException("Error preparing Liberty configuration.", e);
        }
    }
    /**
     * Override to generate liberty-plugin-config.xml pointing to mock server structure.
     * All directories (installDirectory, userDirectory, serverDirectory, serverOutputDirectory)
     * are set to mock locations in target/{tempDirName}/wlp/usr/servers/{serverName}.
     * Server information is always included in the generated configuration.
     */
    @Override
    protected File exportParametersToXml(boolean includeServerInfo)
            throws IOException, ParserConfigurationException, TransformerException {
        // Validate and use the configured temp directory name
        String tempDirName = (prepareConfigTempDir != null && !prepareConfigTempDir.trim().isEmpty())
            ? prepareConfigTempDir.trim()
            : PrepareConfigUtil.DEFAULT_TEMP_DIR_NAME;

        // Build mock Liberty directory structure paths using common utility
        File buildDirectory = new File(project.getBuild().getDirectory());
        File mockInstallDir = PrepareConfigUtil.getMockInstallDirectory(buildDirectory, tempDirName);
        File mockUserDir = PrepareConfigUtil.getMockUserDirectory(buildDirectory, tempDirName);
        File mockServersDir = PrepareConfigUtil.getMockServersDirectory(buildDirectory, tempDirName);
        File mockServerDir = PrepareConfigUtil.getMockServerDirectory(buildDirectory, serverName, tempDirName);
        
        // Save original values to restore after XML generation
        File originalInstallDir = installDirectory;
        File originalUserDir = userDirectory;
        File originalServerDir = serverDirectory;
        File originalOutputDir = outputDirectory;
        
        try {
            // Override all directories to point to mock structure
            installDirectory = mockInstallDir;
            userDirectory = mockUserDir;
            serverDirectory = mockServerDir;
            // Parent creates serverOutputDirectory as new File(outputDirectory, serverName)
            // Set outputDirectory = mockServersDir to get mockServersDir/serverName = mockServerDir
            outputDirectory = mockServersDir;
            
            // Call parent implementation with mock directories, always including server info
            return super.exportParametersToXml(true);
            
        } finally {
            // Restore original values
            installDirectory = originalInstallDir;
            userDirectory = originalUserDir;
            serverDirectory = originalServerDir;
            outputDirectory = originalOutputDir;
        }
    }
}