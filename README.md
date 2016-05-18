# ci.maven [![Build Status](https://travis-ci.org/WASdev/ci.maven.svg?branch=master)](https://travis-ci.org/WASdev/ci.maven) [![Maven Central Latest](https://maven-badges.herokuapp.com/maven-central/net.wasdev.wlp.maven.plugins/liberty-maven-plugin/badge.svg)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22net.wasdev.wlp.maven.plugins%22%20AND%20a%3A%22liberty-maven-plugin%22)

Collection of Maven plugins and archetypes for managing WebSphere Application Server Liberty Profile and applications.

* [Build](#build)
* [Plugins](#plugins)
 * [liberty-maven-plugin](#liberty-maven-plugin)
     * [Configuration](#configuration)
     * [Goals](#goals)
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
                <version>1.2</version>
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

`liberty-maven-plugin` must first be configured with Liberty profile installation information. The installation information can be specified as an [existing installation directory](docs/installation-configuration.md#using-an-existing-installation), a [packaged server](docs/installation-configuration.md#using-a-packaged-server), or as a [Maven artifact](docs/installation-configuration.md#using-maven-artifact). The `liberty-maven-plugin` can also download and install Liberty profile server from the [Liberty repository](https://developer.ibm.com/wasdev/downloads/) or other location using the [install parameter](docs/installation-configuration.md#using-a-repository). 
By default, the plugin will install Liberty runtime with the Java EE 7 Web Profile features from the Liberty repository.

#### Goals

The `liberty-maven-plugin` provides the following goals.

| Goal | Description |
| --------- | ------------ |
| [install-server](docs/install-server.md#install-server) | Installs Liberty profile runtime. This goal is implicitly invoked by all the other plugin goals and usually does not need to be executed explicitly. | | [create-server](docs/create-server.md#create-server) | Create a Liberty Profile server. |
| [start-server](docs/start-server.md#start-server) | Start a Liberty Profile server in background. The server instance will be automatically created if it does not exist. |
| [run-server](docs/run-server.md#run-server) | Start a Liberty Profile server in foreground. The server instance will be automatically created if it does not exist. |
| [stop-server](docs/stop-server.md#stop-server) | Stop a Liberty Profile server. The server instance must exist and must be running. |
| [package-server](docs/package-server.md#package-server) | Package a Liberty Profile server. |
| [clean-server](docs/clean-server.md#clean-server) | Deletes every file in the `${outputDirectory}/logs`, `${outputDirectory}/workarea`, `${userDirectory}/dropins` or `${userDirectory}/apps`. |
| [dump-server](docs/dump-server.md#dump-server) | Dump diagnostic information from the server into an archive. |
| [java-dump-server](docs/java-dump-server.md#java-dump-server) | Dump diagnostic information from the server JVM. |
| [deploy](docs/deploy.md#deploy) | Deploy an application to a Liberty Profile server. The server instance must exist and must be running. |
| [undeploy](docs/undeploy.md#undeploy) | Undeploy an application to a Liberty Profile server. The server instance must exist and must be running. |
| [install-feature](docs/install-feature.md#install-feature) | Install a feature packaged as a Subsystem Archive (esa) to the Liberty runtime. |
| [uninstall-feature](docs/uninstall-feature.md#uninstall-feature) | Uninstall a feature from the Liberty runtime. |
| [install-apps](docs/install-apps.md#install-apps) | Copy applications specified as Maven compile dependencies to Liberty server's `dropins` or `apps` directory. |

##### Common Parameters

Parameters shared by all goals. [See common parameters](docs/common-parameters.md#common-parameters).

##### Common Server Parameters

Additional parameters shared by all server-based goals. [See common server parameters](docs/common-server-parameters.md#common-server-parameters).

## Packaging types

### liberty-assembly

The `liberty-assembly` Maven packaging type is used to create a packaged Liberty profile server Maven artifact out of existing server installation, compressed archive, or another server Maven artifact. Any applications specified as Maven compile dependencies will be automatically packaged with the assembled server. [Liberty features](docs/install-feature.md) can also be installed and packaged with the assembled server.

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
                <version>1.2</version>
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

`liberty-plugin-archetype` is used to generate a basic multi-module project that builds a simple web application, deploys and tests it on the Liberty profile server. It also creates a Liberty profile server package that includes the application.

#### Usage

    mvn archetype:generate \
        -DarchetypeGroupId=net.wasdev.wlp.maven \
        -DarchetypeArtifactId=liberty-plugin-archetype \
        -DwlpPluginVersion=1.2 \
        -DgroupId=test \
        -DartifactId=test \
        -Dversion=1.0-SNAPSHOT

