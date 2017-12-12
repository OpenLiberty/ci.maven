# ci.maven [![Build Status](https://travis-ci.org/WASdev/ci.maven.svg?branch=master)](https://travis-ci.org/WASdev/ci.maven) [![Maven Central Latest](https://maven-badges.herokuapp.com/maven-central/net.wasdev.wlp.maven.plugins/liberty-maven-plugin/badge.svg)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22net.wasdev.wlp.maven.plugins%22%20AND%20a%3A%22liberty-maven-plugin%22)

Collection of Maven plug-ins and archetypes for managing WebSphere Application Server Liberty servers and applications.

* [Build](#build)
* [Plug-ins](#plug-ins)
 * [liberty-maven-plugin](#liberty-maven-plugin)
     * [Configuration](#configuration)
     * [Goals](#goals)
* [Packaging types](#packaging-types)
 * [liberty-assembly](#liberty-assembly)
* [Archetypes](#archetypes)
 * [liberty-plugin-archetype](#liberty-plugin-archetype)
 * [liberty-archetype-webapp](#liberty-archetype-webapp)
 * [liberty-archetype-ear](#liberty-archetype-ear)

## Build

Use Maven 3.x to build the Liberty plug-ins and archetypes.

* `mvn install` : builds the plug-in and the archetypes.
* `mvn install -Poffline-its -DwlpInstallDir=<liberty_install_directory>` : builds the plug-in and the archetypes and runs the integration tests by providing an existing installation.
* `mvn install -Ponline-its -DwlpVersion=<liberty_version> -DwlpLicense=<liberty_license_code>` : builds the plug-in and archetypes and runs the integration tests by downloading a new server.
  * Liberty versions and their respective link to the license code can be found in the [index.yml](http://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/wasdev/downloads/wlp/index.yml) file. You can obtain the license code by reading the current license and looking for the D/N: <license code> line.

## Information for release 2.0

A number of new features and changes were made in release 2.0 of the Liberty Maven plug-in and are described 
[here](docs/version_2.0_differences.md).

## Plug-ins

### liberty-maven-plugin

`liberty-maven-plugin` provides a number of goals for managing a Liberty server and applications.

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
                <version>2.0</version>
                <!-- Specify configuration, executions for liberty-maven-plugin -->
                ...
            </plugin>
        </plugins>
    </build>
    ...
</project>
```

If you are using a snapshot version of `liberty-maven-plugin` then you will also need to add the following plug-in repository to your `pom.xml`:

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

`liberty-maven-plugin` must first be configured with the Liberty server installation information. The installation information can be specified as an [existing installation directory](docs/installation-configuration.md#using-an-existing-installation), a [packaged server](docs/installation-configuration.md#using-a-packaged-server), or as a [Maven artifact](docs/installation-configuration.md#using-maven-artifact). The `liberty-maven-plugin` can also download and install a Liberty server from the [Liberty repository](https://developer.ibm.com/wasdev/downloads/) or other location using the [install parameter](docs/installation-configuration.md#using-a-repository). 
By default, the plug-in installs the Liberty runtime with the Java EE 7 Web Profile features from the Liberty repository.

#### Goals

The `liberty-maven-plugin` provides the following goals.

| Goal | Description |
| --------- | ------------ |
| [install-server](docs/install-server.md#install-server) | Installs the Liberty runtime. This goal is implicitly invoked by all the other plug-in goals and usually does not need to be executed explicitly. |
| [create-server](docs/create-server.md#create-server) | Create a Liberty server. |
| [start-server](docs/start-server.md#start-server) | Start a Liberty server in background. The server instance will be automatically created if it does not exist. |
| [test-start-server](docs/test-start-server.md/#test-start-server) | Allows you to bypass automatically starting the server during the pre-integration-test phase with pom configuration or a Liberty-specific command line argument. |
| [run-server](docs/run-server.md#run-server) | Start a Liberty server in foreground. The server instance will be automatically created if it does not exist. |
| [stop-server](docs/stop-server.md#stop-server) | Stop a Liberty server. The server instance must exist and must be running. |
| [test-stop-server](docs/test-stop-server.md#stop-server) | Allows you to bypass automatically stopping the server during the post-integration-test phase with pom configuration or a Liberty-specific command line argument. |
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


##### Common Parameters

Parameters shared by all goals. [See common parameters](docs/common-parameters.md#common-parameters).

##### Common Server Parameters

Additional parameters shared by all server-based goals. [See common server parameters](docs/common-server-parameters.md#common-server-parameters).

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
                <version>2.0</version>
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

### liberty-plugin-archetype

`liberty-plugin-archetype` is used to generate a basic multi-module project that builds a simple web application then deploys and tests it on a Liberty server. It also creates a Liberty server package that includes the application.

#### Usage

    mvn archetype:generate \
        -DarchetypeGroupId=net.wasdev.wlp.maven \
        -DarchetypeArtifactId=liberty-plugin-archetype \
        -DarchetypeVersion=2.1.2 \
        -DwlpPluginVersion=2.1.2 \
        -DgroupId=test \
        -DartifactId=test \
        -Dversion=1.0-SNAPSHOT

### liberty-archetype-webapp

`liberty-archetype-webapp` is used to generate a basic single-module project that builds a simple web application then deploys and tests on a Liberty server. It also creates a minified, runnable Liberty server package that includes the application. The generated project includes [`liberty-maven-app-parent`](docs/parent-pom.md) parent pom that binds `liberty-maven-plugin` goals to the Maven default build lifecycle.

#### Usage

    mvn archetype:generate \
        -DarchetypeGroupId=net.wasdev.wlp.maven \
        -DarchetypeArtifactId=liberty-archetype-webapp \
        -DarchetypeVersion=2.1.2 \
        -DgroupId=test \
        -DartifactId=test \
        -Dversion=1.0-SNAPSHOT
        
By default, the `liberty-maven-plugin` version is set the same as the `liberty-archetype-webapp` archetype version used. To specify a different version of the plugin, use the `wlpPluginVersion` parameter. For example, you could set `-DwlpPluginVersion=2.1`.
        
### liberty-archetype-ear

`liberty-archetype-ear` is used to generate a multi-module project that includes an EJB module, a web application module and an EAR module. In the EAR module, it packages the application in a Java EE 7 Enterprise Archive then deploys and tests on a Liberty server. It also creates a minified, runnable Liberty server package that includes the application EAR file. The generated project includes [`liberty-maven-app-parent`](docs/parent-pom.md) parent pom that binds `liberty-maven-plugin` goals to the Maven default build lifecycle.

#### Usage

    mvn archetype:generate \
        -DarchetypeGroupId=net.wasdev.wlp.maven \
        -DarchetypeArtifactId=liberty-archetype-ear \
        -DarchetypeVersion=2.1.2 \
        -DgroupId=test \
        -DartifactId=test \
        -Dversion=1.0-SNAPSHOT

By default, the `liberty-maven-plugin` version is set the same as the `liberty-archetype-ear` archetype version used. To specify a different version of the plugin, use the `wlpPluginVersion` parameter. For example, you could set `-DwlpPluginVersion=2.1`.


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
