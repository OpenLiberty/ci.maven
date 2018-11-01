#### create-server
---
Create a Liberty server.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common server parameters](common-server-parameters.md#common-server-parameters) and the [common parameters](common-parameters.md#common-parameters).

The create-server goal will update any [server configuration](common-server-parameters.md#common-server-parameters) files that were changed in the source directory. 

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| template | Name of the template to use when creating a new server. | No |

Example:
```xml
<plugin>
    <groupId>net.wasdev.wlp.maven.plugins</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <executions>
        ...
        <execution>
            <id>create-server</id>
            <phase>pre-integration-test</phase>
            <goals>
                <goal>create-server</goal>
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
