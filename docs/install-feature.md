#### install-feature
---
Install one or more features packaged as Subsystem Archive (ESA) files to the Liberty runtime.

To install the missing features declared in the `server.xml` file, set the `acceptLicense` parameter to `true` but do not specify any `feature` names in the `features` configuration.

This goal is not supported if the Liberty runtime is installed from the Open Liberty runtime package, `io.openliberty:openliberty-runtime:17.0.0.3`. You will get a Maven build error since the `bin/installUtiltiy` command is removed from the Open Liberty runtime package. The Open Liberty runtime is always bundled with all applicable features and there isn't any need to install any additional feature.

###### Additional Parameters

The following parameters are supported by this goal in addition to the [common parameters](common-parameters.md#common-parameters). Place them within the `features` configuration element.

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| feature | Specify the location of the Subsystem archive to install. This can be an ESA file, an IBM-Shortname, or a Subsystem-SymbolicName of the Subsystem archive. The value can be a file name or a URL to the esa file. Multiple `feature` elements can be added to the `features` configuration. | No |
| acceptLicense | Automatically indicate acceptance of license terms and conditions. The default is `false`, so you must add this parameter to get features installed. | Yes |
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
            <phase>prepare-package</phase>
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

2. Check the server.xml file and install any required features that are missing from the runtime.
 ```xml
<plugin>
    <groupId>net.wasdev.wlp.maven.plugins</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <executions>
        ...
        <execution>
            <id>install-feature</id>
            <phase>prepare-package</phase>
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
