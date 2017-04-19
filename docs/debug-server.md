#### debug-server

----
Start a Liberty server in debug mode. The server will listen on a specified port or `port 7777` by default.

A port can be specified in the `server.env` file located at `${basedir}/src/test/resources/server.env`.
To set the port add the line `WLP_DEBUG_ADDRESS=<port #>` anywhere in the file.

###### Additional Parameters

This goal supports the [common server parameters](common-server-parameters.md#common-server-parameters) shared by server goals.


