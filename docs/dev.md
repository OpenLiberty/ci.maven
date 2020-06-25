#### dev

----

Start a Liberty server in dev mode. This goal also invokes the `create`, `install-feature`, and `deploy` goals before starting the server. **Note:** This goal is designed to be executed directly from the Maven command line.  To exit dev mode, press `Control-C`, or type `q` and press Enter.

To start the server in a container see the section `devc` below. 

Dev mode provides three key features. Code changes are detected, recompiled, and picked up by your running server. Unit and integration tests are run on demand when you press Enter in the command terminal where dev mode is running, or optionally on every code change to give you instant feedback on the status of your code. Finally, it allows you to attach a debugger to the running server at any time to step through your code.

The following are dev mode supported code changes. Changes to your server such as changes to the port, server name, hostname, etc. will require restarting dev mode to be detected.  Changes other than those listed below may also require restarting dev mode to be detected.

* Java source file changes and Java test file changes are detected, recompiled, and picked up by your running server.  
* Added dependencies to your `pom.xml` are detected and added to your classpath.  Dependencies that are Liberty features will be installed via the `install-feature` goal.  Any other changes to your `pom.xml` will require restarting dev mode to be detected.
* Resource file changes are detected and copied into your `target` directory. 
* Configuration directory and configuration file changes are detected and copied into your `target` directory.  Added features to your `server.xml` will be installed and picked up by your running server.  Adding a configuration directory or configuration file that did not previously exist while dev mode is running will require restarting dev mode to be detected.


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

This is a technology preview. The features and parameters described below may change in future milestones or releases of the Liberty Maven plugin.

Start a Liberty server in a local container using the Dockerfile that you provide. An alternative to the `devc` goal is to specify the `dev` goal with the `-Dcontainer` option. 

The Dockerfile must copy the application .war file and the server configuration files that the application requires into the container. A sample Dockerfile is shown in [Building an application image](https://github.com/openliberty/ci.docker/#building-an-application-image). Note that the context of the `docker build` command used to generate the container image is the directory containing the Dockerfile. When dev mode exits the container is stopped and deleted and the logs are preserved as described below.

You need to install the Docker runtime (e.g. Docker Desktop) locally to use this Maven goal. You can examine the commands used to build and run the container by viewing the console output of dev mode.

When dev mode runs with container support it still provides the same features. It monitors files for changes and runs tests either automatically or on demand. This mode also allows you to attach a debugger to work on your application. You can review the logs generated by your server in the Liberty directory in your project e.g. target/liberty/wlp/usr/servers/defaultServer/logs.

This mode publishes the container ports 9080, 9443 and 7777 by default. If your application needs to publish other ports add them to the `dockerRunOpts` option either in the `pom.xml` file or on the `mvn` command line. E.g. `-DdockerRunOpts="-p 9081:9081"`

The following is a sample configuration of this plugin using the `devc` parameters.  Note that changing these while dev mode is running is not supported.
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

These parameters are available in addition to the ones in the `dev` section above.

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| container | If set to `true`, run the server in the container specified by the `dockerfile` parameter. Setting this to `true` is equivalent to using the `devc` goal. The default value is `false` when the `dev` goal is used, and `true` when the `devc` goal is used. | No |
| dockerRunOpts | Specifies options to add to the `docker run` command when using dev mode to launch your server in a container. For example, `-e key=value` is recognized by `docker run` to define an environment variable with the name `key` and value `value`. Setting this parameter overrides the `container` parameter to `true`. | No |
| dockerfile | Location of a Dockerfile to be used by dev mode to build the container that runs your server and to specify the context used to build the container image. The default value is `Dockerfile`. Setting this parameter overrides the `container` parameter to `true`. | No |
