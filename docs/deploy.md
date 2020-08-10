#### deploy
---
Deploy or copy applications specified as either Maven compile dependencies or the Maven project package to Liberty server's `dropins` or `apps` directory. This goal can be used when the server is not running to copy applications onto the server, or when the server is running to deploy applications and verify that they have started. To install Spring Boot applications on Liberty see [Spring Boot Support](spring-boot-support.md#spring-boot-support).

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common parameters](common-parameters.md#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| appsDirectory | The server's `apps` or `dropins` directory where the application files should be copied. The default value is set to `apps` if the application is defined in the server configuration, otherwise it is set to `dropins`.  | No |
| copyDependencies | Copies the specified dependencies to the specified locations. Multiple `dependency` parameters and `dependencyGroup` parameters can be added to the `copyDependencies` configuration. The `location` parameter can be added to the `copyDependencies` or `dependencyGroup` configuration to override the default location, which is the `lib/global` folder of the target server. The `stripVersion` parameter can be added to the `copyDependencies` configuration to override the default `stripVersion` value, which is `false`. | No |
| copyLibsDirectory | The optional directory to which loose application dependencies referenced by the loose application configuration file are copied. For example, if you want loose application dependencies to be contained within the build directory, you could set this parameter to `target`. The loose application configuration file will reference this directory for the loose application dependencies instead of the local repository cache. Only applicable when `looseApplication` is set to `true`. | No |
| deployPackages | The Maven packages to copy to Liberty runtime's application directory. One of `dependencies`, `project` or `all`. The default is `project`.<br>For an ear type project, this parameter is ignored and only the project package is installed. | No |
| looseApplication | Generate a loose application configuration file representing the Maven project package and copy it to the Liberty server's `apps` or `dropins` directory. The default value is `true`. This parameter is ignored if `deployPackages` is set to `dependencies` or if the project packaging type is neither `war` nor `liberty-assembly`. When using the packaging type `liberty-assembly`, using a combination of `deployPackages` set to `all` or `project` and `looseApplication` set to `true` results in the installation of application code provided in the project without the need of adding additional goals to your POM file. | No |
| stripVersion | Strip artifact version when copying the application to Liberty runtime's application directory. The default value is `false`. | No |
| timeout | Maximum time to wait (in seconds) to verify that the deployment has completed successfully. The default value is 40 seconds. | No |

The `copyDependencies` parameter can contain the following parameters.

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| dependency | A collection of `dependency` parameters that specify the coordinate of the Maven dependency to copy. | Yes, only when `dependencyGroup` parameter is not set. |
| dependencyGroup | A collection of `dependencyGroup` parameters that can contain a `location` parameter to override the default location, and multiple `dependency` parameters. | Yes, only when `dependency` parameter is not set. |
| location | The optional directory to which the dependencies are copied. This can be an absolute path, or a relative path to the target server configuration directory. The default location is the `lib/global` folder of the target server.| No |
| stripVersion | The optional boolean indicating whether to strip the artifact version when copying the dependency. The default value is `false`.| No |

The `dependencyGroup` parameter within the `copyDependencies` can contain the following parameters.

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| dependency | A collection of `dependency` parameters that specify the coordinate of the Maven dependency to copy. | Yes |
| location | The optional directory to which the dependencies are copied. This can be an absolute path, or a relative path to the target server configuration directory. If not specified, the `location` from the `copyDependencies` is used.| No |

The `dependency` parameter within the `copyDependencies` or `dependencyGroup` can contain the following parameters.

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| groupId | The groupId of the Maven dependency to be copied. The `artifactId` and `version` are optional. If only `groupId` is specified, all resolved dependencies with a matching `groupId` are copied to the specified or default location along with their transitive dependencies. | Yes |
| artifactId | The artifactId of the Maven dependency to be copied. If an `artifactId` is specified, the resolved dependency with a matching `groupId` and `artifactId` is copied to the specified or default location along with its transitive dependencies. The `artifactId` may also end with a `*` to match all artifacts that have an `artifactId` that start with the specified string. | No |
| version | The version of the Maven dependency to be copied. You must specify the `version` for any dependency that is not configured in the Maven `dependencies` or Maven `dependencyManagement` section of the `pom.xml` file. | No |

When determining which resolved dependencies to copy for the `copyDependencies` configuration, only scopes compile, runtime, system and provided are included. This ensures test scope dependencies are not copied. Please note that dependencies with scope compile, runtime, or system will still be packaged within the application unless configured otherwise. If you do not want the dependency within the application, then consider removing the dependency from the Maven `dependencies` or Maven `dependencyManagement` section of the pom.xml and specify the full coordinate with `version` within a `dependency` in `copyDependencies`. Alternatively, you change the dependency scope to provided. The `type` is also defaulted to `jar`. If your scenario is more complex, consider using the `copy` or `copy-dependencies` goal in the `maven-dependency-plugin` instead.

When determining which resolved dependencies to copy for the `copyDependencies` configuration, only scopes compile, runtime, system and provided are included. This ensures test scope dependencies are not copied. Please note that dependencies with scope compile, runtime, or system will still be packaged within the application unless configured otherwise. If you do not want the dependency within the application, then consider removing the dependency from the Maven `dependencies` or Maven `dependencyManagement` section of the pom.xml and specify the full coordinate with `version` within a `dependency` in `copyDependencies`. Alternatively, you could change the dependency scope to provided. A dependency that is configured in `copyDependencies` with a `version` will be treated as a 'provided'-scoped dependency in calculating transitive dependencies. The `type` is also defaulted to `jar`. If your scenario is more complex, consider using the `copy` or `copy-dependencies` goal in the `maven-dependency-plugin` instead.

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
            <groupId>org.apache.derby</groupId>
            <artifactId>derby</artifactId>
            <version>10.15.2.0</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.apache.derby</groupId>
            <artifactId>derbyclient</artifactId>
            <version>10.15.2.0</version>
            <scope>provided</scope>
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
                                <stripVersion>true</stripVersion>
                                <!-- copies the commons-logging:commons-logging:1.0.4 dependency plus transitive dependencies
                                     to the default location lib/global folder of the target server and strips the version. -->
                                <dependency>
                                    <groupId>commons-logging</groupId>
                                    <artifactId>commons-logging</artifactId>
                                    <version>1.0.4</version>
                                </dependency>
                                <!-- copies the org.apache.derby:derby:10.15.2.0 and org.apache.derby:derbyclient:10.15.2.0 
                                     dependencies plus transitive dependencies to the lib/global/derby folder of the target 
                                     server and strips the version during the copy. -->
                                <dependencyGroup>
                                    <location>lib/global/derby</location>
                                    <dependency>
                                        <groupId>org.apache.derby</groupId>
                                        <artifactId>derby*</artifactId>
                                    </dependency>
                                </dependencyGroup>
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
