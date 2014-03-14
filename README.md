ci.maven
========

Collection of Maven plugins and archetypes for managing WebSphere Application Server Liberty Profile and applications.

* [Build](#build)
* [Plugins](#plugins)
 * [liberty-maven-plugin](#liberty-maven-plugin)
     * [Configuration](#configuration)
     * [Goals](#goals)
       * [create-server](#create-server)
       * [start-server](#start-server)
       * [stop-server](#stop-server)
       * [package-server](#package-server)
       * [dump-server](#dump-server)
       * [java-dump-server](#java-dump-server)
       * [deploy](#deploy)
       * [undeploy](#undeploy)
* [Packaging types](#packaging-types)
 * [liberty-assembly](#liberty-assembly)
* [Archetypes](#archetypes)
 * [liberty-plugin-archetype](#liberty-plugin-archetype)

## Build

Use Maven 2.x or 3.x to build the Liberty plugins and archetypes. 

* `mvn install` : builds the plugin and the archetype. 
* `mvn install -DwlpInstallDir=<liberty_install_directory>` : builds the plugin, archetype and runs the integration tests.
  * Liberty Profile installation is required to run the integration tests.

## Plugins

### liberty-maven-plugin

`liberty-maven-plugin` provides a number of goals for managing Liberty Profile server and applications.

#### Configuration

To enable `liberty-maven-plugin` in your project add the following to your `pom.xml`:

    <project>
        ...
        <build>
            <plugins>
                <!-- Enable liberty-maven-plugin -->
                <plugin>
                    <groupId>net.wasdev.wlp.maven.plugins</groupId>
                    <artifactId>liberty-maven-plugin</artifactId> 
                    <version>1.0-SNAPSHOT</version>
                    <!-- Specify configuration, executions for liberty-maven-plugin --> 
                    ...             
                </plugin>
            </plugins>
        </build>
        ...
    </project>

If you are using a snapshot version of `liberty-maven-plugin` then you will also need to add the following plugin repository to your `pom.xml`:

    <project>
        ...
        <pluginRepositories>
            <!-- Configure Sonatype OSS Maven snapshots repository -->
            <pluginRepository>
                <id>sonatype-nexus-snapshots</id>
                <name>Sonatype Nexus Snapshots</name>
                <url>https://oss.sonatype.org/content/repositories/snapshots/</url>
                <snapshots>
                    <enabled>true</enabled>
                </snapshots>
                <releases>
                    <enabled>false</enabled>
                </releases>
            </pluginRepository>
        </pluginRepositories>
        ...
    </project>

`liberty-maven-plugin` must first be configured with Liberty Profile installation information. The installation information can be specified as an existing installation directory, a compressed archive, or as a Maven artifact. 

* Use the `serverHome` parameter to specify the directory of an existing Liberty Profile server installation. For example:

        <plugin>
            <groupId>net.wasdev.wlp.maven.plugins</groupId>
            <artifactId>liberty-maven-plugin</artifactId> 
            <configuration>
                <serverHome>/opt/ibm/wlp</serverHome>
            </configuration>
        </plugin>

* Use the `assemblyArchive` parameter to specify a compressed archive that contains Liberty Profile server files. For example:

        <plugin>
            <groupId>net.wasdev.wlp.maven.plugins</groupId>
            <artifactId>liberty-maven-plugin</artifactId> 
            <configuration>
                <assemblyArchive>/opt/ibm/wlp.zip</assemblyArchive>
            </configuration>
        </plugin>

* Use the `assemblyArtifact` parameter to specify the name of the Maven artifact that contains Liberty Profile server files. For example:

        <plugin>
            <groupId>net.wasdev.wlp.maven.plugins</groupId>
            <artifactId>liberty-maven-plugin</artifactId> 
            <configuration>
                <assemblyArtifact> 
                    <groupId>net.wasdev.wlp.test</groupId> 
                    <artifactId>liberty-test-server</artifactId>
                    <version>1.0</version>
                    <type>zip</type>
                </assemblyArtifact>         
            </configuration>
        </plugin>

#### Goals

##### Common Parameters

Parameters shared by all goals.

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| serverHome | Directory location of the Liberty profile server installation. | Yes, only when `assemblyArchive` and `assemblyArtifact` parameters are not set. |
| assemblyArchive | Location of the Liberty profile server compressed archive. The archive will be unpacked into a directory as specified by the `installDirectory` parameter. | Yes, only when `serverHome` and `assemblyArtifact` parameters are not set. |
| assemblyArtifact | Maven artifact name of the Liberty profile server assembly. The assembly will be installed into a directory as specified by the `installDirectory` parameter. | Yes, only when `serverHome` and `assemblyArchive` parameters are not set. |
| serverName | Name of the Liberty profile server instance. The default value is `defaultServer`. | No |
| userDirectory | Alternative user directory location that contains server definitions and shared resources (`WLP_USER_DIR`). | No |
| outputDirectory | Alternative location for server generated output such as logs, the _workarea_ directory, and other generated files (`WLP_OUTPUT_DIR`). | No | 
| installDirectory | Local installation directory location of the Liberty profile server when the server is installed using the assembly archive or artifact option. The default value is `${project.build.directory}/liberty`.  | No |
| refresh | If true, re-install Liberty profile server into the local directory. This is only used when when the server is installed using the assembly archive or artifact option. The default value is false. | No |

##### Common Server Parameters

Additional parameters shared by all server-based goals.

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| configFile | Location of a server configuration file to be used by the instance. The default value is `${basedir}/src/test/resources/server.xml`. | No |
| bootstrapPropertiesFile | Location of a bootstrap properties file to be used by the instance. The default value is `${basedir}/src/test/resources/bootstrap.properties`. | No |
| jvmOptionsFile | Location of a JVM options file to be used by the instance. The default value is `${basedir}/src/test/resources/jvm.options`. | No |
| serverEnv | Location of a server environment file to be used by the instance. The default value is `${basedir}/src/test/resources/server.env` | No |

##### start-server
---
Start a Liberty Profile server. The server instance will be automatically created if it does not exist.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common server parameters](#common-server-parameters) and the [common parameters](#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| clean | Clean all cached information on server start up. The default value is `false`. | No | 
| serverStartTimeout | Maximum time to wait (in seconds) to verify that the server has started. The default value is 30 seconds. | No |
| verifyTimeout | Maximum time to wait (in seconds) to verify that the applications have started. This timeout only has effect if the `applications` parameter is set. The default value is 30 seconds. | No |
| applications | A comma-separated list of application names to wait for during server start-up. | No |

Example:

    <plugin>
        <groupId>net.wasdev.wlp.maven.plugins</groupId>
        <artifactId>liberty-maven-plugin</artifactId> 
        <executions>
            ...
            <execution>
                <id>start-server</id>
                <phase>pre-integration-test</phase>
                <goals>
                    <goal>start-server</goal>
                </goals>
                <configuration>
                    <verifyTimeout>60</verifyTimeout>       
                    <configFile>${project.build.testOutputDirectory}/wlp/server.xml</configFile>     
                </configuration>
            </execution>
            ...
        <executions>
        <configuration>
           <serverHome>/opt/ibm/wlp</serverHome>
           <serverName>test</serverName>
        </configuration>              
    </plugin>

##### stop-server
---
Stop a Liberty Profile server. The server instance must exist and must be running.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common server parameters](#common-server-parameters) and the [common parameters](#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| serverStopTimeout | Maximum time to wait (in seconds) to verify that the server has stopped. The default value is 30 seconds. | No |

Example:

    <plugin>
        <groupId>net.wasdev.wlp.maven.plugins</groupId>
        <artifactId>liberty-maven-plugin</artifactId> 
        <executions>
            ...
            <execution>
                <id>stop-server</id>
                <phase>post-integration-test</phase>
                <goals>
                    <goal>stop-server</goal>
                </goals>
                <configuration>
                    <serverStopTimeout>60</serverStopTimeout>            
                </configuration>
            </execution>
            ...
        <executions>
        <configuration>
           <serverHome>/opt/ibm/wlp</serverHome>
           <serverName>test</serverName>
        </configuration>              
    </plugin>

##### create-server
---
Create a Liberty Profile server.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common server parameters](#common-server-parameters) and the [common parameters](#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| template | Name of the template to use when creating a new server. | No |

Example:

    <plugin>
        <groupId>net.wasdev.wlp.maven.plugins</groupId>
        <artifactId>liberty-maven-plugin</artifactId> 
        <executions>
            ...
            <execution>
                <id>create-server</id>
                <phase>pre-integration-test</phase>
                <goals>
                    <goal>create-server</goal>
                </goals>
            </execution>
            ...
        <executions>
        <configuration>
           <serverHome>/opt/ibm/wlp</serverHome>
           <serverName>test</serverName>
        </configuration>              
    </plugin>

##### package-server
---
Package a Liberty Profile server.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common server parameters](#common-server-parameters) and the [common parameters](#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| packageFile | Location of the target file or directory. If the target location is a file, the contents of the server instance will be compressed into the specified file. If the target location is a directory, the contents of the server instance will be compressed into `${packageFile}/${serverName}.zip` file. If the target location is not specified, it defaults to `${serverHome}/usr/servers/${serverName}.zip` if `serverHome` is set. Otherwise, it defaults to `${installDirectory}/usr/servers/${serverName}.zip` if `assemblyArchive` or `assemblyArtifact` is set. | No |
| include | Packaging type. One of `all`, `usr`, or `minify`. The default value is `all`. | No |

Example:

    <plugin>
        <groupId>net.wasdev.wlp.maven.plugins</groupId>
        <artifactId>liberty-maven-plugin</artifactId> 
        <executions>
            ...
            <execution>
                <id>package-server</id>
                <phase>package</phase>
                <goals>
                    <goal>package-server</goal>
                </goals>
                <configuration>
                    <packageFile>${project.build.directory}/test.zip</packageFile>                        
                </configuration>
            </execution>
            ...
        <executions>
        <configuration>
           <serverHome>/opt/ibm/wlp</serverHome>
           <serverName>test</serverName>
        </configuration>              
    </plugin>

##### dump-server
---
Dump diagnostic information from the server into an archive.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common server parameters](#common-server-parameters) and the[common parameters](#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| archive | Location of the target archive file. | No |
| systemDump | Include system dump information. The default value is `false`. | No |
| heapDump | Include heap dump information. The default value is `false`. | No |
| threadDump | Include thread dump information. The default value is `false`. | No |

Example:

    <plugin>
        <groupId>net.wasdev.wlp.maven.plugins</groupId>
        <artifactId>liberty-maven-plugin</artifactId> 
        <executions>
            ...
            <execution>
                <id>dump-server</id>
                <phase>post-integration-test</phase>
                <goals>
                    <goal>dump-server</goal>
                </goals>
                <configuration>
                    <archive>${project.build.directory}/dump.zip</archive> 
                    <heapDump>true</heapDump>                       
                </configuration>
            </execution>
            ...
        <executions>
        <configuration>
           <serverHome>/opt/ibm/wlp</serverHome>
           <serverName>test</serverName>
        </configuration>              
    </plugin>

##### java-dump-server
---
Dump diagnostic information from the server JVM.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common server parameters](#common-server-parameters) and the [common parameters](#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| systemDump | Include system dump information. The default value is `false`. | No |
| heapDump | Include heap dump information. The default value is `false`. | No |

Example:

    <plugin>
        <groupId>net.wasdev.wlp.maven.plugins</groupId>
        <artifactId>liberty-maven-plugin</artifactId> 
        <executions>
            ...
            <execution>
                <id>java-dump-server</id>
                <phase>post-integration-test</phase>
                <goals>
                    <goal>java-dump-server</goal>
                </goals>
                <configuration>
                    <heapDump>true</heapDump>
                    <systemDump>true</systemDump>                       
                </configuration>
            </execution>
            ...
        <executions>
        <configuration>
           <serverHome>/opt/ibm/wlp</serverHome>
           <serverName>test</serverName>
        </configuration>              
    </plugin>

##### deploy
---
Deploy an application to a Liberty Profile server. The server instance must exist and must be running.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common parameters](#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| appArchive | Location of an application file to be deployed. The application type can be war, ear, rar, eba, zip, or jar. | Yes |
| timeout | Maximum time to wait (in milliseconds) to verify that the deployment has completed successfully. The default value is 40 seconds. | No |

Example:

    <plugin>
        <groupId>net.wasdev.wlp.maven.plugins</groupId>
        <artifactId>liberty-maven-plugin</artifactId> 
        <executions>
            ...
            <execution>
                <id>deploy-app</id>
                <phase>post-integration-test</phase>
                <goals>
                    <goal>deploy</goal>
                </goals>
                <configuration>
                    <appArchive>HelloWorld.war</appArchive>                        
                </configuration>
            </execution>
            ...
        <executions>
        <configuration>
           <serverHome>/opt/ibm/wlp</serverHome>
           <serverName>test</serverName>
        </configuration>              
    </plugin>

##### undeploy
---
Undeploy an application to a Liberty Profile server. The server instance must exist and must be running.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common parameters](#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| appArchive | Name of an application to be undeployed. The application type can be war, ear, rar, eba, zip, or jar. | Yes |
| timeout | Maximum time to wait (in milliseconds) to verify that the undeployment has completed successfully. The default value is 40 seconds. | No |

Example:

    <plugin>
        <groupId>net.wasdev.wlp.maven.plugins</groupId>
        <artifactId>liberty-maven-plugin</artifactId> 
        <executions>
            ...
            <execution>
                <id>undeploy-app</id>
                <phase>post-integration-test</phase>
                <goals>
                    <goal>undeploy</goal>
                </goals>
                <configuration>
                    <appArchive>HelloWorld.war</appArchive>                        
                </configuration>
            </execution>
            ...
        <executions>
        <configuration>
           <serverHome>/opt/ibm/wlp</serverHome>
           <serverName>test</serverName>
        </configuration>              
    </plugin>

## Packaging types

### liberty-assembly

The `liberty-assembly` Maven packaging type is used to create a packaged Liberty profile server Maven artifact out of existing server installation, compressed archive, or another server Maven artifact. Any applications specified as Maven compile dependencies will be automatically packaged with the assembled server in the _dropins/_ directory.

Example:

    <project>
        ...
        <groupId>myGroup</groupId>
        <artifactId>myServer</artifactId>
        <!-- Create Liberty profile server assembly -->
        <packaging>liberty-assembly</packaging>
        ...
        <dependencies>
            <!-- Package SimpleServlet.war with server assembly -->
            <dependency>
                <groupId>wasdev</groupId>
                <artifactId>SimpleServlet</artifactId>
                <version>1.0</version>
                <type>war</type>
            </dependency>
        </dependencies>
        ...
        <build>
            <plugins>
                <!-- Enable liberty-maven-plugin -->
                <plugin>
                    <groupId>net.wasdev.wlp.maven.plugins</groupId>
                    <artifactId>liberty-maven-plugin</artifactId> 
                    <version>1.0-SNAPSHOT</version>
                    <extensions>true</extensions>
                    <configuration>
                        <serverHome>/opt/ibm/wlp</serverHome>
                        <serverName>test</serverName>
                    </configuration>         
                </plugin>
            </plugins>
        </build>
        ...
    </project>

## Archetypes

### liberty-plugin-archetype

`liberty-plugin-archetype` is used to generate a basic multi-module project that builds a simple web application, deploys and tests it on the Liberty Profile server. It also creates a Liberty Profile server package that includes the application.

#### Usage

    mvn archetype:generate \
        -DarchetypeGroupId=net.wasdev.wlp.maven \
        -DarchetypeArtifactId=liberty-plugin-archetype \
        -DgroupId=test \
        -DartifactId=test \
        -Dversion=1.0-SNAPSHOT \
        -DwlpInstallDir=<liberty_install_directory>

