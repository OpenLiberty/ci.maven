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
 * <li>Server configuration file locations (when includeServerInfo=true)</li>
 * <li>Liberty installation paths (when Liberty is already installed)</li>
 * </ul>
 * 
 * <p>
 * <b>Parameters:</b>
 * </p>
 * <ul>
 * <li><b>includeServerInfo</b> (default: true) - Include server-specific
 * configuration file paths in the generated config. Set to false for faster
 * execution when only basic project information is needed.</li>
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
 * <!-- Generate config with server info (default) -->
 * mvn liberty:prepare-config
 * 
 * <!-- Skip server info for faster execution -->
 * mvn liberty:prepare-config -DincludeServerInfo=false
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
     * Whether to include server-specific information in the generated config.
     * When true, includes server.xml, bootstrap.properties, jvm.options, etc.
     * When false, only includes project and build metadata.
     */
    @Parameter(property = "includeServerInfo", defaultValue = "true")
    private boolean includeServerInfo;

    @Override
    public void execute() throws MojoExecutionException {
        init();

        if (skip) {
            getLog().info("\nSkipping prepare-config goal.\n");
            return;
        }

        doPrepareConfig();
    }

    private void doPrepareConfig() throws MojoExecutionException {
        getLog().info("Preparing Liberty configuration...");

        try {
            // Create mock Liberty server structure using common utility
            File buildDirectory = new File(project.getBuild().getDirectory());
            File mockServerDir = PrepareConfigUtil.createMockLibertyServerStructure(buildDirectory, serverName);
            
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
            File configFile = exportParametersToXml(includeServerInfo);
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
     * are set to mock locations in target/tmp/wlp/usr/servers/{serverName}.
     */
    @Override
    protected File exportParametersToXml(boolean includeServerInfo)
            throws IOException, ParserConfigurationException, TransformerException {
        // Build mock Liberty directory structure paths using common utility
        File buildDirectory = new File(project.getBuild().getDirectory());
        File mockInstallDir = PrepareConfigUtil.getMockInstallDirectory(buildDirectory);
        File mockUserDir = PrepareConfigUtil.getMockUserDirectory(buildDirectory);
        File mockServersDir = PrepareConfigUtil.getMockServersDirectory(buildDirectory);
        File mockServerDir = PrepareConfigUtil.getMockServerDirectory(buildDirectory, serverName);
        
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
            
            // Call parent implementation with mock directories
            return super.exportParametersToXml(includeServerInfo);
            
        } finally {
            // Restore original values
            installDirectory = originalInstallDir;
            userDirectory = originalUserDir;
            serverDirectory = originalServerDir;
            outputDirectory = originalOutputDir;
        }
    }
}