#### deploy
---
Deploy or copy applications specified as either Maven compile dependencies or the Maven project package to Liberty server's `dropins` or `apps` directory. This goal can be used when the server is not running to copy applications onto the server, or when the server is running to deploy applications and verify that they have started. To install Spring Boot applications on Liberty see [Spring Boot Support](spring-boot-support.md#spring-boot-support).

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common parameters](common-parameters.md#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| appsDirectory | The server's `apps` or `dropins` directory where the application files should be copied. The default value is set to `apps` if the application is defined in the server configuration, otherwise it is set to `dropins`.  | No |
| stripVersion | Strip artifact version when copying the application to Liberty runtime's application directory. The default value is `false`. | No |
| deployPackages | The Maven packages to copy to Liberty runtime's application directory. One of `dependencies`, `project` or `all`. The default is `project`.<br>For an ear type project, this parameter is ignored and only the project package is installed. | No |
| looseApplication | Generate a loose application configuration file representing the Maven project package and copy it to the Liberty server's `apps` or `dropins` directory. The default value is `true`. This parameter is ignored if `deployPackages` is set to `dependencies` or if the project packaging type is neither `war` nor `liberty-assembly`. When using the packaging type `liberty-assembly`, using a combination of `deployPackages` set to `all` or `project` and `looseApplication` set to `true` results in the installation of application code provided in the project without the need of adding additional goals to your POM file. | No |
| copyLibsDirectory | The optional directory to which loose application dependencies are copied. For example, if you want loose application dependencies to be contained within the build directory, you could set this parameter to `target`. Only applicable when `looseApplication` is set to `true`. | No |
| timeout | Maximum time to wait (in seconds) to verify that the deployment has completed successfully. The default value is 40 seconds. | No |

Example:
Copy the Maven project dependencies.
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
                <groupId>io.openliberty.tools</groupId>
                <artifactId>liberty-maven-plugin</artifactId>
                <executions>
                    ...
                    <execution>
                        <id>deploy</id>
                        <phase>pre-integration-test</phase>
                        <goals>
                            <goal>deploy</goal>
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
Copy the Maven project package.
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
                <groupId>io.openliberty.tools</groupId>
                <artifactId>liberty-maven-plugin</artifactId>
                <executions>
                    ...
                    <execution>
                        <id>deploy</id>
                        <phase>pre-integration-test</phase>
                        <goals>
                            <goal>deploy</goal>
                        </goals>
                        <configuration>
                            <appsDirectory>apps</appsDirectory>
                            <stripVersion>true</stripVersion>
                            <deployPackages>project</deployPackages>
                            <looseApplication>true</looseApplication>
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
