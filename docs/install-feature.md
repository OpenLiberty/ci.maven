#### install-feature
---
Install one or more features packaged as Subsystem Archive (ESA) files to the Liberty runtime.

In Open Liberty and WebSphere Liberty runtime versions 18.0.0.2 and above, this goal can install features specified in the following ways:
* Maven artifacts from the project's `dependencies` section,
* `feature` names in the `features` configuration,
* features declared in the `server.xml` file, its `include` elements, and from additional configuration files in the `configDropins` directory.

In WebSphere Liberty runtime versions 18.0.0.1 and below, this goal will install features specified as `feature` names in the `features` configuration. To install the missing features declared in the `server.xml` file (including its `include` elements, and from additional configuration files in the `configDropins` directory), set the `acceptLicense` parameter to `true` but do not specify any `feature` names in the `features` configuration.

In Open Liberty runtime versions 18.0.0.1 and below, this goal will be skipped. A warning message will be displayed. The Open Liberty runtime versions 18.0.0.1 and below are bundled with all applicable features. There is no need to install or uninstall additional features.

In Open Liberty runtime versions 21.0.0.11 and above, you can install custom user features. Check this [blog](https://openliberty.io/blog/2022/07/06/user-feature-install.html) on how to build and install user feature using Maven plug-ins.

###### Additional Parameters

The following parameters are supported by this goal in addition to the [common parameters](common-parameters.md#common-parameters). Place them within the `features` configuration element.

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| feature | Specify the location of the Subsystem archive to install. This can be a local ESA file, an IBM-Shortname, or a Subsystem-SymbolicName of the Subsystem archive. Multiple `feature` elements can be added to the `features` configuration. | No |
| acceptLicense | Automatically indicate acceptance of license terms and conditions. The default is `false`, so you must add this parameter to get features installed if it is required. | Required for runtime versions 18.0.0.1 and below, or for features that are not from Open Liberty. <p/> Not required for Open Liberty features on runtime versions 18.0.0.2 and above. |
| to | Specify where to install the feature. The feature can be installed to any configured product extension location, or as a user feature (usr, extension). If this option is not specified the feature will be installed as a user feature. | No |
| from | Specifies a single directory-based repository as the source of the assets. | No |
| verify | Specifies how features must be verified during a process or an installation. Supported values are `enforce`, `skip`, `all`, and `warn`. If this option is not specified, the default value is enforce. <ul><li>`enforce`: Verifies the signatures of all Liberty features except for user features. It checks the integrity and authenticity of the features that are provided by the Liberty framework.</li><li>`skip`: Choosing this option skips the verification process altogether. No feature signatures are downloaded or checked. It expedites the installation process but must be used with caution, as it bypasses an important security step.</li><li>`all`: Verifies both the Liberty features and the user features. The features that are provided by the Liberty framework and any additional user features or components are checked for integrity.</li><li>`warn`: Similar to the all option, warn also verifies both the Liberty features and user features. This option allows the process to continue, even if some feature signatures cannot be validated. A verification failure does not immediately end the installation process, but it results in a warning message.</li></ul> | No |

You can verify your user features by providing the long key ID and key URL to reference your public key that is stored on a key server. For more information about generating a key pair, signing the user feature, and distributing your key, see [Working with PGP Signatures](https://central.sonatype.org/publish/requirements/gpg/#signing-a-file).

Place the following parameters in `keys` configurations.
| Parameter | Description | Required |
| --------  | ----------- | -------  |
| keyid  | Provide the long key ID for your public key. The long key ID is a 64-bit identifier that is used to uniquely identify a PGP key. | No  |
| keyurl  | Provide the full URL of your public key. The URL must be accessible and point to a location where your key can be retrieved. The supported protocols for the key URL are `HTTP`, `HTTPS`, and `file`. | No  |

Examples:

1. Install specific features.
 ```xml
<plugin>
    <groupId>io.openliberty.tools</groupId>
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
    <groupId>io.openliberty.tools</groupId>
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
    <groupId>io.openliberty.tools</groupId>
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
    <groupId>io.openliberty.tools</groupId>
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

* Install user features:
1. To install user feature using either the IBM-Shortname or the Subsystem-SymbolicName as the name reference, you first need to run [prepare-feature](prepare-feature.md) task to generate a `features.json` file. To bypass this task, you need to specify the local esa path.

2. Keep the BOM dependency from the previous step for your user feature. To verify the signature of your user feature, provide the public key by specifying the long key ID and key URL. The user feature signature file must reside in the same directory as the corresponding feature esa file.
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>my.user.features</groupId>
            <artifactId>features-bom</artifactId>
            <version>1.0</version>
            <type>pom</type>
        </dependency>
    </dependencies>
</dependencyManagement>

<plugin>
    <groupId>io.openliberty.tools</groupId>
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
                <keys>
                    <key1>
                        <keyid>0xBD9FD5BE9E68CA00</keyid>
                        <keyurl>https://keyserver.ubuntu.com/pks/lookup?op=get&amp;options=mr&amp;search=0xBD9FD5BE9E68CA00<keyurl>
                    </key1>
                </keys>
                <features>
                    <acceptLicense>true</acceptLicense>
                    <feature>myUserFeature-1.0</feature>
                    <verify>all</verify>
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