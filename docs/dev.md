#### dev

----

Start a Liberty server in dev mode. This goal also invokes the `create`, `install-feature`, and `deploy` goals before starting the server. **Note:** This goal is designed to be executed directly from the Maven command line.

To start the server in a container, see the [devc](#devc-container-mode) section below. 

##### Console actions

While dev mode is running, perform the following in the command terminal to run the corresponding actions.

* To run tests on demand, press Enter.
* To restart the server, type `r` and press Enter.
* To exit dev mode, press `Control-C`, or type `q` and press Enter.

##### Features

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

The following is a technology preview. The features and parameters described below may change in future milestones or releases of the Liberty Maven plugin.

Start a Liberty server in a local container using the Dockerfile that you provide. An alternative to the `devc` goal is to specify the `dev` goal with the `-Dcontainer` option. 

The Dockerfile must copy the application .war file and the server configuration files that the application requires into the container. A sample Dockerfile is shown in [Building an application image](https://github.com/openliberty/ci.docker/#building-an-application-image). Note that the context of the `docker build` command used to generate the container image is the directory containing the Dockerfile. When dev mode exits the container is stopped and deleted and the logs are preserved as described below.

You need to install the Docker runtime (e.g. Docker Desktop) locally to use this Maven goal. You can examine the commands used to build and run the container by viewing the console output of dev mode.

When dev mode runs with container support it still provides the same features. It monitors files for changes and runs tests either automatically or on demand. This mode also allows you to attach a debugger to work on your application. You can review the logs generated by your server in the Liberty directory in your project e.g. target/liberty/wlp/usr/servers/defaultServer/logs.

By default, this mode publishes the container ports 9080, 9443 and 7777 and maps them to the corresponding local ports of the same value. If your application needs to publish additional ports, add them to the `dockerRunOpts` option either in the `pom.xml` file or on the `mvn` command line. E.g. `-DdockerRunOpts="-p 9081:9081"`. If you need to map the container ports to different local ports, use the `skipDefaultPorts` option and specify the alternative mappings using the `dockerRunOpts` option. E.g. `-DskipDefaultPorts -DdockerRunOpts="-p 8000:9080" -p 8001:9443"`. 

The previous example should be used when you want to run multiple instances of dev mode in container mode. You can start the first instance with default settings, but you will need to specify `skipDefaultPorts` and the alternative ports in `dockerRunOpts`, as shown in the previous example, for all following instances. Instead of explicity defining the local ports yourself for each instance of dev mode, you can have Docker map random ephemeral local ports to the exposed container ports (9080, 9443) by adding `-P` to `dockerRunOpts`. E.g. `-DskipDefaultPorts -DdockerRunOpts="-P"`.

##### Console actions

While dev mode is running in container mode, perform the following in the command terminal to run the corresponding actions.

* To run tests on demand, press Enter.
* To rebuild the Docker image and restart the container, type `r` and press Enter.
* To exit dev mode, press `Control-C`, or type `q` and press Enter.

###### Limitations

For the current technology preview, the following limitations apply.

- Platform limitations:
  - Supported on macOS and Windows with Docker Desktop installed.
  - Supported on Linux. Note the following.
    - In dev mode the Open Liberty server runs in the container on the UID (user identifier) of the current user. This is so that the server can access the configuration files from your project and you can access the Open Liberty log files. Outside of dev mode the Open Liberty server will run on the UID specified in the Docker image.
    - Use of editors like `vim`: when you edit a configuration file with `vim` it will delete the file
    and rewrite it when you save. This necessitates a container restart. To avoid the restart edit your
    .vimrc file and add the line `set backupcopy=yes`

- Dockerfile limitations:
  - The Dockerfile must copy only one .war file for the application.  Other application archive formats or multiple .war files are not supported.
  - Hot deployment is only supported for individual configuration files that are specified as the source in the Dockerfile's COPY commands. Hot deployment is not supported for COPY commands with variable substitution, wildcard characters, spaces in paths, paths relative to WORKDIR, multi-stage builds, or entire directories specified as the source.

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
                    <dockerRunOpts>-p 9081:9081</dockerRunOpts>
                    <dockerfile>myDockerfile</dockerfile>
                </configuration>
            </plugin>
```

###### Additional Parameters

These parameters are available in addition to the ones in the `dev` section above.

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| container | If set to `true`, run the server in the container specified by the `dockerfile` parameter. Setting this to `true` is equivalent to using the `devc` goal. The default value is `false` when the `dev` goal is used, and `true` when the `devc` goal is used. | No |
| dockerRunOpts | Specifies options to add to the `docker run` command when using dev mode to launch your server in a container. For example, `-e key=value` is recognized by `docker run` to define an environment variable with the name `key` and value `value`. | No |
| dockerfile | Location of a Dockerfile to be used by dev mode to build the Docker image for the container that will run your Liberty server.  The directory containing the Dockerfile will also be the context for the `docker build`. The default value is `Dockerfile`. | No |
| dockerBuildTimeout | Maximum time to wait (in seconds) for the completion of the Docker operation to build the image. The value must be an integer greater than 0. The default value is `60` seconds. | No |
| skipDefaultPorts | If set to `true`, dev mode will not publish the default Docker port mappings of `9080:9080` and `9443:9443`. Use this option if you would like to specify alternative local ports to map to the exposed container ports using the `dockerRunOpts` option. | No |
