#### deploy
---
Deploy or copy applications specified as either Maven compile dependencies or the Maven project package to Liberty server's `dropins` or `apps` directory. This goal can be used when the server is not running to copy applications onto the server, or when the server is running to deploy applications and verify that they have started. To install Spring Boot applications on Liberty see [Spring Boot Support](spring-boot-support.md#spring-boot-support).

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common parameters](common-parameters.md#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| appsDirectory | The server's `apps` or `dropins` directory where the application files should be copied. The default value is set to `apps` if the application is defined in the server configuration, otherwise it is set to `dropins`.  | No |
| copyDependencies | Copies the specified dependencies to the specified locations. Multiple `dependency` elements can be added to the `copyDependencies` configuration. The `location` element can be added to the `copyDependencies` or `dependency` configuration to override the default location, which is the `lib/global` folder of the target server. The `stripVersion` element can be added to the `copyDependencies` or `dependency` configuration to override the default `stripVersion` value, which is `false`. | No |
| copyLibsDirectory | The optional directory to which loose application dependencies referenced by the loose application configuration file are copied. For example, if you want loose application dependencies to be contained within the build directory, you could set this parameter to `target`. The loose application configuration file will reference this directory for the loose application dependencies instead of the local repository cache. Only applicable when `looseApplication` is set to `true`. | No |
| deployPackages | The Maven packages to copy to Liberty runtime's application directory. One of `dependencies`, `project` or `all`. The default is `project`.<br>For an ear type project, this parameter is ignored and only the project package is installed. | No |
| looseApplication | Generate a loose application configuration file representing the Maven project package and copy it to the Liberty server's `apps` or `dropins` directory. The default value is `true`. This parameter is ignored if `deployPackages` is set to `dependencies` or if the project packaging type is neither `war` nor `liberty-assembly`. When using the packaging type `liberty-assembly`, using a combination of `deployPackages` set to `all` or `project` and `looseApplication` set to `true` results in the installation of application code provided in the project without the need of adding additional goals to your POM file. | No |
| stripVersion | Strip artifact version when copying the application to Liberty runtime's application directory. The default value is `false`. | No |
| timeout | Maximum time to wait (in seconds) to verify that the deployment has completed successfully. The default value is 40 seconds. | No |

The `copyDependencies` parameter can contain a `location` parameter, a `stripVersion` parameter and multiple `dependency` elements with the following parameters.

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| filter | The Maven coordinates `groupId:artifactId:version` identifying the dependency to be copied. The `artifactId` and `version` are optional. If only `groupId` is specified for the `filter`, all resolved dependencies with a matching `groupId` are copied to the specified or default location along with their transitive dependencies. If `groupId:artifactId` is specified for the `filter`, the resolved dependency with a matching `groupId` and `artifactId` is copied to the specified or default location along with its transitive dependencies. The `artifactId` may also end with a `*` to match all artifacts that start with the specified string. The `version` should only be specified if the dependency is not configured in the Maven `dependencies` or Maven `dependencyManagement` section of the `pom.xml` file. | Yes |
| location | The optional directory to which the dependency is copied. This can be an absolute path, or a relative path to the target server configuration directory. | No |
| stripVersion | The optional boolean indicating whether to strip the artifact version when copying the dependency. | No |

When determining which resolved dependencies to copy for the `copyDependencies` configuration, only scopes compile, runtime and system are included. This ensures provided scope and test scope dependencies are not copied. Also, the `type` is defaulted to `jar`. If your scenario is more complex, consider using the `copy` or `copy-dependencies` goal in the `maven-dependency-plugin` instead.

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
                            <deployPackages>dependencies</deployPackages>
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
Copy the Maven project package plus dependencies configured with the `copyDependencies` parameter.
```xml
<project>
    <groupId>wasdev</groupId>
    <artifactId>SimpleServlet</artifactId>
    <version>1.0</version>
    <packaging>war</packaging>
    ...
    <dependencies>
        <dependency>
            <groupId>commons-logging</groupId>
            <artifactId>commons-logging</artifactId>
            <version>1.0.4</version>
            <scope>system</scope>
        </dependency>
        <dependency>
            <groupId>org.apache.derby</groupId>
            <artifactId>derby</artifactId>
            <version>10.15.2.0</version>
        </dependency>
        <dependency>
            <groupId>org.apache.derby</groupId>
            <artifactId>derbyclient</artifactId>
            <version>10.15.2.0</version>
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
                            <copyDependencies>
                                <dependency>
                                    <!-- copies the commons-logging:commons-logging:1.0.4 dependency plus transitive 
                                         dependencies to the default location ${server.config.dir}/lib/global -->
                                    <filter>commons-logging</filter>
                                </dependency>
                                <dependency>
                                    <!-- copies the org.apache.derby:derby:10.15.2.0 and org.apache.derby:derbyclient:10.15.2.0 
                                         dependencies plus transitive dependencies to the specified location 
                                         ${server.config.dir}/lib/global/derby and strips the version during the copy. -->
                                    <filter>org.apache.derby:derby*</filter>
                                    <location>lib/global/derby</location>
                                    <stripVersion>true</stripVersion>
                                </dependency>
                            </copyDependencies>
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
