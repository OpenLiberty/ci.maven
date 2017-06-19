#### stop-server
---
Stop a Liberty server. The server instance must exist and must be running.

###### Additional Parameters

This goal supports [common server parameters](common-server-parameters.md#common-server-parameters) and [common parameters](common-parameters.md#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| serverStopTimeout | Deprecated. This parameter is ignored. Maximum time to wait (in seconds) to verify that the server has stopped. The default value is 30 seconds. | Ignored |

Example:
```xml
<plugin>
    <groupId>net.wasdev.wlp.maven.plugins</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <executions>
        ...
        <execution>
            <id>stop-server</id>
            <phase>post-integration-test</phase>
            <goals>
                <goal>stop-server</goal>
            </goals>
        </execution>
        ...
    </executions>
    <configuration>
       <installDirectory>/opt/ibm/wlp</installDirectory>
       <serverName>test</serverName>
    </configuration>
</plugin>
```
