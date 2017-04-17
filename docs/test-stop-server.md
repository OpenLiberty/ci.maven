#### test-start-server
---
Provides a bypass for checking for a server to stop if the server start step has been skipped.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [stop-server](stop-server.md#stop-server), the [common server parameters](common-server-parameters.md#common-server-parameters), and the [common parameters](common-parameters.md#common-parameters).

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
```