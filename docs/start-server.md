#### start-server
---
Start a Liberty Profile server in background. The server instance will be automatically created if it does not exist.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common server parameters](common-server-parameters.md#common-server-parameters) and the [common parameters](common-parameters.md#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| clean | Clean all cached information on server start up. The default value is `false`. | No |
| serverStartTimeout | Maximum time to wait (in seconds) to verify that the server has started. The default value is 30 seconds. | No |
| verifyTimeout | Maximum time to wait (in seconds) to verify that the applications have started. This timeout only has effect if the `applications` parameter is set. The default value is 30 seconds. | No |
| applications | A comma-separated list of application names to wait for during server start-up. | No |

Example:
```xml
<plugin>
    <groupId>net.wasdev.wlp.maven.plugins</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <executions>
        ...
        <execution>
            <id>start-server</id>
            <phase>pre-integration-test</phase>
            <goals>
                <goal>start-server</goal>
            </goals>
            <configuration>
                <verifyTimeout>60</verifyTimeout>
                <configFile>${project.build.testOutputDirectory}/wlp/server.xml</configFile>
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
