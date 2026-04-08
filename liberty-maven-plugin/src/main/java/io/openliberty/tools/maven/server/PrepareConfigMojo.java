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
            // Generate the liberty-plugin-config.xml file
            File configFile = exportParametersToXml(includeServerInfo);
            getLog().info(MessageFormat.format("Liberty configuration file generated: {0}",
                configFile.getAbsolutePath()));

        } catch (IOException | ParserConfigurationException | TransformerException e) {
            throw new MojoExecutionException("Error preparing Liberty configuration.", e);
        }
    }

    /**
     * Override to prevent server creation during config preparation.
     * This goal only generates the config file, it does not install Liberty or create a server.
     */
    @Override
    protected void installServerAssembly() throws MojoExecutionException {
        // Only export parameters, don't install Liberty or create server
        try {
            exportParametersToXml(false);
            // Skip the actual Liberty installation
        } catch (IOException | ParserConfigurationException | TransformerException e) {
            throw new MojoExecutionException("Error exporting parameters.", e);
        }
    }
}