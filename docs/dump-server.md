#### dump-server
---
Dump diagnostic information from the server into an archive.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common server parameters](common-server-parameters.md#common-server-parameters) and the [common parameters](common-parameters.md#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| archive | Location of the target archive file. | No |
| systemDump | Include system dump information. The default value is `false`. | No |
| heapDump | Include heap dump information. The default value is `false`. | No |
| threadDump | Include thread dump information. The default value is `false`. | No |

Example:
```xml
<plugin>
    <groupId>net.wasdev.wlp.maven.plugins</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <executions>
        ...
        <execution>
            <id>dump-server</id>
            <phase>post-integration-test</phase>
            <goals>
                <goal>dump-server</goal>
            </goals>
            <configuration>
                <archive>${project.build.directory}/dump.zip</archive>
                <heapDump>true</heapDump>
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
