#### uninstall-feature
---
Uninstall a feature from the Liberty runtime.

This goal will be skipped in versions of the Open Liberty runtime that do not include `bin/installUtility`. A warning message will be displayed. The Open Liberty runtime versions 18.0.0.1 and below are bundled with all applicable features, so there is no need to install or uninstall additional features. In version 18.0.0.2, Open Liberty is available as different [runtime artifacts](installation-configuration.md#using-maven-artifact) with their corresponding features and does not support uninstalling features.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common parameters](common-parameters.md#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| feature | Specify the feature name to be uninstalled. This can be an IBM-Shortname or a Subsystem-SymbolicName of the Subsystem archive. | Yes |

Example:
```xml
<plugin>
    <groupId>net.wasdev.wlp.maven.plugins</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <executions>
        ...
        <execution>
            <id>install-feature</id>
            <phase>post-integration-test</phase>
            <goals>
                <goal>uninstall-feature</goal>
            </goals>
            <configuration>
                <features>
                    <feature>mongodb-2.0</feature>
                </features>
            </configuration>
        </execution>
        ...
    </executions>
    <configuration>
       <installDirectory>/opt/ibm/wlp</installDirectory>
       <serverName>test</serverName>
    </configuration>
</plugin>
```
