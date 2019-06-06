#### dev

----
Start a Liberty server in dev mode.


Dev mode provides three key features. Code changes are immediately detected, recompiled, and picked up by your running server. Unit and integration tests are run on every code change to give you instant feedback on the status of your code. Finally, it allows you to attach a debugger to the running server at any time to step through your code.


| Parameter | Description | Default Value |
| --------  | ----------- | -------  |
| skipTests | Skip tests | false |
| skipUTs | Skip unit tests | false |
| skipITs | Skip integration tests | false |
| liberty.debug.port | The debug port that you can attach a debugger to | 7777 |
