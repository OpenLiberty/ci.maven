#### test-start
---
Start a Liberty server in background. The server instance will be automatically created if it does not exist. `test-start` honors the `skipTests`, `skipITs`, and `maven-test-skip` properties. It also allows you to bypass automatically starting the server during the pre-integration-test phase with pom configuration or a Liberty-specific command line argument.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the parameters inherited from the [start](start.md#start) goal, the [common server parameters](common-server-parameters.md#common-server-parameters), and the [common parameters](common-parameters.md#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| skipTestServer | Skip the start and stop of server when testing. The default value is `false`. | No |

Example:
```xml
<project>
    ...
    <groupId>myGroup</groupId>
    <artifactId>myServer</artifactId>
    <!-- Create Liberty server assembly -->
    <packaging>liberty-assembly</packaging>
    ...
    <build>
        <plugins>
            <!-- Enable liberty-maven-plugin -->
            <plugin>
                <groupId>io.openliberty.tools</groupId>
                <artifactId>liberty-maven-plugin</artifactId>
                <version>[3.0.1,)</version>
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
