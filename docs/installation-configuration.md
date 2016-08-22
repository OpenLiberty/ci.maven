### Liberty installation configuration
#### Using an existing installation

Use the `installDirectory` parameter to specify the directory of an existing Liberty profile server installation. For example:
```xml
<plugin>
    <groupId>net.wasdev.wlp.maven.plugins</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <configuration>
        <installDirectory>/opt/ibm/wlp</installDirectory>
    </configuration>
</plugin>
```

#### Using a packaged server

Use the `assemblyArchive` parameter to specify a packaged server archive (created using `server package` command) that contains Liberty profile server files. For example:
```xml
<plugin>
    <groupId>net.wasdev.wlp.maven.plugins</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <configuration>
        <assemblyArchive>/opt/ibm/wlp.zip</assemblyArchive>
    </configuration>
</plugin>
```

#### Using Maven artifact

Use the `assemblyArtifact` parameter to specify the name of the Maven artifact that contains a custom Liberty profile server or use one of the provided on the [Maven Central repository](http://search.maven.org/). 

The Maven Central repository includes the following Liberty runtime artifacts:

|Artifact ID | Versions | Description |
| --- | ----------------- | ----------- |
| [wlp-javaee7](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-javaee7/) | 16.0.0.2, 8.5.5.9, 8.5.5.8, 8.5.5.7, 8.5.5.6 | Liberty runtime with all Java EE 7 Full Platform features. |
| [wlp-webProfile7](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-webProfile7/) | 16.0.0.2, 8.5.5.9, 8.5.5.8, 8.5.5.7, 8.5.5.6 | Liberty runtime with Java EE 7 Web Profile features. |
| [wlp-kernel](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-kernel/) | 16.0.0.2, 8.5.5.9, 8.5.5.8 | Liberty runtime kernel. |
| [wlp-osgi](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-osgi/) | 16.0.0.2, 8.5.5.9, 8.5.5.8 | Liberty runtime with features that support OSGi applications. |


Note: The group ID for these artifacts is: `com.ibm.websphere.appserver.runtime`.

Example for using the `assemblyArtifact` parameter:
```xml
<plugin>
    <groupId>net.wasdev.wlp.maven.plugins</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <configuration>
        <assemblyArtifact>
            <groupId>com.ibm.websphere.appserver.runtime</groupId>
            <artifactId>wlp-webProfile7</artifactId>
            <version>8.5.5.7</version>
            <type>zip</type>
        </assemblyArtifact>
    </configuration>
</plugin>
```

#### Using a repository

Use the `install` parameter to download and install Liberty profile server from the [Liberty repository](https://developer.ibm.com/wasdev/downloads/) or other location.

In certain cases, the Liberty license code may need to be provided in order to install the runtime. If the license code is required and if you are installing Liberty from the Liberty repository, you can obtain the license code by reading the [current license](https://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/wasdev/downloads/wlp/16.0.0.2/lafiles/runtime/en.html) and looking for the `D/N: <license code>` line. Otherwise, download the Liberty runtime archive and execute `java -jar wlp*runtime.jar --viewLicenseInfo` command and look for the `D/N: <license code>` line.

* Install from the Liberty repository. The plugin will use the Liberty repository to find the Liberty runtime archive to install based on the given version and type. This is the default installation method - no extra configuration is required. By default, the latest Liberty runtime with the Java EE 7 Web Profile features will be installed.
 ```xml
    <plugin>
        <groupId>net.wasdev.wlp.maven.plugins</groupId>
        <artifactId>liberty-maven-plugin</artifactId>
    </plugin>
 ```

* Install Liberty runtime with Java EE 6 Web Profile features from the Liberty repository (must provide `licenseCode`).
 ```xml
    <plugin>
        <groupId>net.wasdev.wlp.maven.plugins</groupId>
        <artifactId>liberty-maven-plugin</artifactId>
        <configuration>
            <install>
                <type>webProfile6</type>
                <licenseCode><license code></licenseCode>
            </install>
        </configuration>
    </plugin>
 ```

* Install from a given location. The `runtimeUrl` sub-parameter specifies a location of the Liberty profile's runtime `.jar` or `.zip` file to install. The `licenseCode` is only needed when installing from `.jar` file.
 ```xml
    <plugin>
        <groupId>net.wasdev.wlp.maven.plugins</groupId>
        <artifactId>liberty-maven-plugin</artifactId>
        <configuration>
            <install>
                <runtimeUrl><url to .jar or .zip file></runtimeUrl>
                <licenseCode><license code></licenseCode>
            </install>
        </configuration>
    </plugin>
 ```

The `install` parameter has the following sub-parameters:

| Name | Description | Required |
| --------  | ----------- | -------  |
| licenseCode | Liberty profile license code. See [above](#using-a-repository). | Yes, if `type` is `webProfile6` or `runtimeUrl` specifies a `.jar` file. |
| version | Exact or wildcard version of the Liberty profile server to install. Available versions are listed in the [index.yml](http://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/wasdev/downloads/wlp/index.yml) file. Only used if `runtimeUrl` is not set. The default value is `8.5.+`. | No |
| type | Liberty runtime type to download from the Liberty repository. Currently, the following types are supported: `kernel`, `webProfile6`, `webProfile7`, and `javaee7`. Only used if `runtimeUrl` is not set. Defaults to `webProfile6` if `licenseCode` is set and `webProfile7` otherwise. | No |
| runtimeUrl | URL to the Liberty profile's runtime `.jar` or a `.zip` file. If not set, the Liberty repository will be used to find the Liberty runtime archive. | No |
| cacheDirectory | The directory used for caching downloaded files such as the license or `.jar` files. The default value is `${settings.localRepository}/wlp-cache`. | No |
| username | Username needed for basic authentication. | No |
| password | Password needed for basic authentication. | No |
| serverId | Id of the `server` definition with the username and password in the `~/.m2/settings.xml` file. Used for basic authentication. | No |
| maxDownloadTime | Maximum time in seconds the download can take. The default value is `0` (no maximum time). | No |

---
