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
package net.wasdev.wlp.maven.plugins.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import net.wasdev.wlp.ant.ServerTask;
import net.wasdev.wlp.maven.plugins.BasicSupport;

import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Start/Debug server support.
 */
public class StartDebugMojoSupport extends BasicSupport {
    
    private static final String HEADER = "# Generated by liberty-maven-plugin";

    /**
<<<<<<< HEAD
=======
     * Location of customized configuration directory
     */
    @Parameter(property = "configDirectory")
    protected File configDirectory;

    /**
     * Location of customized configuration file server.xml
     */
    @Parameter(property = "configFile", defaultValue = "${basedir}/src/test/resources/server.xml")
    protected File configFile;

    /**
>>>>>>> upstream/tools-integration
     * Location of bootstrap.properties file.
     */
    @Parameter(property = "bootstrapPropertiesFile", defaultValue = "${basedir}/src/test/resources/bootstrap.properties")
    protected File bootstrapPropertiesFile;

    @Parameter
    protected Map<String, String> bootstrapProperties;
    
    /**
     * Location of jvm.options file.
     */
    @Parameter(property = "jvmOptionsFile", defaultValue = "${basedir}/src/test/resources/jvm.options")
    protected File jvmOptionsFile;
    
    @Parameter
    protected List<String> jvmOptions;

    /**
     * Location of customized server environment file server.env
     */
    @Parameter(property = "serverEnv", defaultValue = "${basedir}/src/test/resources/server.env")
    protected File serverEnv;

    protected ServerTask initializeJava() throws Exception {
        ServerTask serverTask = (ServerTask) ant.createTask("antlib:net/wasdev/wlp/ant:server");
        if (serverTask == null) {
            throw new NullPointerException("server task not found");
        }
        serverTask.setInstallDir(installDirectory);
        serverTask.setServerName(serverName);
        serverTask.setUserDir(userDirectory);
        serverTask.setOutputDir(outputDirectory);
        return serverTask;
    }

