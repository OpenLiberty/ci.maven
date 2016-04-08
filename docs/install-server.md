#### install-server
---
Installs Liberty profile runtime. This goal is implicitly invoked by all the other plugin goals and usually does not need to be executed explicitly. However, there might be cases where explicit execution might be needed.

This goal only supports the [common parameters](common-parameters.md#common-parameters).

Example:
```xml
<plugin>
    <groupId>net.wasdev.wlp.maven.plugins</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <executions>
        ...
        <execution>
            <id>install-server</id>
            <phase>pre-integration-test</phase>
            <goals>
                <goal>install-server</goal>
            </goals>
            <configuration>
                <install>
                    <type>javaee7</type>
                </install>
            </configuration>
        </execution>
        ...
    </executions>
</plugin>
```
