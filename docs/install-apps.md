#### install-apps
---
Copy applications specified as either Maven compile dependencies or the Maven project package to Liberty server's `dropins` or `apps` directory. Unlike the [deploy](deploy.md#deploy) goal, this goal only performs a simple copy operation. It does not require the server to be running and does not check if the application was successfully deployed. To install Spring Boot applications on Liberty see [Spring Boot Support](spring-boot-support.md#spring-boot-support).

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common parameters](common-parameters.md#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| appsDirectory | The server's `apps` or `dropins` directory where the application files should be copied. The default value is set to `apps` if the application is defined in the server configuration, otherwise it is set to `dropins`.  | No |
| stripVersion | Strip artifact version when copying the application to Liberty runtime's application directory. The default value is `false`. | No |
| installAppPackages | The Maven packages to copy to Liberty runtime's application directory. One of `dependencies`, `project` or `all`. The default is `dependencies`.<br>For an ear type project, this parameter is ignored and only the project package is installed. | No |
| looseApplication | Generate a loose application configuration file representing the Maven project package and copy it to the Liberty server's `apps` or `dropins` directory. The default value is `false`. This parameter is ignored if `installAppPackages` is set to `dependencies` or if the project packaging type is neither `war` nor `liberty-assembly`. When using the packaging type `liberty-assembly`, using a combination of `installAppPackages` set to `all` or `project` and `looseApplication` set to `true` results in the installation of application code provided in the project without the need of adding additional goals to your POM file. | No |

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
                            <installAppPackages>project</installAppPackages>
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
