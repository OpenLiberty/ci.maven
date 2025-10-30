### Common Server Parameters

Additional parameters shared by all server-based goals.

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| serverXmlFile | Location of a server configuration file to be used by the instance.| No |
| configDirectory | Location of a server configuration directory to be used by the instance. Configuration files and folder structure will be copied to the target server. Files specified by other common server parameters will take precedence over files located in the configDirectory. The default value is `${basedir}/src/main/liberty/config`.| No |
| copyDependencies | Copies the specified dependencies to the specified locations. Multiple `dependency` parameters and `dependencyGroup` parameters can be added to the `copyDependencies` configuration. The `location` parameter can be added to the `copyDependencies` or `dependencyGroup` configuration to override the default location, which is the `lib/global` folder of the target server. The `stripVersion` parameter can be added to the `copyDependencies` or `dependencyGroup` configuration to override the default `stripVersion` value, which is `false`. | No |
| bootstrapProperties | List of bootstrap properties for the server instance. The backslashes will be converted to forward slashes. `bootstrapProperties` will take precedence over `bootstrapPropertiesFile`.| No |
| bootstrapPropertiesFile | Location of a bootstrap properties file to be used by the instance.| No |
| jvmOptions | List of JVM options for the server instance. `jvmOptions` will take precedence over `jvmOptionsFile`.| No |
| jvmOptionsFile | Location of a JVM options file to be used by the instance.| No |
| serverEnvFile | Location of a server environment file to be used by the instance.| No |
| mergeServerEnv | Merge the server environment properties from all specified sources with the default generated `server.env` file in the target server. Conflicts are resolved with the same precedence as the replacement policy when this attribute is set to `false`. The `liberty.env.{var}` Maven properties are highest precedence, followed by the `serverEnvFile` attribute, then the `server.env` file located in the `configDirectory`, and finally the default generated `server.env` file in the target server. The default value is `false`. | No |

#### Backward Compatibility

The following parameter names from version 2.x are still supported for backward compatibility, but the new parameter names listed above should be used:

| 2.x Parameter Name       | 3.0+ Parameter Name     |
| ------------------------ | ----------------------- |
| configFile | serverXmlFile |
| serverEnv | serverEnvFile |

See the [version 3.0 migration guide](version_3.0_differences.md) for more information about parameter name changes.

#### Copying dependencies with liberty-maven-plugin

The `copyDependencies` parameter can contain the following parameters.

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| dependency | A collection of `dependency` parameters that specify the coordinate of the Maven dependency to copy. | Yes, only when `dependencyGroup` parameter is not set. |
| dependencyGroup | A collection of `dependencyGroup` parameters that can contain a `location` parameter to override the default location, and multiple `dependency` parameters. | Yes, only when `dependency` parameter is not set. |
| location | The optional directory to which the dependencies are copied. This can be an absolute path, or a path relative to the target server configuration directory. The default location is the `lib/global` folder of the target server.| No |
| stripVersion | The optional boolean indicating whether to strip the artifact version when copying the dependency. The default value is `false`.| No |

The `dependencyGroup` parameter within the `copyDependencies` can contain the following parameters.

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| dependency | A collection of `dependency` parameters that specify the coordinate of the Maven dependency to copy. | Yes |
| location | The optional directory to which the dependencies are copied. This can be an absolute path, or a relative path to the target server configuration directory. If not specified, the `location` from the `copyDependencies` is used.| No |
| stripVersion | The optional boolean indicating whether to strip the artifact version when copying the dependency. If not specified, the `stripVersion` from the `copyDependencies` is used.| No |

The `dependency` parameter within the `copyDependencies` or `dependencyGroup` can contain the following parameters.

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| groupId | The groupId of the Maven dependency to be copied. The `artifactId` and `version` are optional. If only `groupId` is specified, all resolved dependencies with a matching `groupId` are copied to the specified or default location along with their transitive dependencies. | Yes |
| artifactId | The artifactId of the Maven dependency to be copied. If an `artifactId` is specified, the resolved dependency with a matching `groupId` and `artifactId` is copied to the specified or default location along with its transitive dependencies. The `artifactId` may also end with a `*` to match all artifacts that have an `artifactId` that start with the specified string. | No |
| type | The type of the Maven dependency to be copied. The default is `jar`. | No |
| version | The version of the Maven dependency to be copied. You must specify the `version` for any dependency that is not configured in the Maven `dependencies` or Maven `dependencyManagement` section of the `pom.xml` file. | No |
| classifier | The classifier of the Maven dependency to be copied. It is `null` by default. | No |

When determining which resolved dependencies to copy for the `copyDependencies` configuration, only scopes compile, runtime, system and provided are included. This ensures test scope dependencies are not copied. Please note that dependencies with scope compile, runtime, or system will still be packaged within the application unless configured otherwise. If you do not want the dependency within the application, then consider removing the dependency from the Maven `dependencies` or Maven `dependencyManagement` section of the pom.xml and specify the full coordinate with `version` within a `dependency` in `copyDependencies`. Alternatively, you could change the dependency scope to provided. A dependency that is configured in `copyDependencies` with a `version` will be treated as a 'provided'-scoped dependency in calculating transitive dependencies. The `type` is also defaulted to `jar`. If your scenario is more complex, consider using the `copy` or `copy-dependencies` goal in the `maven-dependency-plugin` instead.

