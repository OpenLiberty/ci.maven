# Liberty Maven plug-in release 3.0 differences

When moving to release 3.0 of the `liberty-maven-plugin` from an earlier version, there are new 
features and behavior differences to take into consideration when updating your project. 

* [Maven dependencies](#maven-dependencies)
* [New capabilities in the liberty-maven-plugin](#new-capabilities-in-the-liberty-maven-plugin)
    * [Dev Mode](#dev-mode)
* [Behavior differences in the liberty-maven-plugin](#behavior-differences-in-the-liberty-maven-plugin)
    * [Removed alias goals and renamed goals](#removed-alias-goals-and-renamed-goals)
    * [Changes in parameter names and defaults for common server parameters](#changes-in-parameter-names-and-defaults-for-common-server-parameters)
    * [Simplified server installation](#simplified-server-installation)
    * [Changed package goal](#changed-package-goal)
    * [Changed deploy goal and removed install-apps goal](#changed-deploy-goal-and-removed-install-apps-goal)

## Maven dependencies

Updates to the `liberty-maven-plugin` require the use of Maven 3.5.0 or higher.

## New capabilities in the liberty-maven-plugin

There are new capabilities available in the `liberty-maven-plugin` that you might what to use when moving to release 3 of the plug-in.

### Dev Mode
There is a new [`dev` goal](dev.md) that starts a Liberty server in dev mode. Dev mode provides three key features. Code changes are detected, recompiled, and picked up by your running server. Unit and integration tests are run on demand when you press Enter in the command terminal where dev mode is running, or optionally on every code change to give you instant feedback on the status of your code. Finally, it allows you to attach a debugger to the running server at any time to step through your code.

## Behavior differences in the liberty-maven-plugin

### Removed alias goals and renamed goals
The following goal names are simplified:

| Previous goal name | New goal name |
| clean-server | clean |
| create-server | create |
| debug-server | debug |
| dump-server | dump |
| java-dump-server | java-dump |
| package-server | package |
| run-server | run |
| start-server | start |
| stop-server | stop |
| test-start-server | test-start |
| test-stop-server | test-stop |

### Changes in parameter names and defaults for common server parameters
In the past, the `configFile`, `bootstrapPropertiesFile`, `jvmOptionsFile`, and `serverEnv` [common server parameters](common-server-parameters.md) had default values. If the file existed in the `configDirectory` though, that file took precedence. Now these common server parameters have no default values. If values are specified for these common server parameters, they will take precedence over files located in the `configDirectory`. The `configDirectory` defaults to `${based}/src/main/liberty/config`. Also, `bootstrapProperties` will take precedence over `bootstrapPropertiesFile`, and `jvmOptions` will take precedence over `jvmOptionsFile`. No merging is done.

| Previous parameter name | Previous default value | New parameter name | New default value |
| configFile | `${basedir}/src/test/resources/server.xml`| serverXmlFile | None | 
| bootstrapPropertiesFile | `${basedir}/src/test/resources/bootstrap.properties` | bootstrapPropertiesFile | None | 
| jvmOptionsFile | `${basedir}/src/test/resources/jvm.options` | jvmOptionsFile | None | 
| serverEnv | `${basedir}/src/test/resources/server.env` | serverEnvFile | None | 

Order of precedence (from highest to lowest):

1) Inlined configuration - bootstrapProperties, jvmOptions
2) Specified file - bootstrapPropertiesFile, jvmOptionsFile, serverEnvFile, serverXmlFile
3) File located in `configDirectory` (which defaults to `${basedir}/src/main/liberty/config`)

### Simplified server installation
In the past, if no `assemblyArchive` or `assemblyArtifact` was specified, the latest Liberty runtime was installed from DHE. Now by default, the latest Open Liberty runtime is installed from Maven Central. The new `liberty.runtime.version` property can be specified on the command line or as a Maven property to override which version of Liberty is installed. Also, `assemblyArchive` is renamed to `runtimeArchive`, and `assemblyArtifact` is renamed to `runtimeArtifact`. These are [common parameters](common-parameters.md) shared by all goals.

### Changed package goal

The [`package` goal](package.md) is changed. The `runnable` value is deprecated for the `include` parameter. Use the new `packageType` parameter with value `jar` to generate a runnable Jar file. The `packageFile` parameter is also removed. The `packageName`, `packageDirectory` and `packageType` should be used instead.

### Changed deploy goal and removed install-apps goal

The [`deploy` goal](deploy.md) is changed to handle both copying and deploying of applications to a Liberty server, depending on the status of the server. If the server is not running, the applications are simply copied onto the server. If the server is running, the applications are deployed and verified to have started on the server. The `install-apps` goal is removed. The `looseApplication` parameter has a new default value of `true`. The `installAppPackages` parameter is renamed `deployPackages` and has a new default value of `project`. The `appArchive` and `appArtifact` parameters are removed. The applications come from the project, the dependencies, or both.