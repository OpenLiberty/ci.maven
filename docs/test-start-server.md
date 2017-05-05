#### test-start-server
---

Allows test suite to skip the start and stop of the server via pom configuration or command line arguments.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common server parameters](common-server-parameters.md#common-server-parameters) and the [common parameters](common-parameters.md#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| testSkipServer | Skip the start and stop of server when testing. The default value is `false`. | No |

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
                <goal>test-start-server</goal>
            </goals>
            <configuration>
                <testSkipServer>true</testSkipServer>
            </configuration>
        </execution>
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
=======
Start a Liberty server in background. The server instance will be automatically created if it does not exist. `test-start-server` honors the `skipTests`, `skipITs`, and `maven-test-skip` properties. It also allows you to bypass automatically starting the server during the pre-integration-test phase with pom configuration or a Liberty-specific command line argument.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [start-server](start-server.md#start-server), the [common server parameters](common-server-parameters.md#common-server-parameters), and the [common parameters](common-parameters.md#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| skipTestServer | Skip the start and stop of server when testing. The default value is `false`. | No |

Example:
```xml
<project>
    ...
    <groupId>myGroup</groupId>
    <artifactId>myServer</artifactId>
    <!-- Create Liberty profile server assembly -->
    <packaging>liberty-assembly</packaging>
    ...
    <build>
        <plugins>
            <!-- Enable liberty-maven-plugin -->
            <plugin>
                <groupId>net.wasdev.wlp.maven.plugins</groupId>
                <artifactId>liberty-maven-plugin</artifactId>
                <version>2.0</version>
                <extensions>true</extensions>
                <configuration>
                    <skipTestServer>true</skipTestServer>
                </configuration>
            </plugin>
        </plugins>
    </build>
    ...
</project>
>>>>>>> 11f88751ab1467408de11ead770731b1ad129799
```