    /**
     * @throws IOException
     * @throws FileNotFoundException
     */
    protected void copyConfigFiles() throws IOException {

        String serverXMLPath = null;
        String jvmOptionsPath = null;
        String bootStrapPropertiesPath = null;
        String serverEnvPath = null;
        
        if (configDirectory != null && configDirectory.exists()) {
            // copy configuration files from configuration directory to server directory if end-user set it
            Copy copydir = (Copy) ant.createTask("copy");
            FileSet fileset = new FileSet();
            fileset.setDir(configDirectory);
            copydir.addFileset(fileset);
            copydir.setTodir(serverDirectory);
            copydir.setOverwrite(true);
            copydir.execute();

            File configDirServerXML = new File(configDirectory, "server.xml");
            if (configDirServerXML.exists()) {
                serverXMLPath = configDirServerXML.getCanonicalPath();
            }
            
            File configDirJvmOptionsFile = new File(configDirectory, "jvm.options");
            if (configDirJvmOptionsFile.exists()) {
                jvmOptionsPath = configDirJvmOptionsFile.getCanonicalPath();
            }
            
            File configDirBootstrapFile = new File(configDirectory, "bootstrap.properties");
            if (configDirBootstrapFile.exists()) {
            	bootStrapPropertiesPath = configDirBootstrapFile.getCanonicalPath();
            }

            File configDirServerEnv = new File(configDirectory, "server.env");
            if (configDirServerEnv.exists()) {
                serverEnvPath = configDirServerEnv.getCanonicalPath();
            }
        } 

        // handle server.xml if not overwritten by server.xml from configDirectory
        if (serverXMLPath == null || serverXMLPath.isEmpty()) {
            // copy configuration file to server directory if end-user set it.
            if (configFile != null && configFile.exists()) {
                Copy copy = (Copy) ant.createTask("copy");
                copy.setFile(configFile);
                copy.setTofile(new File(serverDirectory, "server.xml"));
                copy.setOverwrite(true);
                copy.execute();
                serverXMLPath = configFile.getCanonicalPath();
            }
        }
        
        // handle jvm.options if not overwritten by jvm.options from configDirectory
        if (jvmOptionsPath == null || jvmOptionsPath.isEmpty()) {
            File optionsFile = new File(serverDirectory, "jvm.options");
            if (jvmOptions != null) {
                writeJvmOptions(optionsFile, jvmOptions);
                jvmOptionsPath = "inlined configuration";
            } else if (jvmOptionsFile != null && jvmOptionsFile.exists()) {
                Copy copy = (Copy) ant.createTask("copy");
                copy.setFile(jvmOptionsFile);
                copy.setTofile(optionsFile);
                copy.setOverwrite(true);
                copy.execute();
                jvmOptionsPath = jvmOptionsFile.getCanonicalPath();
            }
        }
        
        // handle bootstrap.properties if not overwritten by bootstrap.properties from configDirectory
        if (bootStrapPropertiesPath == null || bootStrapPropertiesPath.isEmpty()) {
            File bootstrapFile = new File(serverDirectory, "bootstrap.properties");
            if (bootstrapProperties != null) {
                writeBootstrapProperties(bootstrapFile, bootstrapProperties);
                bootStrapPropertiesPath = "inlined configuration";
            } else if (bootstrapPropertiesFile != null && bootstrapPropertiesFile.exists()) {
                Copy copy = (Copy) ant.createTask("copy");
                copy.setFile(bootstrapPropertiesFile);
                copy.setTofile(bootstrapFile);
                copy.setOverwrite(true);
                copy.execute();
                bootStrapPropertiesPath = bootstrapPropertiesFile.getCanonicalPath();
            }
        }
        
        // handle server.env if not overwritten by server.env from configDirectory
        if (serverEnvPath == null || serverEnvPath.isEmpty()) {
            if (serverEnv != null && serverEnv.exists()) {
                Copy copy = (Copy) ant.createTask("copy");
                copy.setFile(serverEnv);
                copy.setTofile(new File(serverDirectory, "server.env"));
                copy.setOverwrite(true);
                copy.execute();
                serverEnvPath = serverEnv.getCanonicalPath();
            }
        }
        
        // log info on the configuration files that get used
        if (serverXMLPath != null && !serverXMLPath.isEmpty()) {
            log.info(MessageFormat.format(messages.getString("info.server.start.update.config"), 
                "server.xml", serverXMLPath));
        }
        if (jvmOptionsPath != null && !jvmOptionsPath.isEmpty()) {
            log.info(MessageFormat.format(messages.getString("info.server.start.update.config"), 
                "jvm.options", jvmOptionsPath));
        }
        if (bootStrapPropertiesPath != null && !bootStrapPropertiesPath.isEmpty()) {
            log.info(MessageFormat.format(messages.getString("info.server.start.update.config"), 
                "bootstrap.properties", bootStrapPropertiesPath));
        }
        if (serverEnvPath != null && !serverEnvPath.isEmpty()) {
            log.info(MessageFormat.format(messages.getString("info.server.start.update.config"), 
                "server.env", serverEnvPath));
        }
    }
    
    private void writeBootstrapProperties(File file, Map<String, String> properties) throws IOException {
        makeParentDirectory(file);
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(file, "UTF-8"); 
            writer.println(HEADER);
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                writer.print(entry.getKey());
                writer.print("=");
                writer.println(entry.getValue().replace("\\", "/"));
            }
        } finally {
            if (writer != null) {
                writer.close(); 
            }
        }
    }
    
    private void writeJvmOptions(File file, List<String> options) throws IOException {
        makeParentDirectory(file);
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(file, "UTF-8"); 
            writer.println(HEADER);
            for (String option : options) {
                writer.println(option);
            }
        } finally {
            if (writer != null) {
                writer.close(); 
            }
        }
    }
    
    private void makeParentDirectory(File file) {        
        File parentDir = file.getParentFile();
        if (parentDir != null) {
            parentDir.mkdirs();
        }
    }

}
