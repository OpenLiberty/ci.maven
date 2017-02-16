#### create-server
---
Create a Liberty Profile server.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common server parameters](common-server-parameters.md#common-server-parameters) and the [common parameters](common-parameters.md#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| template | Name of the template to use when creating a new server. | No |
| configDirectory | Location of a server configuration directory to be used by the instance. Configuration 
files and folder structure will be copied to target server. configDirectory files will take precedence over other common server parameters. | No |

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
       <configDirectory>${project.build.testOutputDirectory}/testConfigDirectory</configDirectory>
    </configuration>
</plugin>
```
