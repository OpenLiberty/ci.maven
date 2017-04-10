#### clean-server
---
Deletes every file in the `${outputDirectory}/logs`, `${outputDirectory}/workarea`, `${userDirectory}/dropins` or `${userDirectory}/apps`.

###### Additional Parameters

| Parameter | Description | Required |
| --------- | ------------ | ----------|
| cleanLogs | Delete all the files in the `${outputDirectory}/logs` directory. The default value is `true`. | No |
| cleanWorkarea | Delete all the files in the `${outputDirectory}/workarea` directory. The default value is `true`. | No |
| cleanDropins | Delete all the files in the `${userDirectory}/dropins` directory. The default value is `false`. | No |
| cleanApps | Delete all the files in the `${userDirectory}/apps` directory. The default value is `false`. | No |

###### Examples

Remove every app deployed to the `${userDirectory}/dropins` and every file in the `${outputDirectory}/workarea` and `${outputDirectory}/logs` directories.
 
```xml
<project>
    ...
    <build>
        <plugins>
            <plugin>
                <groupId>net.wasdev.wlp.maven.plugins</groupId>
                <artifactId>liberty-maven-plugin</artifactId>
                <executions>
                    ...
                    <execution>
                        <id>clean</id>
                        <phase>post-integration-test</phase>
                        <goals>
                            <goal>clean-server</goal>
                        </goals>
                        <configuration>
                            <cleanDropins>true</dropins>
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
