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

import java.text.MessageFormat;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.openliberty.tools.ant.ServerTask;

/**
 * Dump diagnostic information from the server JVM.
 */
@Mojo(name = "java-dump")
public class JavaDumpServerMojo extends StartDebugMojoSupport {

    /**
     * Include heap dump information. 
     */
    @Parameter(property = "heapDump")
    private boolean heapDump;
    
    /**
     * Include system dump information. 
     */
    @Parameter(property = "systemDump")
    private boolean systemDump;

    @Override
    protected void doExecute() throws Exception {
        if (skip) {
            getLog().info("\nSkipping java-dump goal.\n");
            return;
        }
        if (isInstall) {
            installServerAssembly();
        } else {
            log.info(MessageFormat.format(messages.getString("info.install.type.preexisting"), ""));
            checkServerHomeExists();
            checkServerDirectoryExists();
        }

        ServerTask serverTask = initializeJava();
        copyConfigFiles();
        serverTask.setOperation("javadump");
        serverTask.setInclude(generateInclude());
        serverTask.execute();
    }
    
    private String generateInclude() {
        StringBuilder builder = new StringBuilder();
        
        if (heapDump) {
            builder.append("heap");
        } 
        if (systemDump) {
            if (builder.length() != 0) {
                builder.append(",");
            }
            builder.append("system");
        }
        
        return (builder.length() == 0) ? null : builder.toString();
    }
}
