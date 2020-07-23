#### Common Server Parameters

Additional parameters shared by all server-based goals.

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| serverXmlFile | Location of a server configuration file to be used by the instance. This replaces the `configFile` parameter which is still supported for backwards compatibility.| No |
| configDirectory | Location of a server configuration directory to be used by the instance. Configuration files and folder structure will be copied to the target server. Files specified by other common server parameters will take precedence over files located in the configDirectory. The default value is `${basedir}/src/main/liberty/config`.| No |
| bootstrapProperties | List of bootstrap properties for the server instance. The backslashes will be converted to forward slashes. `bootstrapProperties` will take precedence over `bootstrapPropertiesFile`.| No |
| bootstrapPropertiesFile | Location of a bootstrap properties file to be used by the instance.| No |
| jvmOptions | List of JVM options for the server instance. `jvmOptions` will take precedence over `jvmOptionsFile`.| No |
| jvmOptionsFile | Location of a JVM options file to be used by the instance.| No |
| serverEnvFile | Location of a server environment file to be used by the instance. This replaces the `serverEnv` parameter which is still supported for backwards compatibility.| No |
| mergeServerEnv | Merge the server environment properties from all specified sources with the default generated `server.env` file in the target server. Conflicts are resolved with the same precedence as the replacement policy when this attribute is set to `false`. The properties specified in the `env` attribute are highest precedence, followed by the `serverEnvFile` attribute, then the `server.env` file located in the `configDirectory`, and finally the default generated `server.env` file in the target server. The default value is `false`. | No |

Starting with the 3.1 release of the liberty-maven-plugin, support is added to specify Liberty configuration with Maven properties. Use the following property name formats to update the desired Liberty configuration.

| Property name format | Content generated | File generated | Additional info |
| -------------------- | ----------------- | -------------- | --------------- |
| liberty.bootstrap.{var} | var=value | bootstrap.properties | Merged with the `bootstrapProperties` parameter, but `bootstrapProperties` take precedence. |
| liberty.env.{var} | var=value | server.env | None |
| liberty.jvm.{var} | value | jvm.options | Merged with the `jvmOptions` parameter, but `jvmOptions` take precedence. Note that only the value is written to the file since JVM options do not all use the var=value format.|
| liberty.var.{var} | `<variable name="var" value="value">` | liberty-plugin-variable-config.xml | The server configuration file is generated in the configDropins/overrides folder of the target server. |
| liberty.defaultVar.{var} | `<variable name="var" defaultValue="value">` | liberty-plugin-variable-config.xml | The server configuration file is generated in the configDropins/overrides folder of the target server. |

If Liberty configuration is specified with Maven properties, the above indicated files are created in the target Liberty server. There is no merging behavior for these Maven properties with files located in the `configDirectory` or the specific configuration file parameters including `bootstrapPropertiesFile`, `jvmOptionsFile`, and `serverEnvFile`.

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
