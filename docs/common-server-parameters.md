#### Common Server Parameters

Additional parameters shared by all server-based goals.

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| serverXmlFile | Location of a server configuration file to be used by the instance.| No |
| configDirectory | Location of a server configuration directory to be used by the instance. Configuration files and folder structure will be copied to target server. Other common server parameters, if specified, will take precedence over files located in the configDirectory.| No |
| bootstrapProperties | List of bootstrap properties for the server instance. The backslashes will be converted to forward slashes. If specified, this takes precedence over `bootstrapPropertiesFile`.| No |
| bootstrapPropertiesFile | Location of a bootstrap properties file to be used by the instance.| No |
| jvmOptions | List of JVM options for the server instance. If specified, this takes precedence over `jvmOptionsFile`.| No |
| jvmOptionsFile | Location of a JVM options file to be used by the instance.| No |
| serverEnvFile | Location of a server environment file to be used by the instance.| No |

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
                <serverXmlFile>${project.build.testOutputDirectory}/wlp/server.xml</serverXml>
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
