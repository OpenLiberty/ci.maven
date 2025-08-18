# ci.maven [![Maven Central Latest](https://maven-badges.herokuapp.com/maven-central/io.openliberty.tools/liberty-maven-plugin/badge.svg)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22io.openliberty.tools%22%20AND%20a%3A%22liberty-maven-plugin%22) [![Build Status](https://github.com/OpenLiberty/ci.maven/actions/workflows/maven.yml/badge.svg?branch=main)](https://github.com/OpenLiberty/ci.maven/actions?branch=main)

The Liberty Maven plugin supports install and operational control of Liberty runtime and servers. Use it to manage your application on Liberty for integration test and to create Liberty server packages.

* [Build](#build)
* [Plugins](#plugins)
	* [Liberty Maven Plugin](#liberty-maven-plugin)
		* [Java support](#java-support)
		* [Release 3.0 differences](#release-30-differences)
		* [Configuration](#configuration)
		* [Goals](#goals)
* [Packaging types](#packaging-types)
	* [liberty-assembly](#liberty-assembly)
* [Getting started](#getting-started)

## Usage - TLDR

* `mvn liberty:dev` : All-in-one goal: installs Liberty, features, starts server and deploys app. Runs in the background while you develop your application. Applies code and configuration changes (and optionally runs tests) for immediate feedback
* `mvn liberty:help` : List Liberty plugin goals
* `mvn liberty:help -Ddetail=true -Dgoal=dev` : Detailed information on parameters for goal, e.g. 'dev'

## Build

As of version 3.9 of the plugin, you must use Maven 3.8.6 or later to build the Liberty Maven plugin.
We conveniently provide the [maven-wrapper](https://maven.apache.org/wrapper/maven-wrapper-plugin/index.html) script, so you do not need to download Maven yourself if you are not using it yet. 

* `./mvnw install` : builds the plugin, skipping all tests
* `./mvnw install -Poffline-its -DlibertyInstallDir=<liberty_install_directory>` : builds the plugin and runs the integration tests by providing an existing installation.
* `./mvnw install -Ponline-its -Druntime=<ol|wlp> -DruntimeVersion=<runtime_version>` : builds the plugin and runs the integration tests by downloading a new server. Set runtime to `ol` to run tests using the Open Liberty runtime, or `wlp` to run tests using the WebSphere Liberty Java EE 7 runtime.

## Plugins

### Liberty Maven Plugin

The Liberty Maven Plugin provides a number of goals for managing a Liberty server and applications. As of version 3.9 of the plugin, Maven 3.8.6 or later is required to use the Liberty Maven Plugin. 

#### Java Support

The Liberty Maven Plugin is tested with Long-Term-Support (LTS) releases of Java. The plugin, as of release 3.10, supports Java 8, 11, 17 and 21. Versions 3.7 to 3.9.x support Java 8, 11 and 17. Prior to version 3.7, the plugin is supported on Java 8 and 11.

#### Release 3.0 differences

The new capabilities and behavior differences are summarized in the [Liberty Maven Plug-in 3.0](https://github.com/OpenLiberty/ci.maven/releases/tag/liberty-maven-3.0/) release notes.

#### Configuration

To enable Liberty Maven Plugin in your project add the following to your `pom.xml`:

```xml
<project>
    ...
    <build>
        <plugins>
            <!-- Enable liberty-maven-plugin -->
            <plugin>
                <groupId>io.openliberty.tools</groupId>
                <artifactId>liberty-maven-plugin</artifactId>
                <version>3.11.4</version>
                <!-- Specify configuration, executions for liberty-maven-plugin -->
                ...
            </plugin>
        </plugins>
    </build>
    ...
</project>
```

If you are using a snapshot version of Liberty Maven Plugin then you will also need to add the following plugin repository to your `pom.xml`:

```
<project>
    ...
    <pluginRepositories>
        <!-- Configure Sonatype OSS Maven snapshots repository -->
        <pluginRepository>
            <id>central-portal-snapshots</id>
            <name>Central Portal Snapshots</name>
            <url>https://central.sonatype.com/repository/maven-snapshots/</url>
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

The Liberty Maven Plugin must first be configured with the Liberty server installation information. The installation information can be specified as:

* A [Maven artifact](docs/installation-configuration.md#using-maven-artifact)
* An [existing installation directory](docs/installation-configuration.md#using-an-existing-installation)
* A [packaged server](docs/installation-configuration.md#using-a-packaged-server)

Installing from a Maven artifact is the default installation method. The default runtime artifact is the latest version of `io.openliberty:openliberty-kernel`. In order to configure WebSphere Liberty for installation, specify the `runtimeArtifact` with the `com.ibm.websphere.appserver.runtime` groupId and the specific `artifactId` and `version` that is needed. For a full list of artifacts available, see the [Liberty installation configuration](docs/installation-configuration.md#using-maven-artifact) documentation. 

Example using the `runtimeArtifact` parameter to install a WebSphere Liberty runtime from a Maven artifact:

```xml
<plugin>
    <groupId>io.openliberty.tools</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <version>3.11.4</version>
    <configuration>
        <runtimeArtifact>
            <groupId>com.ibm.websphere.appserver.runtime</groupId>
            <artifactId>wlp-webProfile8</artifactId>
            <version>25.0.0.6</version>
            <type>zip</type>
        </runtimeArtifact>
    </configuration>
</plugin>
```

To install an Open Liberty beta runtime, specify the `runtimeArtifact` with the `io.openliberty.beta` groupId, `open liberty-runtime` artifactId and the `version` that is needed.

Example using the `runtimeArtifact` parameter to install an Open Liberty beta runtime from a Maven artifact:

```xml
<plugin>
    <groupId>io.openliberty.tools</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <version>3.11.4</version>
    <configuration>
        <runtimeArtifact>
            <groupId>io.openliberty.beta</groupId>
            <artifactId>openliberty-runtime</artifactId>
            <version>25.0.0.7-beta</version>
            <type>zip</type>
        </runtimeArtifact>
    </configuration>
</plugin>
```

The Liberty Maven Plugin can also download and install a Liberty server from the [Liberty repository](https://developer.ibm.com/wasdev/downloads/) or other location using the [install parameter](docs/installation-configuration.md#using-a-repository).

#### Goals

The Liberty Maven Plugin provides the following goals.

| Goal | Description |
| --------- | ------------ |
| [clean](docs/clean.md#clean) | Deletes every file in the `${outputDirectory}/logs`, `${outputDirectory}/workarea`, `${userDirectory}/dropins` or `${userDirectory}/apps`. |
| [compile-jsp](docs/compile-jsp.md#compile-jsps) | Compile JSPs in the src/main/webapp into the target/classes directory |
| [create](docs/create.md#create) | Create a Liberty server. |
| [debug](docs/debug.md#debug) | Start a Liberty server in debug mode. |
| [deploy](docs/deploy.md#deploy) | Copy applications to the Liberty server's dropins or apps directory. If the server instance is running, it will also verify the applications started successfully. |
| [dev](docs/dev.md#dev) | Start a Liberty server in dev mode.* |
| [devc](docs/dev.md#devc-container-mode) | Start a Liberty server in dev mode in a container.* |
| [display-url](docs/display-url.md#display-url) | Display the application URL in the default browser. |
| [dump](docs/dump.md#dump) | Dump diagnostic information from the server into an archive. |
| [generate-features](docs/generate-features.md#generate-feature) | Scan the class files of an application and create a Liberty configuration file in the source configuration directory that contains the Liberty features the application requires.* |
| [install-feature](docs/install-feature.md#install-feature) | Install a feature packaged as a Subsystem Archive (esa) to the Liberty runtime. |
| [install-server](docs/install-server.md#install-server) | Installs the Liberty runtime. This goal is implicitly invoked by all the other plugin goals and usually does not need to be executed explicitly. |
| [java-dump](docs/java-dump.md#java-dump) | Dump diagnostic information from the server JVM. |
| [package](docs/package.md#package) | Package a Liberty server. |
| [prepare-feature](docs/prepare-feature.md#prepare-feature) | Prepare a user feature for installation to the Liberty runtime. |
| [run](docs/run.md#run) | Start a Liberty server in the foreground. The run goal implicitly creates the server, installs features referenced by the server.xml file, and deploys the application before starting the Liberty server. |
| [start](docs/start.md#start) | Start a Liberty server in the background. The server instance will be automatically created if it does not exist. |
| status | Check a Liberty server status. |
| [stop](docs/stop.md#stop) | Stop a Liberty server. The server instance must exist and must be running. |
| [test-start](docs/test-start.md/#test-start) | Allows you to bypass automatically starting the server during the pre-integration-test phase with pom configuration or a Liberty-specific command line argument. |
| [test-stop](docs/test-stop.md#test-stop) | Allows you to bypass automatically stopping the server during the post-integration-test phase with pom configuration or a Liberty-specific command line argument. |
| [undeploy](docs/undeploy.md#undeploy) | Undeploy an application to a Liberty server. The server instance must exist and must be running. |
| [uninstall-feature](docs/uninstall-feature.md#uninstall-feature) | Uninstall a feature from the Liberty runtime. |

*The `dev`, `devc`, and `generate-features` goals have a runtime dependency on IBM WebSphere Application Server Migration Toolkit for Application Binaries, which is separately licensed under IBM License Agreement for Non-Warranted Programs. For more information, see the [license](https://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/wasdev/license/wamt).
Note:  The `dev` and `devc` goals have this dependency only when auto-generation of features is turned on. By default, auto-generation of features is turned off.

##### Common Parameters

Parameters shared by all goals. [See common parameters](docs/common-parameters.md#common-parameters).

##### Common Server Parameters

Additional parameters shared by all server-based goals. [See common server parameters](docs/common-server-parameters.md#common-server-parameters).

#### Extensions

Extensions improve the compatibility or user experience of third party libraries used with Liberty. The Liberty Maven Plugin provides the following extensions.

| Extension | Description |
| --------- | ------------ |
| [configure-arquillian goal](docs/configure-arquillian.md#configure-arquillian) | Integrates `arquillian.xml` configuration for the Liberty Managed and Remote Arquillian containers in the Liberty Maven Plugin. Automatically configures required `arquillian.xml` parameters for the Liberty Managed container. |
| [Spring Boot Support](docs/spring-boot-support.md#spring-boot-support) | The Liberty Maven Plugin provides support for Spring Boot applications, allowing you to install Spring Boot executable JARs directly to the Liberty runtime. |

## Packaging types

### liberty-assembly

The `liberty-assembly` Maven packaging type is used to create a packaged Liberty server Maven artifact out of existing server installation, compressed archive, or another server Maven artifact. Any applications specified as Maven compile dependencies will be automatically packaged with the assembled server. [Liberty features](docs/install-feature.md) can also be installed and packaged with the assembled server. Any application or test code included in the project is automatically compiled and tests run at appropriate unit or integration test phase. Application code is installed as a loose application WAR file if `deployPackages` is set to `all` or `project` and `looseApplication` is set to `true`.

The `liberty-assembly` default lifecycle includes:

| Phase | Goal |
| ----- | ---- |
| pre-clean | liberty:stop |
| process-resources | maven-resources-plugin:resources |
| compile | maven-compiler-plugin:compile |
| process-test-resources | maven-resources-plugin:testResources |
| test-compile | maven-compiler-plugin:testCompile |
| test | maven-surefire-plugin:test |
| prepare-package | liberty:create, liberty:prepare-feature, liberty:install-feature |
| package | liberty:deploy, liberty:package|
| pre-integration-test | liberty:test-start|
| integration-test | maven-failsafe-plugin:integration-test |
| post-integration-test | liberty:test-stop|
| verify | maven-failsafe-plugin:verify |
| install | maven-install-plugin:install |
| deploy | maven-deploy-plugin:deploy |

Example:
```xml
<project>
    ...
    <groupId>myGroup</groupId>
    <artifactId>myServer</artifactId>
    <!-- Create Liberty server assembly -->
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
                <groupId>io.openliberty.tools</groupId>
                <artifactId>liberty-maven-plugin</artifactId>
                <version>3.11.4</version>
                <extensions>true</extensions>
                <configuration>
                    <installDirectory>/opt/ibm/wlp</installDirectory>
                    <serverName>test</serverName>
                    <features>
                        <acceptLicense>true</acceptLicense>
                        <feature>mongodb-2.0</feature>
                    </features>
                    <looseApplication>true</looseApplication>
                    <deployPackages>all</deployPackages>
                </configuration>
            </plugin>
        </plugins>
    </build>
    ...
</project>
```
## Getting started

There are multiple starters available to generate a package to start developing your first application on Open Liberty.

* [Open Liberty starter](https://openliberty.io/start/)
* [Eclipse Starter for Jakarta EE](https://start.jakarta.ee/) - choose `Open Liberty` for the `Runtime`
* [MicroProfile Starter](https://start.microprofile.io/) - choose `Open Liberty` for the `MicroProfile Runtime`

If you want to use one of the previously published archetypes that we are no longer enhancing, refer to this [documentation](docs/archetypes.md).
