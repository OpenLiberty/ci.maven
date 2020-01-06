# Liberty Maven plug-in release 2.0 differences

When moving to release 2.0 of the `liberty-maven-plugin` from an earlier version, there are new 
features and behavior differences to take into consideration when updating your project. 

* [Maven dependencies](#maven-dependencies)
* [New features in liberty-maven-plugin](#new-capabilities-in-the-liberty-maven-plugin)
    * [Loose application configuration](#loose-application-configuration)
    * [Copy a configuration directory](#copy-a-configuration-directory)
    * [New start and stop goals for testing](#new-start-and-stop-goals-for-testing)
    * [Update Liberty license](#update-liberty-license) 
    * [New debug server goal](#debug-server)
* [Behavior differences in the liberty-maven-plugin](#behavior-differences-in-the-liberty-maven-plugin)
    * [Application installation location default](#application-installation-default-location)
    * [packageFile default](#packagefile-default)
    * [stop-server errors](#stop-server-errors)
    * [liberty-assembly differences](#liberty-assembly-differences)
* [Archetypes](#archetypes)
    * [liberty-archetype-webapp and the new parent pom](#liberty-archetype-webapp-and-the-new-parent-pom)

## Maven dependencies

Updates to the `liberty-maven-plugin` require the use of Maven 3.0 or higher.

## New capabilities in the liberty-maven-plugin

There are new capabilities available in the `liberty-maven-plugin` that you might what to use when moving to release 2 of the plug-in.

### Note upon 3.0 release

With the 3.0 release of the `liberty-maven-plugin`, this page, written at the time of the 2.0 release, now links to an older version of the documentation to
describe the older 2.0 goals.

### Loose application configuration
[Loose applications](https://www.ibm.com/support/knowledgecenter/SSD28V_9.0.0/com.ibm.websphere.wlp.core.doc/ae/rwlp_loose_applications.html) are applications composed from multiple physical locations described by an XML file. In development, they allow easy updates to classes and files without repackaging an application archive. The `liberty-maven-plugin` can now install web applications using loose application XML with the [`install-apps` goal](https://github.com/OpenLiberty/ci.maven/blob/liberty-maven-2.0/docs/install-apps.md) and the `looseApplication` parameter. The `package-server` goal will create a WAR file from the loose application XML file description and package the WAR with the server archive.

### Copy a configuration directory
There is a new [common server parameter](https://github.com/OpenLiberty/ci.maven/blob/liberty-maven-2.0/docs/common-server-parameters.md) that allows you to copy a directory of server 
configuration files. If you are using files included in your `server.xml` file, then with one configuration element, 
you can copy all your configuration related files if they are in one folder. It supports nested directories.

### New start and stop goals for testing
There are two new goals to assist with integration test. The [`test-start-server`](https://github.com/OpenLiberty/ci.maven/blob/liberty-maven-2.0/docs/test-start-server.md) and
[`test-stop-server`](https://github.com/OpenLiberty/ci.maven/blob/liberty-maven-2.0/docs/test-stop-server.md) goals honor elements, such as `skipTests`, `skipITs`, and `maven-test-skip`, used to skip the testing phases.

### Update Liberty license
You can update the Liberty runtime license to a production license using the [`install-server` goal](https://github.com/OpenLiberty/ci.maven/blob/liberty-maven-2.0/docs/install-server.md).

### debug-server
The new [`debug-server` goal](https://github.com/OpenLiberty/ci.maven/blob/liberty-maven-2.0/docs/debug-server.md) allows you to pause the server during foreground start so that a debugger 
can be attached to the server process.

## Behavior differences in the liberty-maven-plugin

### Application installation default location
In previous releases of the `liberty-maven-plugin`, applications are installed in the `dropins` directory by default.
This location could be overridden by the `appsDirectory` parameter. This default causes problems if the 
application was also configured in the `server.xml` or other server configuration file. 
Starting with release 2.0 of the plug-in, the default `appsDirectory` and the plug-in behavior depends on whether
the application is configured for the server or not. Errors and warnings will be issued for situations that would
pass the goal but not run properly on the server. 

| | Application is configured | Application is not configured |
| ----- | ---- | ---- |
|`appsDirectory` not set | Install to the `apps` folder. | Install to the `dropins` folder. |
|`appsDirectory` set to `dropins` | `install-apps` goal will generate an error. | Install to the `dropins` folder |
|`appsDirectory` set to `apps` | Install to the `apps` folder. | Add `webApplication` configuration to the target `configDropins` folder. Produce a warning message telling user to configure the application in the source server configuration. |

### packageFile default
By default, when the `packageFile` parameter is not set on the `package-server` goal, the goal would put the archive at unexpected locations defined by the `server package` command. Now, by default packages are created in the `target` folder with naming described in the [`package-server` goal documentation](https://github.com/OpenLiberty/ci.maven/blob/liberty-maven-2.0/docs/package-server.md).

### stop-server errors
In the past, the stop-server goal would fail if the server package was not valid or could not run the stop.  Now you will get a warning since in most failure cases, the server was not running. The goal would also try to install the server if it was not already installed. It no longer installs the server. The goal will try its best to stop the server, but if it cannot, it will not fail the build.

### liberty-assembly differences

The default lifecycle for the `liberty-assembly` packaging type binds more goals than in the previous release. This allows you to have a more simplified pom file when you are running a typical build lifecycle for application development. The following chart shows all the goals that are bound in the release 2.0 plug-in and whether it is a new goal or not.

| Phase | Goal | New |
| ----- | ---- | -----|
| pre-clean | liberty:stop-server | yes |
| process-resources | maven-resources-plugin:resources | no |
| compile | maven-compiler-plugin:compile | yes |
| process-test-resources | maven-resources-plugin:testResources | no |
| test-compile | maven-compiler-plugin:testCompile | yes |
| test | maven-surefire-plugin:test | yes |
| prepare-package | liberty:install-server, liberty:create-server, liberty:install-feature | no |
| package | liberty:install-apps, liberty:package-server | no |
| pre-integration-test | liberty:test-start-server | yes |
| integration-test | maven-failsafe-plugin:integration-test | yes |
| post-integration-test | liberty:test-stop-server | yes |
| verify | maven-failsafe-plugin:verify | yes |
| install | maven-install-plugin:install | no |
| deploy | maven-deploy-plugin:deploy | no |

## Archetypes
### liberty-archetype-webapp and the new parent pom

`liberty-archetype-webapp` is used to generate a basic single-module project that builds a simple web application then deploys and tests is on a Liberty server. It also creates a minified, runnable Liberty server package that includes the application. The generated project includes [`liberty-maven-app-parent`](https://github.com/OpenLiberty/ci.maven/blob/liberty-maven-2.0/docs/parent-pom.md) parent pom that binds `liberty-maven-plugin` goals to the Maven default build lifecycle.

