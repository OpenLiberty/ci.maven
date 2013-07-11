ci.maven
========

Collection of Maven plugins and archetypes for managing WebSphere Application Server Liberty Profile and applications.

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

#### Goals

##### start-server
##### stop-server
##### create-server
##### package-server
##### deploy
##### undeploy
##### install-apps

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

