# ci.maven [![Build Status](https://travis-ci.org/WASdev/ci.maven.svg?branch=master)](https://travis-ci.org/WASdev/ci.maven)

Collection of Maven plugins and archetypes for managing WebSphere Application Server Liberty Profile and applications.

* [Build](#build)
* [Plugins](#plugins)
 * [liberty-maven-plugin](#liberty-maven-plugin)
     * [Configuration](#configuration)
     * [Goals](#goals)
       * [create-server](#create-server)
       * [start-server](#start-server)
       * [run-server](#run-server)
       * [stop-server](#stop-server)
       * [package-server](#package-server)
       * [dump-server](#dump-server)
       * [java-dump-server](#java-dump-server)
       * [deploy](#deploy)
       * [undeploy](#undeploy)
       * [install-feature](#install-feature)
       * [uninstall-feature](#uninstall-feature)
* [Packaging types](#packaging-types)
 * [liberty-assembly](#liberty-assembly)
* [Archetypes](#archetypes)
 * [liberty-plugin-archetype](#liberty-plugin-archetype)

## Build

Use Maven 2.x or 3.x to build the Liberty plugins and archetypes.

* `mvn install` : builds the plugin and the archetype.
* `mvn install -Poffline-its -DwlpInstallDir=<liberty_install_directory>` : builds the plugin, archetype and runs the integration tests by providing an existing installation.
* `mvn install -Ponline-its -DwlpVersion=<liberty_version> -DwlpLicense=<liberty_license_code>` : builds the plugin, archetype and runs the integration tests by downloading a new server.
  * Liberty versions and their respective link to the license code can be found in the [index.yml](http://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/wasdev/downloads/wlp/index.yml) file. You can obtain the license code by reading the current license and looking for the D/N: <license code> line.

## Plugins

### liberty-maven-plugin

`liberty-maven-plugin` provides a number of goals for managing Liberty Profile server and applications.

#### Configuration

To enable `liberty-maven-plugin` in your project add the following to your `pom.xml`:

```xml
<project>
    ...
    <build>
        <plugins>
            <!-- Enable liberty-maven-plugin -->
            <plugin>
                <groupId>net.wasdev.wlp.maven.plugins</groupId>
                <artifactId>liberty-maven-plugin</artifactId>
                <version>1.0</version>
                <!-- Specify configuration, executions for liberty-maven-plugin -->
                ...
            </plugin>
        </plugins>
    </build>
    ...
</project>
```

If you are using a snapshot version of `liberty-maven-plugin` then you will also need to add the following plugin repository to your `pom.xml`:

```xml
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
```

##### Liberty installation configuration

`liberty-maven-plugin` must first be configured with Liberty profile installation information. The installation information can be specified as an existing installation directory, a packaged server, or as a Maven artifact. The `liberty-maven-plugin` can also download and install Liberty profile server from the [Liberty repository](https://developer.ibm.com/wasdev/downloads/) or other location.

###### Using an existing installation

Use the `installDirectory` parameter to specify the directory of an existing Liberty profile server installation. For example:
```xml
<plugin>
    <groupId>net.wasdev.wlp.maven.plugins</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <configuration>
        <installDirectory>/opt/ibm/wlp</installDirectory>
    </configuration>
</plugin>
```

###### Using a packaged server

Use the `assemblyArchive` parameter to specify a packaged server archive (created using `server package` command) that contains Liberty profile server files. For example:
```xml
<plugin>
    <groupId>net.wasdev.wlp.maven.plugins</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <configuration>
        <assemblyArchive>/opt/ibm/wlp.zip</assemblyArchive>
    </configuration>
</plugin>
```

###### Using Maven artifact

Use the `assemblyArtifact` parameter to specify the name of the Maven artifact that contains Liberty profile server files. For example:
```xml
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
```

###### Using a repository

Use the `install` parameter to download and install Liberty profile server from the [Liberty repository](https://developer.ibm.com/wasdev/downloads/) or other location.

The Liberty license code must always be specified in order to install the Liberty server. If you are installing Liberty from the Liberty repository, you can obtain the license code by reading the [current license](http://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/wasdev/downloads/wlp/8.5.5.5/lafiles/runtime/en.html) and looking for the `D/N: <license code>` line. Otherwise, download the Liberty runtime archive and execute `java -jar wlp*runtime.jar --viewLicenseInfo` command and look for the `D/N: <license code>` line.

* Install using the Liberty repository. The plugin will use the Liberty repository to find the Liberty runtime archive to install based on the given version.
 ```xml
    <plugin>
        <groupId>net.wasdev.wlp.maven.plugins</groupId>
        <artifactId>liberty-maven-plugin</artifactId>
        <configuration>
            <install>
                <licenseCode><license code></licenseCode>
            </install>
        </configuration>
    </plugin>
 ```

* Install from a given location. The `runtimeUrl` sub-parameter specifies a location of the Liberty runtime archive file to install.
 ```xml
    <plugin>
        <groupId>net.wasdev.wlp.maven.plugins</groupId>
        <artifactId>liberty-maven-plugin</artifactId>
        <configuration>
            <install>
                <licenseCode><license code></licenseCode>
                <runtimeUrl><url to runtime.jar></runtimeUrl>
            </install>
        </configuration>
    </plugin>
 ```

The `install` parameter has the following sub-parameters:

| Name | Description | Required |
| --------  | ----------- | -------  |
| licenseCode | Liberty profile license code. See [above](#install-from-repository). | Yes |
| version | Exact or wildcard version of the Liberty profile server to install. Available versions are listed in the [index.yml](http://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/wasdev/downloads/wlp/index.yml) file. Only used if `runtimeUrl` is not set. The default value is `8.5.+`. | No |
| runtimeUrl | URL to the Liberty profile's `wlp*runtime.jar`. If not set, the Liberty repository will be used to find the Liberty runtime archive. | No |
| cacheDirectory | The directory used for caching downloaded files such as the license or `.jar` files. The default value is `${settings.localRepository}/wlp-cache`. | No |
| username | Username needed for basic authentication. | No |
| password | Password needed for basic authentication. | No |
| serverId | Id of the `server` definition with the username and password in the `~/.m2/settings.xml` file. Used for basic authentication. | No |
| maxDownloadTime | Maximum time in seconds the download can take. The default value is `0` (no maximum time). | No |

---

#### Goals

##### Common Parameters

Parameters shared by all goals.

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| installDirectory | Local installation directory of the Liberty profile server. | Yes, only when `assemblyArchive` and `assemblyArtifact` parameters are not set. |
| assemblyArchive | Location of the Liberty profile server compressed archive. The archive will be unpacked into a directory as specified by the `assemblyInstallDirectory` parameter. | Yes, only when `installDirectory` and `assemblyArtifact` parameters are not set. |
| assemblyArtifact | Maven artifact name of the Liberty profile server assembly. The assembly will be installed into a directory as specified by the `assemblyInstallDirectory` parameter. | Yes, only when `installDirectory` and `assemblyArchive` parameters are not set. |
| serverName | Name of the Liberty profile server instance. The default value is `defaultServer`. | No |
| userDirectory | Alternative user directory location that contains server definitions and shared resources (`WLP_USER_DIR`). | No |
| outputDirectory | Alternative location for server generated output such as logs, the _workarea_ directory, and other generated files (`WLP_OUTPUT_DIR`). | No |
| assemblyInstallDirectory | Local installation directory location of the Liberty profile server when the server is installed using the assembly archive, assembly artifact or repository option. The default value is `${project.build.directory}/liberty`.  | No |
| refresh | If true, re-install Liberty profile server into the local directory. This is only used when when the server is installed using the assembly archive or artifact option. The default value is false. | No |
| skip | If true, the specified goal is bypassed entirely. The default value is false. | No |

##### Common Server Parameters

Additional parameters shared by all server-based goals.

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| configFile | Location of a server configuration file to be used by the instance. The default value is `${basedir}/src/test/resources/server.xml`. | No |
| bootstrapProperties | List of bootstrap properties for the server instance. | No |
| bootstrapPropertiesFile | Location of a bootstrap properties file to be used by the instance. The default value is `${basedir}/src/test/resources/bootstrap.properties`. | No |
| jvmOptions | List of JVM options for the server instance. | No |
| jvmOptionsFile | Location of a JVM options file to be used by the instance. The default value is `${basedir}/src/test/resources/jvm.options`. | No |
| serverEnv | Location of a server environment file to be used by the instance. The default value is `${basedir}/src/test/resources/server.env` | No |

Example:
```xml
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
                <configFile>${project.build.testOutputDirectory}/wlp/server.xml</configFile>
                <bootstrapProperties>
                    <httpPort>8080</httpPort>
                </bootstrapProperties>
                <jvmOptions>
                    <param>-Xmx768m</param>
                </jvmOptions>
            </configuration>
        </execution>
        ...
    </executions>
</plugin>
```

##### start-server
---
Start a Liberty Profile server in background. The server instance will be automatically created if it does not exist.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common server parameters](#common-server-parameters) and the [common parameters](#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| clean | Clean all cached information on server start up. The default value is `false`. | No |
| serverStartTimeout | Maximum time to wait (in seconds) to verify that the server has started. The default value is 30 seconds. | No |
| verifyTimeout | Maximum time to wait (in seconds) to verify that the applications have started. This timeout only has effect if the `applications` parameter is set. The default value is 30 seconds. | No |
| applications | A comma-separated list of application names to wait for during server start-up. | No |

Example:
```xml
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
    </executions>
    <configuration>
       <installDirectory>/opt/ibm/wlp</installDirectory>
       <serverName>test</serverName>
    </configuration>
</plugin>
```

##### run-server
---
Start a Liberty Profile server in foreground. The server instance will be automatically created if it does not exist.
**Note:** This goal is designed to be executed directly from the Maven command line.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common server parameters](#common-server-parameters) and the [common parameters](#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| clean | Clean all cached information on server start up. The default value is `false`. | No |

Example:
```xml
<plugin>
    <groupId>net.wasdev.wlp.maven.plugins</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <configuration>
       <installDirectory>/opt/ibm/wlp</installDirectory>
       <serverName>test</serverName>
    </configuration>
</plugin>
```

```bash
$ mvn liberty:run-server
```

##### stop-server
---
Stop a Liberty Profile server. The server instance must exist and must be running.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common server parameters](#common-server-parameters) and the [common parameters](#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| serverStopTimeout | Maximum time to wait (in seconds) to verify that the server has stopped. The default value is 30 seconds. | No |

Example:
```xml
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
    </executions>
    <configuration>
       <installDirectory>/opt/ibm/wlp</installDirectory>
       <serverName>test</serverName>
    </configuration>
</plugin>
```

##### create-server
---
Create a Liberty Profile server.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common server parameters](#common-server-parameters) and the [common parameters](#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| template | Name of the template to use when creating a new server. | No |

Example:
```xml
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
    </executions>
    <configuration>
       <installDirectory>/opt/ibm/wlp</installDirectory>
       <serverName>test</serverName>
    </configuration>
</plugin>
```

##### package-server
---
Package a Liberty Profile server.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common server parameters](#common-server-parameters) and the [common parameters](#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| packageFile | Location of the target file or directory. If the target location is a file, the contents of the server instance will be compressed into the specified file. If the target location is a directory, the contents of the server instance will be compressed into `${packageFile}/${serverName}.zip` file. If the target location is not specified, it defaults to `${installDirectory}/usr/servers/${serverName}.zip` if `installDirectory` is set. Otherwise, it defaults to `${assemblyInstallDirectory}/usr/servers/${serverName}.zip` if `assemblyArchive` or `assemblyArtifact` is set. | No |
| include | Packaging type. One of `all`, `usr`, or `minify`. The default value is `all`. | Yes, only when os parameter is set |
| os | A comma-delimited list of operating systems that you want the packaged server to support. To specify that an operating system is not to be supported, prefix it with a minus sign ("-"). The 'include' attribute must be set to 'minify'. | No |

Example:
```xml
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
                <include>minify</include>
                <os>OS/400,-z/OS</os>
            </configuration>
        </execution>
        ...
    </executions>
    <configuration>
       <installDirectory>/opt/ibm/wlp</installDirectory>
       <serverName>test</serverName>
    </configuration>
</plugin>
```

##### dump-server
---
Dump diagnostic information from the server into an archive.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common server parameters](#common-server-parameters) and the [common parameters](#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| archive | Location of the target archive file. | No |
| systemDump | Include system dump information. The default value is `false`. | No |
| heapDump | Include heap dump information. The default value is `false`. | No |
| threadDump | Include thread dump information. The default value is `false`. | No |

Example:
```xml
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
    </executions>
    <configuration>
       <installDirectory>/opt/ibm/wlp</installDirectory>
       <serverName>test</serverName>
    </configuration>
</plugin>
```

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
```xml
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
    </executions>
    <configuration>
       <installDirectory>/opt/ibm/wlp</installDirectory>
       <serverName>test</serverName>
    </configuration>
</plugin>
```

##### deploy
---
Deploy an application to a Liberty Profile server. The server instance must exist and must be running.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common parameters](#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| appArtifact | Maven coordinates of an application to be deployed. | Yes, if appArchive is not set. |
| appArchive | Location of an application file to be deployed. The application type can be war, ear, rar, eba, zip, or jar. | Yes, if appArtifact is not set. |
| timeout | Maximum time to wait (in seconds) to verify that the deployment has completed successfully. The default value is 40 seconds. | No |

Examples:

 1. Single deploy of an application with the path of a file.

  ```xml
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
   ```

 2. Single deploy of an application with maven coordinates.

  ```xml
    <execution>
        <id>deploy-by-appArtifact</id>
        <phase>post-integration-test</phase>
        <goals>
            <goal>deploy</goal>
        </goals>
        <configuration>
            <appArtifact>
                <groupId>com.mycompany.webapp</groupId>
                <artifactId>webapp</artifactId>
                <version>1.0</version>
                <type>war</type>
            </appArtifact>
        </configuration>
    </execution>
  ```

##### undeploy
---
Undeploy an application to a Liberty Profile server. The server instance must exist and must be running. If appArtifact or appArchive are not defined, the goal will undeploy all applications from the server.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common parameters](#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| appArtifact | Maven coordinates of an application to be undeployed. | No |
| appArchive | Name of an application to be undeployed. The application type can be war, ear, rar, eba, zip, or jar. | No |
| patternSet | Includes and excludes patterns of applications to be undeployed. | No |
| timeout | Maximum time to wait (in seconds) to verify that the undeployment has completed successfully. The default value is 40 seconds. | No |

Examples:

 1. Single undeploy from an application file.
  ```xml
    <execution>
        <id>undeploy-by-appArchive</id>
        <phase>post-integration-test</phase>
        <goals>
            <goal>undeploy</goal>
        </goals>
        <configuration>
            <appArchive>HelloWorld.war</appArchive>
        </configuration>
    </execution>
  ```

 2. Single undeploy from an application with maven coordinates.
   ```xml
    <execution>
        <id>undeploy-by-appArtifact</id>
        <phase>post-integration-test</phase>
        <goals>
            <goal>undeploy</goal>
        </goals>
        <configuration>
            <appArtifact>
                <groupId>com.mycompany.webapp</groupId>
                <artifactId>webapp</artifactId>
                <version>1.0</version>
                <type>war</type>
            </appArtifact>
        </configuration>
    </execution>
  ```
 3. Undeploy all.
  ```xml
    <execution>
        <id>undeploy-by-appArtifact</id>
        <phase>post-integration-test</phase>
        <goals>
            <goal>undeploy</goal>
        </goals>
    </execution>
  ```

 4. Undeploy from a patternSet.
  ```xml
    <execution>
        <id>undeploy-by-appArtifact</id>
        <phase>post-integration-test</phase>
        <goals>
            <goal>undeploy</goal>
        </goals>
        <configuration>
            <patternSet>
                <includes>*.war</includes>
                <excludes>webapp.war</excludes>
            </patternSet>
        </configuration>
    </execution>
  ```

##### install-feature
---
Install a feature packaged as a Subsystem Archive (esa) to the Liberty runtime.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common parameters](#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| feature | Specify the location of the Subsystem archive to be used. This can be an esa file, an IBM-Shortname or a Subsystem-SymbolicName of the Subsystem archive. The value can be a file name or a URL to the esa file. | Yes |
| acceptLicense | Automatically indicate acceptance of license terms and conditions. | No |
| to | Specify where to install the feature. The feature can be installed to any configured product extension location, or as a user feature (usr, extension). If this option is not specified the feature will be installed as a user feature. | No |
| whenFileExists |  If a file that is part of the esa already exists on the system, you must specify what actions to take. Valid options are: `fail` - abort the installation; `ignore` - continue the installation and ignore the file that exists; `replace` - overwrite the existing file. | No |

Example:
```xml
<plugin>
    <groupId>net.wasdev.wlp.maven.plugins</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <executions>
        ...
        <execution>
            <id>install-feature</id>
            <phase>pre-integration-test</phase>
            <goals>
                <goal>install-feature</goal>
            </goals>
            <configuration>
                <features>
                    <acceptLicense>true</acceptLicense>
                    <feature>mongodb-2.0</feature>
                    <feature>ejbLite-3.2</feature>
                </features>
            </configuration>
        </execution>
        ...
    </executions>
    <configuration>
       <installDirectory>/opt/ibm/wlp</installDirectory>
       <serverName>test</serverName>
    </configuration>
</plugin>
```

##### uninstall-feature
---
Uninstall a feature from the Liberty runtime.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common parameters](#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| feature | Specify the feature name to be uninstalled. This can be an IBM-Shortname or a Subsystem-SymbolicName of the Subsystem archive. | Yes |

Example:
```xml
<plugin>
    <groupId>net.wasdev.wlp.maven.plugins</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <executions>
        ...
        <execution>
            <id>install-feature</id>
            <phase>post-integration-test</phase>
            <goals>
                <goal>uninstall-feature</goal>
            </goals>
            <configuration>
                <features>
                    <feature>mongodb-2.0</feature>
                </features>
            </configuration>
        </execution>
        ...
    </executions>
    <configuration>
       <installDirectory>/opt/ibm/wlp</installDirectory>
       <serverName>test</serverName>
    </configuration>
</plugin>
```

## Packaging types

### liberty-assembly

The `liberty-assembly` Maven packaging type is used to create a packaged Liberty profile server Maven artifact out of existing server installation, compressed archive, or another server Maven artifact. Any applications specified as Maven compile dependencies will be automatically packaged with the assembled server. [Liberty features](#install-feature) can also be installed and packaged with the assembled server.

Example:
```xml
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
                <version>1.0</version>
                <extensions>true</extensions>
                <configuration>
                    <installDirectory>/opt/ibm/wlp</installDirectory>
                    <serverName>test</serverName>
                    <features>
                        <acceptLicense>true</acceptLicense>
                        <feature>mongodb-2.0</feature>
                    </features>
                </configuration>
            </plugin>
        </plugins>
    </build>
    ...
</project>
```
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

