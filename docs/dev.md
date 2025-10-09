#### dev

----

Start a Liberty instance in dev mode. This goal also invokes the `create`, `install-feature`, and `deploy` goals before starting the runtime. **Note:** This goal is designed to be executed directly from the Maven command line.

 Starting in version 3.6.1, dev mode invokes the `generate-features` goal when the `generateFeatures` configuration parameter is set to `true`. **This goal modifies the source configuration directory of your application.** See [generate-features](generate-features.md) for details. The default value for the `generateFeatures` parameter is `false`. When auto-generation of features is turned on, dev mode has a runtime dependency on IBM WebSphere Application Server Migration Toolkit for Application Binaries, which is separately licensed under IBM License Agreement for Non-Warranted Programs. For more information, see the [license](https://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/wasdev/license/wamt).

Additionally, starting in version 3.5.2, [resource variable filtering](https://maven.apache.org/plugins/maven-resources-plugin/examples/filter.html) and [WAR overlays](https://maven.apache.org/plugins/maven-war-plugin/overlays.html) are supported for loose WAR applications. This is done by automatically detecting appropriate Maven WAR plugin configuration and calling the WAR plugin's [`exploded`](https://maven.apache.org/plugins/maven-war-plugin/exploded-mojo.html) goal and the Maven Resource plugin's [`resource`](https://maven.apache.org/plugins/maven-resources-plugin/resources-mojo.html) goal when appropriate. Behavior for updating/deleting resources can be configured via the [`outdatedCheckPath`](https://maven.apache.org/plugins/maven-war-plugin/exploded-mojo.html#outdatedCheckPath) parameter introduced and then enhanced in maven-war-plugin versions 3.3.1, 3.3.2.


To start the server in a container, see the [devc](#devc-container-mode) section below. 

###### Console Actions

While dev mode is running, perform the following in the command terminal to run the corresponding actions.

* <kbd>g</kbd> - toggle the automatic generation of features, type <kbd>g</kbd> and press <kbd>Enter</kbd>. A new server configuration file will be generated in the SOURCE configDropins/overrides configuration directory.
* <kbd>o</kbd> - optimize the list of generated features, type <kbd>o</kbd> and press <kbd>Enter</kbd>. A new server configuration file will be generated in the SOURCE configDropins/overrides configuration directory.
* <kbd>Enter</kbd> - run tests on demand, press <kbd>Enter</kbd>.
* <kbd>r</kbd> - restart the server, type <kbd>r</kbd> and press <kbd>Enter</kbd>.
* <kbd>h</kbd> - see the help menu for available actions, type <kbd>h</kbd> and press <kbd>Enter</kbd>.
* <kbd>p</kbd> - see the port information, type <kbd>p</kbd> and press <kbd>Enter</kbd>.
* <kbd>q</kbd> - stop the server and quit dev mode, press <kbd>Ctrl</kbd>-<kbd>C</kbd> or type <kbd>q</kbd> and press <kbd>Enter</kbd>.

###### Features

Dev mode provides three key features. Code changes are detected, recompiled, and picked up by your running server. Unit and integration tests are run on demand when you press <kbd>Enter</kbd> in the command terminal where dev mode is running, or optionally on every code change to give you instant feedback on the status of your code. Finally, it allows you to attach a debugger to the running server at any time to step through your code.

The following are dev mode supported code changes. Changes to your server such as changes to the port, server name, hostname, etc. will require restarting dev mode to be detected.  Changes other than those listed below may also require restarting dev mode to be detected.

* Java source file changes and Java test file changes are detected, recompiled, and picked up by your running server.
* Added dependencies to your `pom.xml` are detected and added to your classpath.  Dependencies that are Liberty features will be installed via the `install-feature` goal.  Any other changes to your `pom.xml` will require restarting dev mode to be detected.
* Resource file changes are detected and copied into your `target` directory. 
* Configuration directory and configuration file changes are detected and copied into your `target` directory, which are hot deployed to the server.  Added features to your `server.xml` will be installed and picked up by your running server.  Adding a configuration directory or configuration file that did not previously exist while dev mode is running will require restarting dev mode to be detected.

###### Multiple Modules

Dev mode can be run on a single Maven module or on a multi module Maven project (a project consisting of multiple modules specified in the `<modules>` section of its `pom.xml`).  When run on a single Maven module, only changes within that module are detected and hot deployed.  When run on a multi module Maven project, changes in all modules are detected and hot deployed according to the Maven Reactor build order.  Note that any modules that other modules rely on as a compile dependency must have a non-empty Java source folder with Java file(s) before starting dev mode, otherwise the other modules may fail to compile.

To start dev mode on a multi module project, run the following from the directory containing the multi module `pom.xml`:
```
$ mvn io.openliberty.tools:liberty-maven-plugin:3.9:dev
```

To start dev mode on a multi module project by using the short-form `liberty` name for the Liberty Maven plugin:
1. Do one of the following:
  * In `~.m2/settings.xml`, add:
```
<pluginGroups>
  <pluginGroup>io.openliberty.tools</pluginGroup>
</pluginGroups> 
```  
  * or define the Liberty Maven plugin in the parent `pom.xml` of every module,  
  * or define the Liberty Maven plugin in `pom.xml` of every module.

2. If the Liberty Maven plugin is defined in your `pom.xml` file(s), ensure it is at version `3.4` or later.
3. From the directory containing the multi module `pom.xml`, run:
```
$ mvn liberty:dev
```

Liberty server configuration files (such as `server.xml`) will be used from the module that does not have any other modules depending on it.  If there is more than one module without other modules depending on it, specify which module with Liberty configuration that you want to use by including the parameters `-pl :<module-with-liberty-config-artifact-id> -am`.  
For example, to use Liberty configuration from a module with artifact id `ear`, run:
```
$ mvn liberty:dev -pl :ear -am
```

More details on the `--projects` flag (`-pl` for short) can be found at https://maven.apache.org/guides/mini/guide-multiple-modules-4.html#inclusion-and-exclusion.

###### Examples

The examples below apply regardless of whether you are using a single module or multi module project.  

Start dev mode.
```
$ mvn liberty:dev
```

Start dev mode and run tests automatically after every code change.
```
$ mvn liberty:dev -DhotTests=true
```

Start dev mode and listen on a specific port for attaching a debugger (default is 7777).
```
$ mvn liberty:dev -DdebugPort=8787
```

Start dev mode without allowing to attach a debugger.
```
$ mvn liberty:dev -Ddebug=false
```

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common server parameters](common-server-parameters.md#common-server-parameters) and the [common parameters](common-parameters.md#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| changeOnDemandTestsAction | If set to `true`, change the action for running on demand tests from `Enter` to type `t` and press `Enter`. The default value is `false`. This parameter is introduced in version 3.11.0. | No |
| compileWait | Time in seconds to wait before processing Java changes. If you encounter compile errors while refactoring, increase this value to allow all files to be saved before compilation occurs. The default value is `0.5` seconds. | No |
| debug | Whether to allow attaching a debugger to the running server. The default value is `true`. | No |
| debugPort | The debug port that you can attach a debugger to. The default value is `7777`. | No |
| generateFeatures | If set to `true`, when a Java file, server configuration file, or build file is changed, generate features required by the application in the source configuration directory. The default value is `false`. | No |
| hotTests | If set to `true`, run unit and integration tests automatically after every change. The default value is `false`. | No |
| recompileDependencies | If set to `true`, when a Java file is changed, recompile all classes in that module and any modules that depend on it. The default value is `false` when running dev mode on a single module, and `true` when running dev mode on a multi module project.  | No |
| serverStartTimeout | Maximum time to wait (in seconds) to verify that the server has started. The value must be an integer greater than or equal to 0. The default value is `90` seconds. | No |
| skipInstallFeature | If set to `true`, the `install-feature` goal will be skipped when `dev` mode is started on an already existing Liberty runtime installation. It will also be skipped when `dev` mode is running and a restart of the server is triggered either directly by the user or by application changes. The `install-feature` goal will be invoked though when `dev` mode is running and a change to the configured features is detected. The default value is `false`. | No |
| skipITs | If set to `true`, skip integration tests. The default value is `false`.  | No |
| skipTests | If this option is enabled, do not run any tests in dev mode, even when the on demand test action is entered or when `hotTests` is set to `true`. The default value is `false`. | No |
| skipUTs | If set to `true`, skip unit tests. The default value is `false`. If the project packaging type is `ear`, unit tests are always skipped. | No |
| verifyTimeout | Maximum time to wait (in seconds) to verify that the application has started or updated before running integration tests. The value must be an integer greater than or equal to 0. The default value is `30` seconds. | No |

###### System Properties for Integration Tests

Integration tests can read the following system properties to obtain information about the Liberty server.

| Property | Description |
| --------  | ----------- |
| wlp.user.dir | The user directory location that contains server definitions and shared resources. |
| liberty.hostname | The host name of the Liberty server. |
| liberty.http.port | The port used for client HTTP requests. |
| liberty.https.port | The port used for client HTTP requests secured with SSL (https). |

----

#### devc, Container Mode

Start a Liberty server in a local container using the Containerfile or Dockerfile that you provide. An alternative to the `devc` goal is to specify the `dev` goal with the `-Dcontainer` option.

When dev mode runs with container support, it builds a container image and runs the container. You can examine the commands that it uses to build and run the container by viewing the console output of dev mode. Additionally, it still provides the same features as the `dev` goal. It monitors files for changes and runs tests either automatically or on demand. This mode also allows you to attach a debugger to work on your application. You can review the logs generated by your server in the Liberty directory in your project e.g. target/liberty/wlp/usr/servers/defaultServer/logs.


##### Copying Generated Configurations to Container Image


N.B. starting in 3.6.1, dev mode invokes `generate-features` if the `generateFeatures` configuration parameter is set to true. Ensure that the `generated-features.xml` configuration file is copied to your container image via your Containerfile/Dockerfile.
```dockerfile
COPY --chown=1001:0  target/liberty/wlp/usr/servers/defaultServer/configDropins/overrides/generated-features.xml /config/configDropins/overrides/
```
If you are using `liberty.var.{var}` or `liberty.defaultVar.{var}`, then the server configuration file named `liberty-plugin-variable-config.xml` is generated in the configDropins/overrides folder or configDropins/defaults folder of the target server respectively.
To ensure your application runs correctly inside a container, you need to copy this generated liberty-plugin-variable-config.xml file into your container image. 

You can do this by adding one of the following lines to your Dockerfile or Containerfile, depending on which type of variable you used.
To copy the file from the overrides folder:

```dockerfile
COPY --chown=1001:0  target/liberty/wlp/usr/servers/defaultServer/configDropins/overrides/liberty-plugin-variable-config.xml /config/configDropins/overrides/
```
To copy the file from the defaults folder:

```dockerfile
COPY --chown=1001:0  target/liberty/wlp/usr/servers/defaultServer/configDropins/defaults/liberty-plugin-variable-config.xml /config/configDropins/defaults/
```


On Linux, it's a good practice to copy the entire configDropins/overrides and configDropins/defaults directories into your container image using your Dockerfile. 

This helps you avoid file permission problems that can happen when the user ID of the container is different from the host's user ID.
```dockerfile
COPY --chown=1001:0  target/liberty/wlp/usr/servers/defaultServer/configDropins/overrides /config/configDropins/overrides
COPY --chown=1001:0  target/liberty/wlp/usr/servers/defaultServer/configDropins/defaults /config/configDropins/defaults
```
Remember, you only need to include the command that matches your configuration.

###### Prerequisites

You need to install Podman or the Docker runtime (Docker Desktop on macOS or Windows, or Docker on Linux) locally to use this Maven goal. When using Podman, you'll need version 4.4.4 or higher, and the Maven plugin requires version [3.9](https://github.com/OpenLiberty/ci.maven/releases/tag/liberty-maven-3.9) or higher. If using Docker, the installed Docker Client and Engine versions must be 18.03.0 or higher.

###### Containerfile/Dockerfile

Your project must have a Containerfile or Dockerfile to use dev mode in container mode. An example of container configuration is shown in [Building an application image](https://github.com/openliberty/ci.docker/#building-an-application-image). The parent image must be one of the [Open Liberty container images](https://github.com/openliberty/ci.docker/#container-images), or an image using Linux with Open Liberty configured with the same paths as the Open Liberty container images. The container config file must copy the application .war file and the server configuration files that the application requires into the container.

In case you are using a custom image, you need to add `--user 1001` (or whatever user id the image should run with) to your `containerRunOpts` configuration variable. You can also set it on the command line using `"-DcontainerRunOpts=--user 1001"`.

Dev mode works with a temporary, modified copy of your container config file to allow for hot deployment during development as detailed below. When dev mode starts up, it pulls the latest version of the parent image defined in the container config file, builds the container image, then runs the container. Note that the context of the container's `build` command used to generate the container image is the directory containing the container config file, unless the `containerBuildContext` parameter is specified. When dev mode exits, the container is stopped and deleted, and the logs are preserved in the directory mentioned above.

Hot deployment is made possible because the application is installed as a loose application WAR. This method uses a file type of `.war.xml` which is functionally equivalent to the `.war` file. Dev mode only supports the application under development in the current project so to avoid application conflicts, dev mode removes all commands that copy or add a `.war` file from the container config file.

The `.war.xml` file is generated in the `defaultServer/apps` or the `defaultServer/dropins` directory so these directories are mounted in the container. Therefore any files that the container config file may have copied into these directories in the container image will not be accessible.

There are other features of the Containerfile/Dockerfile which are not supported for hot deployment of changes. See the section on [File Tracking](#File-Tracking) for details.

Finally, if dev mode detects the Liberty command `RUN configure.sh` it will insert the environment variable command `ENV OPENJ9_SCC=false` in order to skip the configuration of the [shared class cache](https://github.com/OpenLiberty/ci.docker/#openj9-shared-class-cache-scc).

###### File Tracking

Dev mode offers different levels of file tracking and deployment depending on the way the file is specified in the Containerfile/Dockerfile. 
1. When you use the COPY command on an individual file, dev mode can track file changes and hot deploy them to the container subject to the limitations below. **This is the recommended way to deploy files for dev mode,** so that you can make changes to those files at any time without needing to rebuild the image or restart the container.
   - E.g. `COPY target/liberty/wlp/usr/servers/defaultServer/server.xml /config/`
   - Note that the container config file must copy only one `.war` file for the application. See the section on [container config files](#Containerfile/Dockerfile) for details.
2. You can use the COPY command to deploy an entire directory and its sub-directories. In this case, dev mode will detect file changes and automatically rebuild the image and restart the container upon changes.
3. The ADD command can be used on individual files, including tar files, as well as on directories. Again, dev mode will rebuild the image and restart the container when it detects file changes.
4. Certain Containerfile/Dockerfile features are not supported by dev mode. In these cases, the files specified are not tracked. If you change these files, you must rebuild the image and restart the container manually. **Type <kbd>r</kbd> and press <kbd>Enter</kbd> to rebuild the image and restart the container.**
   - variable substitution used in the COPY or ADD command e.g. `$PROJECT/config`
   - wildcards used in the COPY or ADD command e.g. `target/liberty/wlp/usr/servers/defaultServer/configDropins/*`
   - paths relative to WORKDIR e.g. `WORKDIR /other/project` followed by `COPY test.txt relativeDir/`
   - files copied from a different part of a multistage Podman or Docker build e.g. `COPY --from=<name>`

###### Console Actions

While dev mode is running in container mode, perform the following in the command terminal to run the corresponding actions.

* <kbd>g</kbd> - toggle the automatic generation of features, type <kbd>g</kbd> and press <kbd>Enter</kbd>. A new server configuration file will be generated in the SOURCE configDropins/overrides configuration directory.
* <kbd>o</kbd> - optimize the list of generated features, type <kbd>o</kbd> and press <kbd>Enter</kbd>. A new server configuration file will be generated in the SOURCE configDropins/overrides configuration directory.
* <kbd>t</kbd> or <kbd>Enter</kbd> - If `changeOnDemandTestAction` is set to `true`, type <kbd>t</kbd> and press <kbd>Enter</kbd> to run tests on demand. Otherwise, press <kbd>Enter</kbd>. 
* <kbd>r</kbd> - rebuild the container image and restart the container, type <kbd>r</kbd> and press <kbd>Enter</kbd>.
* <kbd>h</kbd> - see the help menu for available actions, type <kbd>h</kbd> and press <kbd>Enter</kbd>.
* <kbd>p</kbd> - see the port information, type <kbd>p</kbd> and press <kbd>Enter</kbd>.
* <kbd>q</kbd> - stop the server and quit dev mode, press <kbd>Ctrl</kbd>-<kbd>C</kbd> or type <kbd>q</kbd> and press <kbd>Enter</kbd>.

###### Linux Limitations

The following limitations apply to Linux:

* In dev mode, the Open Liberty server runs in the container on the UID (user identifier) of the current user. This is so that the server can access the configuration files from your project and you can access the Open Liberty log files. Outside of dev mode, the Open Liberty server will run on the UID specified in the Docker image.
* Use of editors like `vim`: when you edit a configuration file with `vim` it will delete the file and rewrite it when you save. This necessitates a container restart. To avoid the restart edit your .vimrc file and add the line `set backupcopy=yes`

### Podman Troubleshooting
Podman VMs may need to be configured to run in rootful mode depending on how permissions are used in the container. This can be done with the following commands:
```
podman machine stop
podman machine set --rootful=true
podman machine start
```

If permissions issues continue with Podman the machine may need to be deleted and reinitialized. This can be done as follows:
```
podman machine stop
podman machine rm <machine name>
podman machine init --rootful=true
podman machine start
```

###### Multiple Modules

The `devc` goal is supported with multi module Maven projects in the same way as the [`dev` goal](#multiple-modules) but substitute `dev` for `devc` in the examples.

###### Examples

Start dev mode with the server in a container using the Containerfile/Dockerfile in the project root.
```
$ mvn liberty:devc
```

Customizing the container configuration in `pom.xml`.  Note that changing these while dev mode is running is not supported.
```
            <!-- Enable liberty-maven plugin -->
            <plugin>
                <groupId>io.openliberty.tools</groupId>
                <artifactId>liberty-maven-plugin</artifactId>
                <version>3.9</version>
                <configuration>
                    <container>true</container>
                    <containerRunOpts>-e key=value</containerRunOpts>
                    <containerfile>myDockerfile</containerfile>
                </configuration>
            </plugin>
```

###### Port Mappings

By default, container mode publishes the following ports and maps them to the corresponding local ports of the same value:
* HTTP port at 9080
* HTTPS port at 9443
* Debug port at 7777

The container ports and mapped local ports will be displayed when dev mode starts up.

If you use the default ports and you run multiple instances of dev mode in container mode, the containers will use different local port mappings to avoid errors. The first instance will use the local ports 9080 and 9443, the second instance will use 9081 and 9444, and so on.  

To publish additional ports, add them to the `containerRunOpts` parameter either in the `pom.xml` file or on the `mvn` command line.  For example:
```
-DcontainerRunOpts="-p 8000:8000"
```

To map the container ports to specific local ports that are not the default, use the `skipDefaultPorts` parameter and specify container port mappings using the `containerRunOpts` parameter:
```
-DskipDefaultPorts -DcontainerRunOpts="-p 10000:9080 -p 10001:9443"
```

Alternatively, you can have Podman/Docker map random ephemeral local ports to the exposed container ports as follows.
```
-DskipDefaultPorts -DcontainerRunOpts="-P"
```

Note that you do not need to specify an alternative for the debug port. Dev mode will automatically find an open local port to map the container debug port to. 

###### Additional Parameters

These parameters are available in addition to the ones in the `dev` section above.

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| container | If set to `true`, run the server in the container specified by the `containerfile` parameter. Setting this to `true` is equivalent to using the `devc` goal. The default value is `false` when the `dev` goal is used, and `true` when the `devc` goal is used. | No |
| containerBuildContext | The container build context directory to be used by dev mode for the `build` command.  The default location is the directory of the Containerfile/Dockerfile. This attribute replaces the deprecated `dockerBuildContext` attribute. | No |
| containerBuildTimeout | Maximum time to wait (in seconds) for the completion of the container operation to build the image. The value must be an integer greater than 0. The default value is `600` seconds. This attribute replaces the deprecated `dockerBuildTimeout` attribute. | No |
| containerfile | Location of a Containerfile/Dockerfile to be used by dev mode to build the image for the container that will run your Liberty server. The default value is `Containerfile` or `Dockerfile`. This attribute replaces the deprecated `dockerfile` attribute. | No |
| containerRunOpts | Specifies options to add to the `run` command when using dev mode to launch your server in a container. For example, `-e key=value` is recognized by `run` to define an environment variable with the name `key` and value `value`. This attribute replaces the deprecated `dockerRunOpts` attribute. | No |
| keepTempContainerfile | If set to `true`, dev mode will not delete the temporary modified copy of your Containerfile used to build the contianer image. This file is handy in case you need to debug the process of building the container image. The path of the temporary Containerfile can be seen when dev mode displays the container's `build` command. The default value is `false`. This attribute replaces the deprecated `keepTempDockerfile` attribute. | No |
| skipDefaultPorts | If set to `true`, dev mode will not publish the default container port mappings of `9080:9080` (HTTP) and `9443:9443` (HTTPS). Use this option if you would like to specify alternative local ports to map to the exposed container ports for HTTP and HTTPS using the `containerRunOpts` parameter. | No |
