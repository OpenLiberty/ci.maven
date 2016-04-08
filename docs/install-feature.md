#### install-feature
---
Install a feature packaged as a Subsystem Archive (esa) to the Liberty runtime.

To install the missing features declared in the `server.xml` file, don't specify any `feature` names in the `features` configuration (You still need to specify the `acceptLicense` parameter).

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common parameters](common-parameters.md#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| feature | Specify the location of the Subsystem archive to be used. This can be an esa file, an IBM-Shortname or a Subsystem-SymbolicName of the Subsystem archive. The value can be a file name or a URL to the esa file. | No |
| acceptLicense | Automatically indicate acceptance of license terms and conditions. | No |
| to | Specify where to install the feature. The feature can be installed to any configured product extension location, or as a user feature (usr, extension). If this option is not specified the feature will be installed as a user feature. | No |
| from | Specifies a single directory-based repository as the source of the assets. | No |

Examples:

1. Install specific features.
 ```xml
<plugin>
    <groupId>net.wasdev.wlp.maven.plugins</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <executions>
        ...
        <execution>
            <id>install-feature</id>
            <phase>pre-integration-test</phase>
            <goals>
                <goal>install-feature</goal>
            </goals>
            <configuration>
                <features>
                    <acceptLicense>true</acceptLicense>
                    <feature>mongodb-2.0</feature>
                    <feature>ejbLite-3.2</feature>
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

2. Check the server.xml file and install the missing features.
 ```xml
<plugin>
    <groupId>net.wasdev.wlp.maven.plugins</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <executions>
        ...
        <execution>
            <id>install-feature</id>
            <phase>pre-integration-test</phase>
            <goals>
                <goal>install-feature</goal>
            </goals>
            <configuration>
                <features>
                    <acceptLicense>true</acceptLicense>
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
