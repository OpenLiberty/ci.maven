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

Example:
```xml
<plugin>
    <groupId>net.wasdev.wlp.maven.plugins</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <executions>
        ...
        <execution>
            <id>start-server</id>
            <phase>pre-integration-test</phase>
            <goals>
                <goal>start-server</goal>
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
