#### dev

----
Start a Liberty server in dev mode. This goal also runs the `create-server`, `install-feature`, and `install-apps` goals before starting the server. **Note:** This goal is designed to be executed directly from the Maven command line.


Dev mode provides three key features. Code changes are immediately detected, recompiled, and picked up by your running server. Unit and integration tests are run on every code change to give you instant feedback on the status of your code. Finally, it allows you to attach a debugger to the running server at any time to step through your code.

The following are the parameters supported by this goal in addition to the [common server parameters](common-server-parameters.md#common-server-parameters) and the [common parameters](common-parameters.md#common-parameters).


| Parameter | Description | Required |
| --------  | ----------- | -------  |
| skipTests | Skip tests. | No |
| skipUTs | Skip unit tests. | No |
| skipITs | Skip integration tests. | No |
| liberty.debug.port | The debug port that you can attach a debugger to. The default value is `7777`. | No |


Example:

```
$ mvn liberty:dev
```

