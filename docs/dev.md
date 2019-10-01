#### dev

----

Start a Liberty server in dev mode. This goal also invokes the `create`, `install-feature`, and `deploy` goals before starting the server. **Note:** This goal is designed to be executed directly from the Maven command line.  To exit dev mode, press `Control-C`, or type `q` and press Enter.

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
| serverStartTimeout | Maximum time to wait (in seconds) to verify that the server has started. The default value is `30` seconds. | No |
| verifyTimeout | Maximum time to wait (in seconds) to verify that the application has started before running integration tests. The default value is `30` seconds. | No |

###### System Properties for Integration Tests

Integration tests can read the following system properties to obtain information about the Liberty server.

| Property | Description |
| --------  | ----------- |
| wlp.user.dir | The user directory location that contains server definitions and shared resources. |
| liberty.hostname | The host name of the Liberty server. |
| liberty.http.port | The port used for client HTTP requests. |
| liberty.https.port | The port used for client HTTP requests secured with SSL (https). |
