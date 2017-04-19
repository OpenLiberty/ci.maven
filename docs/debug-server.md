#### debug-server

----
Start a Liberty server in debug mode. The server will listen on a specified debug port or `port 7777` by default.

A nondefault debug port can be specified in the `server.env` file by adding the line `WLP_DEBUG_ADDRESS=<port #>`. The file can be created at `${basedir}/src/test/resources/server.env` or at a location defined by the `serverEnv` [common server parameter](common-server-parameters.md#common-server-parameters).

###### Additional Parameters

This goal supports the other [common server parameters](common-server-parameters.md#common-server-parameters) shared by server goals.