# ci.maven [![Maven Central Latest](https://maven-badges.herokuapp.com/maven-central/io.openliberty.tools/liberty-maven-plugin/badge.svg)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22io.openliberty.tools%22%20AND%20a%3A%22liberty-maven-plugin%22) [![Build Status](https://github.com/OpenLiberty/ci.maven/actions/workflows/maven.yml/badge.svg?branch=main)](https://github.com/OpenLiberty/ci.maven/actions?branch=main) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/3882b05e21b14d85bfe2f7bfe6abef13)](https://www.codacy.com/app/wasdevb1/ci.maven?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=WASdev/ci.maven&amp;utm_campaign=Badge_Grade)

Collection of Maven plugins and archetypes for managing Open Liberty and WebSphere Liberty servers and applications.

* [Build](#build)
* [Plugins](#plugins)
	* [Liberty Maven Plugin](#liberty-maven-plugin)
		* [Release 3.0 differences](#release-30-differences)
		* [Configuration](#configuration)
		* [Goals](#goals)
* [Packaging types](#packaging-types)
	* [liberty-assembly](#liberty-assembly)
* [Archetypes](#archetypes)
	* [liberty-plugin-archetype](#liberty-plugin-archetype)
	* [liberty-archetype-webapp](#liberty-archetype-webapp)
	* [liberty-archetype-ear](#liberty-archetype-ear)

## Build

Use Maven 3.5.0 or later to build the Liberty plugins and archetypes.

* `mvn install` : builds the plugin and the archetypes.
* `mvn install -Poffline-its -DlibertyInstallDir=<liberty_install_directory>` : builds the plugin and the archetypes and runs the integration tests by providing an existing installation.
* `mvn install -Ponline-its -Druntime=<ol|wlp> -DruntimeVersion=<runtime_version>` : builds the plugin and archetypes and runs the integration tests by downloading a new server. Set runtime to `ol` to run tests using the Open Liberty runtime, or `wlp` to run tests using the WebSphere Liberty Java EE 7 runtime.

## Plugins

### Liberty Maven Plugin

The Liberty Maven Plugin provides a number of goals for managing a Liberty server and applications. Maven 3.5.0 or later is recommended to use the Liberty Maven Plugin. 

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
                <version>[3.3.4,)</version>
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

The Liberty Maven Plugin must first be configured with the Liberty server installation information. The installation information can be specified as an [existing installation directory](docs/installation-configuration.md#using-an-existing-installation), a [packaged server](docs/installation-configuration.md#using-a-packaged-server), or as a [Maven artifact](docs/installation-configuration.md#using-maven-artifact). The Liberty Maven Plugin can also download and install a Liberty server from the [Liberty repository](https://developer.ibm.com/wasdev/downloads/) or other location using the [install parameter](docs/installation-configuration.md#using-a-repository).
By default, the plugin installs the Open Liberty runtime from Maven Central. 

#### Goals

The Liberty Maven Plugin provides the following goals.

| Goal | Description |
| --------- | ------------ |
| [clean](docs/clean.md#clean) | Deletes every file in the `${outputDirectory}/logs`, `${outputDirectory}/workarea`, `${userDirectory}/dropins` or `${userDirectory}/apps`. |
| [compile-jsp](docs/compile-jsp.md#compile-jsps) | Compile JSPs in the src/main/webapp into the target/classes directory |
| [create](docs/create.md#create) | Create a Liberty server. |
| [debug](docs/debug.md#debug) | Start a Liberty server in debug mode. |
| [deploy](docs/deploy.md#deploy) | Copy applications to the Liberty server's dropins or apps directory. If the server instance is running, it will also verify the applications started successfully. |
| [dev](docs/dev.md#dev) | Start a Liberty server in dev mode. |
| [devc](docs/dev.md#devc-container-mode) | Start a Liberty server in dev mode in a container. |
| [display-url](docs/display-url.md#display-url) | Display the application URL in the default browser. |
| [dump](docs/dump.md#dump) | Dump diagnostic information from the server into an archive. |
| [install-feature](docs/install-feature.md#install-feature) | Install a feature packaged as a Subsystem Archive (esa) to the Liberty runtime. |
| [install-server](docs/install-server.md#install-server) | Installs the Liberty runtime. This goal is implicitly invoked by all the other plugin goals and usually does not need to be executed explicitly. |
| [java-dump](docs/java-dump.md#java-dump) | Dump diagnostic information from the server JVM. |
| [package](docs/package.md#package) | Package a Liberty server. |
| [run](docs/run.md#run) | Start a Liberty server in the foreground. The run goal implicitly creates the server, installs features referenced by the server.xml file, and deploys the application before starting the Liberty server. |
| [start](docs/start.md#start) | Start a Liberty server in the background. The server instance will be automatically created if it does not exist. |
| status | Check a Liberty server status. |
| [stop](docs/stop.md#stop) | Stop a Liberty server. The server instance must exist and must be running. |
| [test-start](docs/test-start.md/#test-start) | Allows you to bypass automatically starting the server during the pre-integration-test phase with pom configuration or a Liberty-specific command line argument. |
| [test-stop](docs/test-stop.md#test-stop) | Allows you to bypass automatically stopping the server during the post-integration-test phase with pom configuration or a Liberty-specific command line argument. |
| [undeploy](docs/undeploy.md#undeploy) | Undeploy an application to a Liberty server. The server instance must exist and must be running. |
| [uninstall-feature](docs/uninstall-feature.md#uninstall-feature) | Uninstall a feature from the Liberty runtime. |


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
| prepare-package | liberty:create, liberty:install-feature |
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
                <version>[3.2.3,)</version>
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
## Archetypes

By default, all archetypes that specify a Liberty runtime use the latest version of the Open Liberty runtime. You can use a different runtime by setting the `runtimeGroupId` and `runtimeArtifactId`. For example, you can use `wlp-webProfile7` by setting `-DruntimeGroupId=com.ibm.websphere.appserver.runtime` and `-DruntimeArtifactId=wlp-webProfile7`. 

The runtime version can also be set dynamically. For example, you can specify version `20.0.0.3` of the runtime by setting `-DruntimeVersion=20.0.0.3`.

Finally, the default Liberty Maven Plugin version is set to be the latest version of the plugin. To specify a different version of the plugin, use the `libertyPluginVersion` parameter. For example, you could set `-DlibertyPluginVersion=3.2`.

### liberty-plugin-archetype

The `liberty-plugin-archetype` is used to generate a basic multi-module project that builds a simple web application then deploys and tests it on a Liberty server. It also creates a Liberty server package that includes the application.

#### Usage

    mvn archetype:generate \
        -DarchetypeGroupId=io.openliberty.tools \
        -DarchetypeArtifactId=liberty-plugin-archetype \
        -DarchetypeVersion=3.2.3  \
        -DlibertyPluginVersion=3.2.3  \
        -DgroupId=test \
        -DartifactId=test \
        -Dversion=1.0-SNAPSHOT

### liberty-archetype-webapp

The `liberty-archetype-webapp` is used to generate a basic single-module project that builds a simple web application then deploys and tests on a Liberty server. It also creates a minified, runnable Liberty server package that includes the application. The generated project includes the [`liberty-maven-app-parent`](docs/parent-pom.md) parent pom that binds Liberty Maven Plugin goals to the Maven default build lifecycle.

#### Usage

    mvn archetype:generate \
        -DarchetypeGroupId=io.openliberty.tools \
        -DarchetypeArtifactId=liberty-archetype-webapp \
        -DarchetypeVersion=3.2.3 \
        -DgroupId=test \
        -DartifactId=test \
        -Dversion=1.0-SNAPSHOT

### liberty-archetype-ear

The `liberty-archetype-ear` is used to generate a multi-module project that includes an EJB module, a web application module and an EAR module. In the EAR module, it packages the application in a Java EE 7 Enterprise Archive then deploys and tests on a Liberty server. It also creates a minified, runnable Liberty server package that includes the application EAR file. The generated project includes [`liberty-maven-app-parent`](docs/parent-pom.md) parent pom that binds Liberty Maven Plugin goals to the Maven default build lifecycle.

#### Usage

    mvn archetype:generate \
        -DarchetypeGroupId=io.openliberty.tools \
        -DarchetypeArtifactId=liberty-archetype-ear \
        -DarchetypeVersion=3.2.3  \
        -DgroupId=test \
        -DartifactId=test \
        -Dversion=1.0-SNAPSHOT

### Using Archetype Snapshots

If you are using a snapshot version of `liberty-archetype-webapp` or `liberty-archetype-ear`, then you will also need to add the following archetype repository to `${user.home}/.m2/settings.xml`:

``` xml
<settings>
    ...
    <profiles>
        <profile>
            <id>archetype-snapshot-repo</id>
            <properties>
                <archetypeRepository>https://oss.sonatype.org/content/repositories/snapshots
                </archetypeRepository>
            </properties>
            <repositories>
                <repository>
                    <id>sonatype-nexus-snapshots</id>
                    <name>Sonatype Nexus Snapshots</name>
                    <url>https://oss.sonatype.org/content/repositories/snapshots/
                    </url>
                    <releases>
                        <enabled>false</enabled>
                    </releases>
                    <snapshots>
                        <enabled>true</enabled>
                    </snapshots>
                </repository>
            </repositories>
            <pluginRepositories>
                <pluginRepository>
                    <id>sonatype-nexus-snapshots</id>
                    <name>Sonatype Nexus Snapshots</name>
                    <url>https://oss.sonatype.org/content/repositories/snapshots/
                    </url>
                    <releases>
                        <enabled>false</enabled>
                    </releases>
                    <snapshots>
                        <enabled>true</enabled>
                    </snapshots>
                </pluginRepository>
            </pluginRepositories>
        </profile>
    </profiles>
    <activeProfiles>
        <activeProfile>archetype-snapshot-repo</activeProfile>
    </activeProfiles>
</settings>

```
