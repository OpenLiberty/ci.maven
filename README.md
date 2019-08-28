# ci.maven [![Maven Central Latest](https://maven-badges.herokuapp.com/maven-central/net.wasdev.wlp.maven.plugins/liberty-maven-plugin/badge.svg)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22io.openliberty.tools%22%20AND%20a%3A%22liberty-maven-plugin%22) [![Build Status](https://travis-ci.com/OpenLiberty/ci.maven.svg?branch=master)](https://travis-ci.com/OpenLiberty/ci.maven) [![Build status](https://ci.appveyor.com/api/projects/status/vket9064enwhf2lp?svg=true)](https://ci.appveyor.com/project/wasdevb1/ci-maven) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/3882b05e21b14d85bfe2f7bfe6abef13)](https://www.codacy.com/app/wasdevb1/ci.maven?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=WASdev/ci.maven&amp;utm_campaign=Badge_Grade)

Collection of Maven plugins and archetypes for managing Open Liberty and WebSphere Liberty servers and applications.

* [Build](#build)
* [Plugins](#plugins)
	* [Liberty Maven Plugin](#liberty-maven-plugin)
		* [Configuration](#configuration)
		* [Goals](#goals)
* [Packaging types](#packaging-types)
	* [liberty-assembly](#liberty-assembly)
* [Archetypes](#archetypes)
	* [liberty-plugin-archetype](#liberty-plugin-archetype)
	* [liberty-archetype-mp](#liberty-archetype-mp)
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

#### Configuration

To enable Liberty Maven Plugin in your project add the following to your `pom.xml`:

```xml
<project>
    ...
    <build>
        <plugins>
            <!-- Enable liberty-maven-plugin -->
            <plugin>
                <groupId>net.wasdev.wlp.maven.plugins</groupId>
                <artifactId>liberty-maven-plugin</artifactId>
                <version>2.5</version>
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
| [install-server](docs/install-server.md#install-server) | Installs the Liberty runtime. This goal is implicitly invoked by all the other plugin goals and usually does not need to be executed explicitly. |
| [create-server](docs/create-server.md#create-server) | Create a Liberty server. |
| [start](docs/start-server.md#start-server) | Start a Liberty server in background. The server instance will be automatically created if it does not exist. |
| [start-server](docs/start-server.md#start-server) | Alias of the `start` goal. |
| [test-start-server](docs/test-start-server.md/#test-start-server) | Allows you to bypass automatically starting the server during the pre-integration-test phase with pom configuration or a Liberty-specific command line argument. |
| [run](docs/run-server.md#run-server) | Start a Liberty server in foreground. The server instance will be automatically created if it does not exist. |
| [run-server](docs/run-server.md#run-server) | Alias of the `run` goal. |
| [stop](docs/stop-server.md#stop-server) | Stop a Liberty server. The server instance must exist and must be running. |
| [stop-server](docs/stop-server.md#stop-server) | Alias of the `stop` goal. |
| [test-stop-server](docs/test-stop-server.md#stop-server) | Allows you to bypass automatically stopping the server during the post-integration-test phase with pom configuration or a Liberty-specific command line argument. |
| [debug](docs/debug-server.md#debug-server) | Start a Liberty server in debug mode. |
| [debug-server](docs/debug-server.md#debug-server) | Alias of the `debug` goal. |
| [dev](docs/dev.md#dev) | Start a Liberty server in dev mode. |
| [package-server](docs/package-server.md#package-server) | Package a Liberty server. |
| [clean-server](docs/clean-server.md#clean-server) | Deletes every file in the `${outputDirectory}/logs`, `${outputDirectory}/workarea`, `${userDirectory}/dropins` or `${userDirectory}/apps`. |
| [dump-server](docs/dump-server.md#dump-server) | Dump diagnostic information from the server into an archive. |
| [java-dump-server](docs/java-dump-server.md#java-dump-server) | Dump diagnostic information from the server JVM. |
| [deploy](docs/deploy.md#deploy) | Deploy an application to a Liberty server. The server instance must exist and must be running. |
| [undeploy](docs/undeploy.md#undeploy) | Undeploy an application to a Liberty server. The server instance must exist and must be running. |
| [install-feature](docs/install-feature.md#install-feature) | Install a feature packaged as a Subsystem Archive (esa) to the Liberty runtime. |
| [uninstall-feature](docs/uninstall-feature.md#uninstall-feature) | Uninstall a feature from the Liberty runtime. |
| [install-apps](docs/install-apps.md#install-apps) | Copy applications specified as Maven compile dependencies to Liberty server's `dropins` or `apps` directory. |
| [compile-jsp](docs/compile-jsp.md#compile-jsps) | Compile JSPs in the src/main/webapp into the target/classes directory |
| [display-url](docs/display-url.md#display-url) | Display the application URL in the default browser. |
| status | Check a Liberty server status. |
| server-status | Alias of the `status` goal. |


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

The `liberty-assembly` Maven packaging type is used to create a packaged Liberty server Maven artifact out of existing server installation, compressed archive, or another server Maven artifact. Any applications specified as Maven compile dependencies will be automatically packaged with the assembled server. [Liberty features](docs/install-feature.md) can also be installed and packaged with the assembled server. Any application or test code included in the project is automatically compiled and tests run at appropriate unit or integration test phase. Application code is installed as a loose application WAR file if `installAppPackages` is set to `all` or `project` and `looseApplication` is set to `true`.

The `liberty-assembly` default lifecycle includes:

| Phase | Goal |
| ----- | ---- |
| pre-clean | liberty:stop-server |
| process-resources | maven-resources-plugin:resources |
| compile | maven-compiler-plugin:compile |
| process-test-resources | maven-resources-plugin:testResources |
| test-compile | maven-compiler-plugin:testCompile |
| test | maven-surefire-plugin:test |
| prepare-package | liberty:create-server, liberty:install-feature |
| package | liberty:install-apps, liberty:package-server |
| pre-integration-test | liberty:test-start-server |
| integration-test | maven-failsafe-plugin:integration-test |
| post-integration-test | liberty:test-stop-server |
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
                <groupId>net.wasdev.wlp.maven.plugins</groupId>
                <artifactId>liberty-maven-plugin</artifactId>
                <version>2.5</version>
                <extensions>true</extensions>
                <configuration>
                    <installDirectory>/opt/ibm/wlp</installDirectory>
                    <serverName>test</serverName>
                    <features>
                        <acceptLicense>true</acceptLicense>
                        <feature>mongodb-2.0</feature>
                    </features>
                    <looseApplication>true</looseApplication>
                    <installAppPackages>all</installAppPackages>
                </configuration>
            </plugin>
        </plugins>
    </build>
    ...
</project>
```
## Archetypes

By default, all archetypes that specify a Liberty runtime use the latest version of the Open Liberty runtime. You can use a different runtime by setting the `runtimeGroupId` and `runtimeArtifactId`. For example, you can use `wlp-webProfile7` by setting `-DruntimeGroupId=com.ibm.websphere.appserver.runtime` and `-DruntimeArtifactId=wlp-webProfile7`. 

The runtime version can also be set dynamically. For example, you can specify version `18.0.0.1` of the runtime by setting `-DruntimeVersion=18.0.0.1`.

Finally, the default Liberty Maven Plugin version is set to be the latest version of the plugin. To specify a different version of the plugin, use the `libertyPluginVersion` parameter. For example, you could set `-DlibertyPluginVersion=2.2`.

### liberty-plugin-archetype

The `liberty-plugin-archetype` is used to generate a basic multi-module project that builds a simple web application then deploys and tests it on a Liberty server. It also creates a Liberty server package that includes the application.

#### Usage

    mvn archetype:generate \
        -DarchetypeGroupId=net.wasdev.wlp.maven \
        -DarchetypeArtifactId=liberty-plugin-archetype \
        -DarchetypeVersion=2.2 \
        -DlibertyPluginVersion=2.2 \
        -DgroupId=test \
        -DartifactId=test \
        -Dversion=1.0-SNAPSHOT

### liberty-archetype-mp

The `liberty-archetype-mp` is used to generate a basic single-module project that builds a simple MicroProfile application then deploys and tests on a Liberty server. It also creates a minified, runnable Liberty server package that includes the application. The generated project includes [`liberty-maven-app-parent`](docs/parent-pom.md) parent pom that binds Liberty Maven Plugin goals to the Maven default build lifecycle.

#### Usage

    mvn archetype:generate \
        -DarchetypeGroupId=net.wasdev.wlp.maven \
        -DarchetypeArtifactId=liberty-archetype-mp \
        -DarchetypeVersion=2.3-SNAPSHOT \
        -DgroupId=test \
        -DartifactId=test \
        -Dversion=1.0-SNAPSHOT
        
For this archetype, you might want to use the `wlp-microprofile1` runtime. You can specify this by setting `-DruntimeGroupId=com.ibm.websphere.appserver.runtime` and `-DruntimeArtifactId=wlp-microprofile1`. 
        
### liberty-archetype-webapp

The `liberty-archetype-webapp` is used to generate a basic single-module project that builds a simple web application then deploys and tests on a Liberty server. It also creates a minified, runnable Liberty server package that includes the application. The generated project includes the [`liberty-maven-app-parent`](docs/parent-pom.md) parent pom that binds Liberty Maven Plugin goals to the Maven default build lifecycle.

#### Usage

    mvn archetype:generate \
        -DarchetypeGroupId=net.wasdev.wlp.maven \
        -DarchetypeArtifactId=liberty-archetype-webapp \
        -DarchetypeVersion=2.2 \
        -DgroupId=test \
        -DartifactId=test \
        -Dversion=1.0-SNAPSHOT

### liberty-archetype-ear

The `liberty-archetype-ear` is used to generate a multi-module project that includes an EJB module, a web application module and an EAR module. In the EAR module, it packages the application in a Java EE 7 Enterprise Archive then deploys and tests on a Liberty server. It also creates a minified, runnable Liberty server package that includes the application EAR file. The generated project includes [`liberty-maven-app-parent`](docs/parent-pom.md) parent pom that binds Liberty Maven Plugin goals to the Maven default build lifecycle.

#### Usage

    mvn archetype:generate \
        -DarchetypeGroupId=net.wasdev.wlp.maven \
        -DarchetypeArtifactId=liberty-archetype-ear \
        -DarchetypeVersion=2.2 \
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
