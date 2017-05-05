#### Common Server Parameters

Additional parameters shared by all server-based goals.

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| configFile | Location of a server configuration file to be used by the instance. The default value is `${basedir}/src/test/resources/server.xml`. | No |
| configDirectory | Location of a server configuration directory to be used by the instance. Configuration files and folder structure will be copied to target server. configDirectory files will take precedence over other common server parameters. | No |
| bootstrapProperties | List of bootstrap properties for the server instance. The backslashes will be converted to forward slashes. | No |
| bootstrapPropertiesFile | Location of a bootstrap properties file to be used by the instance. The default value is `${basedir}/src/test/resources/bootstrap.properties`. | No |
| jvmOptions | List of JVM options for the server instance. | No |
| jvmOptionsFile | Location of a JVM options file to be used by the instance. The default value is `${basedir}/src/test/resources/jvm.options`. | No |
| serverEnv | Location of a server environment file to be used by the instance. The default value is `${basedir}/src/test/resources/server.env` | No |

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
                <configFile>${project.build.testOutputDirectory}/wlp/server.xml</configFile>
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
