/**
 * (C) Copyright IBM Corporation 2014, 2020.
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

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import io.openliberty.tools.ant.ServerTask;

/**
 * Start a liberty server
 */
@Mojo(name = "run", requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class RunServerMojo extends PluginConfigSupport {

    /**
     * Clean all cached information on server start up.
     */
    @Parameter(property = "clean", defaultValue = "false")
    protected boolean clean;
    
    /**
     * Run the server in embedded mode
     */
    @Parameter(property = "embedded", defaultValue = "false")
    private boolean embedded;

    @Override
    protected void doExecute() throws Exception {
        if (skip) {
            getLog().info("\nSkipping run goal.\n");
            return;
        }
        String projectPackaging = project.getPackaging();

        runMojo("org.apache.maven.plugins", "maven-compiler-plugin", "compile");

        if(projectPackaging.equals("ear")) {
            runMojo("org.apache.maven.plugins", "maven-ear-plugin", "generate-application-xml");
        }

        runMojo("org.apache.maven.plugins", "maven-resources-plugin", "resources");
        
        if(!looseApplication) {
            switch (projectPackaging) {
                case "war":
                    runMojo("org.apache.maven.plugins", "maven-war-plugin", "war");
                    break;
                case "ear":
                    runMojo("org.apache.maven.plugins", "maven-ear-plugin", "ear");
                    break;
            }
        }
        
        runLibertyMojoCreate();
        runLibertyMojoInstallFeature(null, null);
        runLibertyMojoDeploy(false);

        ServerTask serverTask = initializeJava();
        copyConfigFiles();
        serverTask.setUseEmbeddedServer(embedded);
        serverTask.setClean(clean);
        serverTask.setOperation("run");       
        serverTask.execute();
    }

}
