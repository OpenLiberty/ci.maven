#### export-plugin-config
---
This goal is to export the effective configuration settings for `liberty-maven-plugin` from the Maven project to a file called `liberty-plugin-confiug.xml` in the project `target` directory.

When the maven project is imported into IBM WebSphere Application Server Developer Tools for Eclipse, the presence of `liberty-plugin-confiug.xml` will trigger the Liberty runtime environment and the server instance to be created. 

Example:
```xml
<pluginManagement>
    <plugins>
        <plugin>
            <groupId>net.wasdev.wlp.maven.plugins</groupId>
            <artifactId>liberty-maven-plugin</artifactId>
            <executions>
                <execution>
                    <id>generate-plugin-config</id>
                    <phase>compile</phase>
                    <goals>
                        <goal>export-plugin-config</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</pluginManagement>

```
