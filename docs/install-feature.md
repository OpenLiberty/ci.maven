#### install-feature
---
Install one or more features packaged as Subsystem Archive (ESA) files to the Liberty runtime.

In Open Liberty and WebSphere Liberty runtime versions 18.0.0.2 and above, this goal can install features specified in the following ways:
* Maven artifacts from the project's `dependencies` section,
* `feature` names in the `features` configuration,
* features declared in the `server.xml` file, its `include` elements, and from additional configuration files in the `configDropins` directory.

In WebSphere Liberty runtime versions 18.0.0.1 and below, this goal will install features specified as `feature` names in the `features` configuration. To install the missing features declared in the `server.xml` file (including its `include` elements, and from additional configuration files in the `configDropins` directory), set the `acceptLicense` parameter to `true` but do not specify any `feature` names in the `features` configuration.

In Open Liberty runtime versions 18.0.0.1 and below, this goal will be skipped. A warning message will be displayed. The Open Liberty runtime versions 18.0.0.1 and below are bundled with all applicable features. There is no need to install or uninstall additional features.

###### Additional Parameters

The following parameters are supported by this goal in addition to the [common parameters](common-parameters.md#common-parameters). Place them within the `features` configuration element.

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| feature | Specify the location of the Subsystem archive to install. This can be an ESA file, an IBM-Shortname, or a Subsystem-SymbolicName of the Subsystem archive. The value can be a file name or a URL to the esa file. Multiple `feature` elements can be added to the `features` configuration. | No |
| acceptLicense | Automatically indicate acceptance of license terms and conditions. The default is `false`, so you must add this parameter to get features installed if it is required. | Required for runtime versions 18.0.0.1 and below, or for features that are not from Open Liberty. <p/> Not required for Open Liberty features on runtime versions 18.0.0.2 and above. |
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

2. Install features from Maven artifacts listed as dependencies in Liberty runtime versions 18.0.0.2 and above. Use the `io.openliberty.features` groupId for Open Liberty features, or the `com.ibm.websphere.appserver.features` groupId for WebSphere Liberty features.  The `features-bom` artifact in each groupId provides the bill of materials (BOM) for each release, and the BOM for WebSphere Liberty includes the BOM for Open Liberty.  Features listed as dependencies also provide Liberty API, SPI, and Java specification dependencies for compilation.

 * Install Open Liberty features:
 ```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.openliberty.features</groupId>
            <artifactId>features-bom</artifactId>
            <version>18.0.0.2</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>io.openliberty.features</groupId>
        <artifactId>jaxrs-2.1</artifactId>
        <type>esa</type>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>io.openliberty.features</groupId>
        <artifactId>jsonp-1.1</artifactId>
        <type>esa</type>
        <scope>provided</scope>
    </dependency>
</dependencies>

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
        </execution>
        ...
    </executions>
    <configuration>
       <installDirectory>/opt/ibm/wlp</installDirectory>
       <serverName>test</serverName>
    </configuration>
</plugin>
 ```
     
 * Install WebSphere Liberty features:
 ```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.ibm.websphere.appserver.features</groupId>
            <artifactId>features-bom</artifactId>
            <version>18.0.0.2</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>com.ibm.websphere.appserver.features</groupId>
        <artifactId>servlet-3.0</artifactId>
        <type>esa</type>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>io.openliberty.features</groupId>
        <artifactId>localConnector-1.0</artifactId>
        <type>esa</type>
        <scope>provided</scope>
    </dependency>
</dependencies>

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

3. Check the server.xml file and install any required features that are missing from the runtime.
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
