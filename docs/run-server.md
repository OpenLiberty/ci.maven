#### run-server
---
Start a Liberty Profile server in foreground. The server instance will be automatically created if it does not exist.
**Note:** This goal is designed to be executed directly from the Maven command line.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common server parameters](common-server-parameters.md#common-server-parameters) and the [common parameters](common-parameters.md#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| clean | Clean all cached information on server start up. The default value is `false`. | No |

Example:
```xml
<plugin>
    <groupId>net.wasdev.wlp.maven.plugins</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <configuration>
       <installDirectory>/opt/ibm/wlp</installDirectory>
       <serverName>test</serverName>
    </configuration>
</plugin>
```

```bash
$ mvn liberty:run-server
```
