ci.maven
========

Maven plugin and archetype for managing WebSphere Application Server Liberty Profile and its applications.

## Build

Use Maven 2.x or 3.x to build the Liberty plugins and archetype. 

* `mvn install` : builds the plugin and the archetype. 
* `mvn install -DwlpInstallDir=<liberty_install_directory>` : builds the plugin, archetype and runs the integration tests.
  * Liberty Profile installation is required to run the integration tests.