Example of copying dependencies with the `copyDependencies` parameter:
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
                        <id>start-server</id>
                        <phase>pre-integration-test</phase>
                        <goals>
                            <goal>start</goal>
                        </goals>
                        <configuration>
                            <copyDependencies>
                                <!-- Copies the commons-logging:commons-logging:1.0.4 dependency plus transitive dependencies
                                     to the default location lib/global folder of the target server. This dependency was not
                                     defined in the Maven dependencies above which is why the version is specified here. -->
                                <dependency>
                                    <groupId>commons-logging</groupId>
                                    <artifactId>commons-logging</artifactId>
                                    <version>1.0.4</version>
                                </dependency>
                                <!-- Copies the org.apache.derby:derby:10.15.2.0 and org.apache.derby:derbyclient:10.15.2.0 
                                     dependencies plus transitive dependencies which were defined in the Maven dependencies
                                     above to the lib/global/derby folder of the target server and strips the version. -->
                                <dependencyGroup>
                                    <stripVersion>true</stripVersion>
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
            </plugin>
        </plugins>
    </build>
    ...
</project>
```

#### Setting Liberty configuration with Maven project properties

Starting with the 3.1 release of the liberty-maven-plugin, support is added to specify Liberty configuration with Maven properties. Use the following property name formats to update the desired Liberty configuration.

| Property name format | Content generated | File generated | Additional info                                                                                                                                                                                                                                        |
| -------------------- | ----------------- | -------------- |--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| liberty.bootstrap.{var} | var=value | bootstrap.properties | Merged with the `bootstrapProperties` parameter, but `bootstrapProperties` take precedence.                                                                                                                                                            |
| liberty.env.{var} | var=value | server.env | None                                                                                                                                                                                                                                                   |
| liberty.jvm.{var} | value | jvm.options | Merged with the `jvmOptions` parameter, but `jvmOptions` take precedence. Note that only the value is written to the file since JVM options do not all use the var=value format.                                                                       |
| liberty.var.{var} | `<variable name="var" value="value">` | liberty-plugin-variable-config.xml | The server configuration file is generated in the configDropins/overrides folder of the target server. If you are using [dev container mode](dev.md#devc-container-mode), ensure to copy the configDropins/overrides folder into your container image. |
| liberty.defaultVar.{var} | `<variable name="var" defaultValue="value">` | liberty-plugin-variable-config.xml | The server configuration file is generated in the configDropins/defaults folder of the target server.  If you are using [dev container mode](dev.md#devc-container-mode), ensure to copy the configDropins/defaults folder into your container image.  |

If Liberty configuration is specified with Maven properties, the above indicated files are created in the target Liberty server. By default there is no merging behavior for the Maven properties with files located in the `configDirectory` or the specific configuration file parameters such as `bootstrapPropertiesFile`, `jvmOptionsFile` and `serverEnvFile`. However, the `liberty.env.{var}` Maven properties can be merged with other configured `server.env` files by setting the `mergeServerEnv` parameter to `true`.   

As a special case when `mergeServerEnv` is `false`,  an existing `keystore_password` property in the default generated `server.env` file in the target server will be merged in if there is no `serverEnvFile` configured nor `server.env` file located in the `configDirectory`, and the `keystore_password` env var is not defined as a Maven property.

Note that properties specified with `-D` on the command line are also analyzed for the property name formats listed above and take precedence over Maven properties specified in the pom.xml.

Example of Liberty configuration with Maven properties:
```xml
<properties>
    <liberty.jvm.minHeap>-Xms512m</liberty.jvm.minHeap>
    <liberty.env.JAVA_HOME>/opt/ibm/java</liberty.env.JAVA_HOME>
    <liberty.var.someVariable1>someValue1</liberty.var.someVariable1>
    <liberty.defaultVar.someDefaultVar1>someDefaultValue1</liberty.defaultVar.someDefaultVar1>
</properties>
```

Example of Liberty configuration with parameters:
```xml
<plugin>
    <groupId>io.openliberty.tools</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <executions>
        ...
        <execution>
            <id>start-server</id>
            <phase>pre-integration-test</phase>
            <goals>
                <goal>start</goal>
            </goals>
            <configuration>
                <configDirectory>${project.build.testOutputDirectory}/wlp/configDir</configDirectory>
                <serverXmlFile>${project.build.testOutputDirectory}/wlp/server.xml</serverXmlFile>
                <bootstrapProperties>
                    <httpPort>8080</httpPort>
                </bootstrapProperties>
                <jvmOptions>
                    <param>-Xmx768m</param>
                </jvmOptions>
            </configuration>
        </execution>
        ...
    </executions>
</plugin>
```

#### Late property replacement

Maven does property replacement for `${...}` values in pom.xml before any plugin is run. Starting with the 3.8.3 release of the liberty-maven-plugin, an alternate syntax `@{...}` is supported which allows late replacement of properties when the plugin is executed. This enables properties that are modified by other plugins to be picked up correctly.

The alternate syntax is supported for Liberty configuration specified by Maven properties, as well as the `jvmOptions` and `bootstrapProperties` parameters.
