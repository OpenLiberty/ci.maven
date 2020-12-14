#### dev

----

Start a Liberty server in dev mode. This goal also invokes the `create`, `install-feature`, and `deploy` goals before starting the server. **Note:** This goal is designed to be executed directly from the Maven command line.

To start the server in a container, see the [devc](#devc-container-mode) section below. 

###### Console Actions

While dev mode is running, perform the following in the command terminal to run the corresponding actions.

* To run tests on demand, press Enter.
* To restart the server, type `r` and press Enter.
* To exit dev mode, press `Control-C`, or type `q` and press Enter.

###### Features

Dev mode provides three key features. Code changes are detected, recompiled, and picked up by your running server. Unit and integration tests are run on demand when you press Enter in the command terminal where dev mode is running, or optionally on every code change to give you instant feedback on the status of your code. Finally, it allows you to attach a debugger to the running server at any time to step through your code.

The following are dev mode supported code changes. Changes to your server such as changes to the port, server name, hostname, etc. will require restarting dev mode to be detected.  Changes other than those listed below may also require restarting dev mode to be detected.

* Java source file changes and Java test file changes are detected, recompiled, and picked up by your running server.  
* Added dependencies to your `pom.xml` are detected and added to your classpath.  Dependencies that are Liberty features will be installed via the `install-feature` goal.  Any other changes to your `pom.xml` will require restarting dev mode to be detected.
* Resource file changes are detected and copied into your `target` directory. 
* Configuration directory and configuration file changes are detected and copied into your `target` directory, which are hot deployed to the server.  Added features to your `server.xml` will be installed and picked up by your running server.  Adding a configuration directory or configuration file that did not previously exist while dev mode is running will require restarting dev mode to be detected.


###### Examples

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
| hotTests | If set to `true`, run unit and integration tests automatically after every change. The default value is `false`. | No |
| skipTests | If set to `true`, do not run any tests in dev mode. The default value is `false`. | No |
| skipUTs | If set to `true`, skip unit tests. The default value is `false`. If the project packaging type is `ear`, unit tests are always skipped. | No |
| skipITs | If set to `true`, skip integration tests. The default value is `false`.  | No |
| debug | Whether to allow attaching a debugger to the running server. The default value is `true`. | No |
| debugPort | The debug port that you can attach a debugger to. The default value is `7777`. | No |
| compileWait | Time in seconds to wait before processing Java changes. If you encounter compile errors while refactoring, increase this value to allow all files to be saved before compilation occurs. The default value is `0.5` seconds. | No |
| serverStartTimeout | Maximum time to wait (in seconds) to verify that the server has started. The value must be an integer greater than or equal to 0. The default value is `90` seconds. | No |
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

Start a Liberty server in a local container using the Dockerfile that you provide. An alternative to the `devc` goal is to specify the `dev` goal with the `-Dcontainer` option.

When dev mode runs with container support, it builds a container image and runs the container. You can examine the commands that it uses to build and run the container by viewing the console output of dev mode. Additionally, it still provides the same features as the `dev` goal. It monitors files for changes and runs tests either automatically or on demand. This mode also allows you to attach a debugger to work on your application. You can review the logs generated by your server in the Liberty directory in your project e.g. target/liberty/wlp/usr/servers/defaultServer/logs.

###### Prerequisites

You need to install the Docker runtime locally (Docker Desktop on macOS or Windows, or Docker on Linux) to use this Maven goal. The installed Docker Client and Engine versions must be 18.03.0 or higher.

###### Dockerfile

Your project must have a Dockerfile to use dev mode in container mode. A sample Dockerfile is shown in [Building an application image](https://github.com/openliberty/ci.docker/#building-an-application-image). The parent image must be one of the [Open Liberty container images](https://github.com/openliberty/ci.docker/#container-images), or an image using Linux with Open Liberty configured with the same paths as the Open Liberty container images. The Dockerfile must copy the application .war file and the server configuration files that the application requires into the container.

Dev mode works with a temporary, modified copy of your Dockerfile to allow for hot deployment during development as detailed below. When dev mode starts up, it pulls the latest version of the parent image defined in the Dockerfile, builds the container image, then runs the container. Note that the context of the `docker build` command used to generate the container image is the directory containing the Dockerfile. When dev mode exits, the container is stopped and deleted, and the logs are preserved in the directory mentioned above.

Hot deployment is made possible because the application is installed as a loose application WAR. This method uses a file type of `.war.xml` which is functionally equivalent to the `.war` file. Dev mode only supports the application under development in the current project so to avoid application conflicts, dev mode removes all Dockerfile commands that copy or add a `.war` file.

The `.war.xml` file is generated in the `defaultServer/apps` or the `defaultServer/dropins` directory so these directories are mounted in the container. Therefore any files that the Dockerfile may have copied into these directories in the container image will not be accessible.

There are other features of the Dockerfile which are not supported for hot deployment of changes. See the section on [File Tracking](#File-Tracking) for details.

Finally, if dev mode detects the Liberty command `RUN configure.sh` it will insert the environment variable command `ENV OPENJ9_SCC=false` in order to skip the configuration of the [shared class cache](https://github.com/OpenLiberty/ci.docker/#openj9-shared-class-cache-scc).

###### File Tracking

Dev mode offers different levels of file tracking and deployment depending on the way the file is specified in the Dockerfile. 
1. When you use the COPY command on an individual file, dev mode can track file changes and hot deploy them to the container subject to the limitations below. **This is the recommended way to deploy files for dev mode,** so that you can make changes to those files at any time without needing to rebuild the image or restart the container.
   - E.g. `COPY src/main/liberty/config/server.xml /config/` 
   - Note that the Dockerfile must copy only one `.war` file for the application. See the section on [Dockerfiles](#Dockerfile) for details.
2. You can use the COPY command to deploy an entire directory and its sub-directories. In this case, dev mode will detect file changes and automatically rebuild the image and restart the container upon changes.
3. The ADD command can be used on individual files, including tar files, as well as on directories. Again, dev mode will rebuild the image and restart the container when it detects file changes.
4. Certain Dockerfile features are not supported by dev mode. In these cases, the files specified are not tracked. If you change these files, you must rebuild the image and restart the container manually. **Type 'r' and press Enter to rebuild the image and restart the container.**
   - variable substitution used in the COPY or ADD command e.g. `$PROJECT/config`
   - wildcards used in the COPY or ADD command e.g. `src/main/liberty/config/*`
   - paths relative to WORKDIR e.g. `WORKDIR /other/project` followed by `COPY test.txt relativeDir/`
   - files copied from a different part of a multistage Docker build e.g. `COPY --from=<name>`

###### Console Actions

While dev mode is running in container mode, perform the following in the command terminal to run the corresponding actions.

* To run tests on demand, press Enter.
* To rebuild the Docker image and restart the container, type `r` and press Enter.
* To exit dev mode, press `Control-C`, or type `q` and press Enter.

###### Linux Limitations

The following limitations apply to Linux:

* In dev mode, the Open Liberty server runs in the container on the UID (user identifier) of the current user. This is so that the server can access the configuration files from your project and you can access the Open Liberty log files. Outside of dev mode, the Open Liberty server will run on the UID specified in the Docker image.
* Use of editors like `vim`: when you edit a configuration file with `vim` it will delete the file and rewrite it when you save. This necessitates a container restart. To avoid the restart edit your .vimrc file and add the line `set backupcopy=yes`

###### Examples

Start dev mode with the server in a container using the Dockerfile in the project root.
```
$ mvn liberty:devc
```

Customizing the container configuration in `pom.xml`.  Note that changing these while dev mode is running is not supported.
```
            <!-- Enable liberty-maven plugin -->
            <plugin>
                <groupId>io.openliberty.tools</groupId>
                <artifactId>liberty-maven-plugin</artifactId>
                <version>3.3-M1</version>
                <configuration>
                    <container>true</container>
                    <dockerRunOpts>-e key=value</dockerRunOpts>
                    <dockerfile>myDockerfile</dockerfile>
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

To publish additional ports, add them to the `dockerRunOpts` parameter either in the `pom.xml` file or on the `mvn` command line.  For example:
```
-DdockerRunOpts="-p 8000:8000"
```

To map the container ports to specific local ports that are not the default, use the `skipDefaultPorts` parameter and specify Docker port mappings using the `dockerRunOpts` parameter:
```
-DskipDefaultPorts -DdockerRunOpts="-p 10000:9080 -p 10001:9443"
```

Alternatively, you can have Docker map random ephemeral local ports to the exposed container ports as follows.
```
-DskipDefaultPorts -DdockerRunOpts="-P"
```

Note that you do not need to specify an alternative for the debug port. Dev mode will automatically find an open local port to map the container debug port to. 

###### Additional Parameters

These parameters are available in addition to the ones in the `dev` section above.

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| container | If set to `true`, run the server in the container specified by the `dockerfile` parameter. Setting this to `true` is equivalent to using the `devc` goal. The default value is `false` when the `dev` goal is used, and `true` when the `devc` goal is used. | No |
| dockerRunOpts | Specifies options to add to the `docker run` command when using dev mode to launch your server in a container. For example, `-e key=value` is recognized by `docker run` to define an environment variable with the name `key` and value `value`. | No |
| dockerfile | Location of a Dockerfile to be used by dev mode to build the Docker image for the container that will run your Liberty server.  The directory containing the Dockerfile will also be the context for the `docker build`. The default value is `Dockerfile`. | No |
| dockerBuildTimeout | Maximum time to wait (in seconds) for the completion of the Docker operation to build the image. The value must be an integer greater than 0. The default value is `600` seconds. | No |
| skipDefaultPorts | If set to `true`, dev mode will not publish the default Docker port mappings of `9080:9080` (HTTP) and `9443:9443` (HTTPS). Use this option if you would like to specify alternative local ports to map to the exposed container ports for HTTP and HTTPS using the `dockerRunOpts` parameter. | No |
| keepTempDockerfile | If set to `true`, dev mode will not delete the temporary modified copy of your Dockerfile used to build the Docker image. This file is handy in case you need to debug the process of building the Docker image. The path of the temporary Dockerfile can be seen when dev mode displays the `docker build` command. The default value is `false`.| No |
