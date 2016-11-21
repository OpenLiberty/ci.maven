#### install-artifact
---
Copy the build artifact of the project to Liberty server's `dropins` or `apps` directory. The build artifact is determined by the Maven packaging type. Unlike the [deploy](deploy.md#deploy) goal, this goal only performs a simple copy operation. It does not require the server to be running and does not check if the artifact was successfully deployed as an application. 

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common parameters](common-parameters.md#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| appsDirectory | The directory where the artifact should be copied. The default value is `dropins`.  | No |
| stripVersion | Strip artifact version when copying the artifact to Liberty runtime's application directory. The default value is `false`. | No |

Example:
```xml
<project>
    <groupId>wasdev</groupId>
    <artifactId>SimpleServlet</artifactId>
    <version>1.0</version>
    <packaging>war</packaging>
    ...
    <build>
        <plugins>
            <plugin>
                <groupId>net.wasdev.wlp.maven.plugins</groupId>
                <artifactId>liberty-maven-plugin</artifactId>
                <executions>
                    ...
                    <execution>
                        <id>install-apps</id>
                        <phase>pre-integration-test</phase>
                        <goals>
                            <goal>install-artifact</goal>
                        </goals>
                        <configuration>
                            <appsDirectory>apps</appsDirectory>
                            <stripVersion>true</stripVersion>
                        </configuration>
                    </execution>
                    ...
                </executions>
                <configuration>
                   <installDirectory>/opt/ibm/wlp</installDirectory>
                   <serverName>test</serverName>
                </configuration>
            </plugin>
        </plugins>
    </build>
    ...
</project>
```
