### Liberty installation configuration

### Using a Maven artifact

Use the `runtimeArtifact` parameter to specify the name of the Maven artifact that contains a custom Liberty server or use one of the provided on the [Maven Central repository](http://search.maven.org/). This is the default installation method. The default runtime artifact is the latest version of `io.openliberty:openliberty-kernel`. 

The Maven Central repository includes the following Liberty runtime artifacts. Versions for each artifact can be found at the specified link.

| Group ID : Artifact ID | Description |
| ---------------------- | ----------- |
| [`io.openliberty:openliberty-runtime`](https://repo1.maven.org/maven2/io/openliberty/openliberty-runtime/)  | Open Liberty runtime. |
| [`io.openliberty:openliberty-javaee8`](https://repo1.maven.org/maven2/io/openliberty/openliberty-javaee8/)  | Open Liberty runtime with all Java EE 8 Full Platform features. |
| [`io.openliberty:openliberty-webProfile8`](https://repo1.maven.org/maven2/io/openliberty/openliberty-webProfile8/)  | Open Liberty runtime with Java EE 8 Web Profile features. |
| [`io.openliberty:openliberty-kernel`](https://repo1.maven.org/maven2/io/openliberty/openliberty-kernel/)  | Open Liberty runtime kernel. |
| [`com.ibm.websphere.appserver.runtime:wlp-javaee8`](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-javaee8/)  | WebSphere Liberty runtime with all Java EE 8 Full Platform features. |
| [`com.ibm.websphere.appserver.runtime:wlp-javaee7`](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-javaee7/)  | WebSphere Liberty runtime with all Java EE 7 Full Platform features. |
| [`com.ibm.websphere.appserver.runtime:wlp-webProfile8`](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-webProfile8/) | WebSphere Liberty runtime with Java EE 8 Web Profile features. |
| [`com.ibm.websphere.appserver.runtime:wlp-webProfile7`](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-webProfile7/) | WebSphere Liberty runtime with Java EE 7 Web Profile features. |
| [`com.ibm.websphere.appserver.runtime:wlp-kernel`](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-kernel/)  | WebSphere Liberty runtime kernel. |
| [`com.ibm.websphere.appserver.runtime:wlp-osgi`](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-osgi/) |  WebSphere Liberty runtime with features that support OSGi applications. |
| [`com.ibm.websphere.appserver.runtime:wlp-microProfile1`](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-microProfile1/) | WebSphere Liberty with features for a MicroProfile runtime. |

Example for using the `runtimeArtifact` parameter:

```xml
<plugin>
    <groupId>io.openliberty.tools</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <configuration>
        <runtimeArtifact>
            <groupId>com.ibm.websphere.appserver.runtime</groupId>
            <artifactId>wlp-webProfile7</artifactId>
            <version>8.5.5.7</version>
            <type>zip</type>
        </runtimeArtifact>
    </configuration>
</plugin>
```

### Using an existing installation

Use the `installDirectory` parameter to specify the directory of an existing Liberty server installation. For example:

```xml
<plugin>
    <groupId>io.openliberty.tools</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <configuration>
        <installDirectory>/opt/ibm/wlp</installDirectory>
    </configuration>
</plugin>
```

### Using a packaged server

Use the `runtimeArchive` parameter to specify a packaged server archive (created using `server package` command) that contains Liberty server files. For example:

```xml
<plugin>
    <groupId>io.openliberty.tools</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <configuration>
        <runtimeArchive>/opt/ibm/wlp.zip</runtimeArchive>
    </configuration>
</plugin>
```

### Using the `install-liberty` Ant Task

Use the `install` parameter to configure a runtime installation using the `install-liberty` Ant task. The Ant task allows you to install a Liberty runtime from a specified location (via `runtimeUrl`) or automatically resolve it from the [Wasdev Liberty repository](https://developer.ibm.com/wasdev/downloads/) or [Open Liberty repository](https://openliberty.io/downloads/) based on a version and a runtime type. 

For full documentation of the usage and parameters, please read the [install-liberty](https://github.com/WASdev/ci.ant/blob/main/docs/install-liberty.md) Ant task documentation.

#### Ant Task Usage Examples

* Install from the Open Liberty repository. The plugin will use the Open Liberty repository to find the Open Liberty runtime archive to install based on the given version and type. This is the default installation method - no extra configuration is required. By default, the latest Open Liberty runtime will be installed.

 ```xml
    <plugin>
        <groupId>io.openliberty.tools</groupId>
        <artifactId>liberty-maven-plugin</artifactId>
    </plugin>
 ```

* Install the latest WebSphere Liberty runtime with Java EE 7 Web Profile features from the Wasdev Liberty repository.

 ```xml
    <plugin>
        <groupId>io.openliberty.tools</groupId>
        <artifactId>liberty-maven-plugin</artifactId>
        <configuration>
            <install>
                <type>webProfile7</type>
                <useOpenLiberty>false</useOpenLiberty>
            </install>
        </configuration>
    </plugin>
 ```

* Install from a given location. The `runtimeUrl` sub-parameter specifies a location of the Liberty runtime `.jar` or `.zip` file to install. The `licenseCode` is only needed when installing from `.jar` file.

 ```xml
    <plugin>
        <groupId>io.openliberty.tools</groupId>
        <artifactId>liberty-maven-plugin</artifactId>
        <configuration>
            <install>
                <runtimeUrl><url to .jar or .zip file></runtimeUrl>
                <licenseCode><license code></licenseCode>
            </install>
        </configuration>
    </plugin>
 ```