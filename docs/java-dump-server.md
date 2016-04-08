#### java-dump-server
---
Dump diagnostic information from the server JVM.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common server parameters](common-server-parameters.md#common-server-parameters) and the [common parameters](common-parameters.md#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| systemDump | Include system dump information. The default value is `false`. | No |
| heapDump | Include heap dump information. The default value is `false`. | No |

Example:
```xml
<plugin>
    <groupId>net.wasdev.wlp.maven.plugins</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <executions>
        ...
        <execution>
            <id>java-dump-server</id>
            <phase>post-integration-test</phase>
            <goals>
                <goal>java-dump-server</goal>
            </goals>
            <configuration>
                <heapDump>true</heapDump>
                <systemDump>true</systemDump>
            </configuration>
        </execution>
        ...
    </executions>
    <configuration>
       <installDirectory>/opt/ibm/wlp</installDirectory>
       <serverName>test</serverName>
    </configuration>
</plugin>
```
