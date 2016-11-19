#### install-apps
---
Copy applications to Liberty server's `dropins` or `apps` directory. This includes applications specified as Maven compile dependencies for any Maven packaging type and the build artifact if the Maven packaging type is not `liberty-assembly`. Unlike the [deploy](deploy.md#deploy) goal, this goal only performs a simple copy operation. It does not require the server to be running and does not check if the application was successfully deployed. 

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common parameters](common-parameters.md#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| appsDirectory | The directory where the application files should be copied. The default value is `dropins`.  | No |
| stripVersion | Strip artifact version when copying the application to Liberty runtime's application directory. The default value is `false`. | No |

###### Examples:
Install app specified as dependencies:
```xml
<project>
    ...
    <dependencies>
        <!-- SimpleServlet.war specified as a dependency -->
        <dependency>
            <groupId>wasdev</groupId>
            <artifactId>SimpleServlet</artifactId>
            <version>1.0</version>
            <type>war</type>
        </dependency>
    </dependencies>
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
                            <goal>install-apps</goal>
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
Install build artifact:
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
                            <goal>install-apps</goal>
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
