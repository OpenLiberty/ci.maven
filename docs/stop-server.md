#### stop-server
---
Stop a Liberty server. The server instance must exist and must be running.

###### Additional Parameters

This goal supports [common server parameters](common-server-parameters.md#common-server-parameters) and [common parameters](common-parameters.md#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| serverStopTimeout | Deprecated. This parameter is ignored. | No |
| embedded | Whether the server is [embedded](https://www.ibm.com/support/knowledgecenter/SSD28V_9.0.0/com.ibm.websphere.wlp.core.doc/ae/twlp_extend_embed.html) in the Maven JVM. If the server is started in embedded mode, it must also be stopped in embedded mode. The default value is `false`. | No |

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
