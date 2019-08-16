/**
 * (C) Copyright IBM Corporation 2014, 2017.
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

import net.wasdev.wlp.ant.ServerTask;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Dump diagnostic information from the server into an archive.
  */
@Mojo(name = "dump-server")
public class DumpServerMojo extends StartDebugMojoSupport {

    /**
     * Location of the target archive file.
     */
    @Parameter(property = "archive")
    private File archive;

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
    
    /**
     * Include thread dump information. 
     */
    @Parameter(property = "threadDump")
    private boolean threadDump;

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

        ServerTask serverTask = initializeJava();
        copyConfigFiles();
        serverTask.setOperation("dump");
        serverTask.setArchive(archive);
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
        if (threadDump) {
            if (builder.length() != 0) {
                builder.append(",");
            }
            builder.append("thread");
        }
        
        return (builder.length() == 0) ? null : builder.toString();
    }
}
