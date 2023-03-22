### Liberty installation configuration

### Using a Maven artifact

Use the `runtimeArtifact` parameter to specify the name of the Maven artifact that contains a custom Liberty server or use one of the provided artifacts on the [Maven Central repository](http://search.maven.org/). This is the default installation method. The default runtime artifact is the latest version of `io.openliberty:openliberty-kernel`. 

The Maven Central repository includes the following Liberty runtime artifacts. Versions for each artifact can be found at the specified link.

| Group ID : Artifact ID | Description |
| ---------------------- | ----------- |
| [`io.openliberty:openliberty-kernel`](https://repo1.maven.org/maven2/io/openliberty/openliberty-kernel/)  | Open Liberty runtime kernel. |
| [`io.openliberty:openliberty-runtime`](https://repo1.maven.org/maven2/io/openliberty/openliberty-runtime/)  | Open Liberty runtime. |
| [`io.openliberty:openliberty-jakartaee9`](https://repo1.maven.org/maven2/io/openliberty/openliberty-jakartaee9/)  | Open Liberty runtime with all Jakarta EE 9 features. |
| [`io.openliberty:openliberty-javaee8`](https://repo1.maven.org/maven2/io/openliberty/openliberty-javaee8/)  | Open Liberty runtime with all Java EE 8 Full Platform features. |
| [`io.openliberty:openliberty-webProfile9`](https://repo1.maven.org/maven2/io/openliberty/openliberty-webProfile9/)  | Open Liberty runtime with Jakarta EE 9 Web Profile features. |
| [`io.openliberty:openliberty-webProfile8`](https://repo1.maven.org/maven2/io/openliberty/openliberty-webProfile8/)  | Open Liberty runtime with Java EE 8 Web Profile features. |
| [`io.openliberty:openliberty-microProfile5`](https://repo1.maven.org/maven2/io/openliberty/openliberty-microProfile5/)  | Open Liberty runtime with features for a MicroProfile 5 runtime. |
| [`io.openliberty:openliberty-microProfile4`](https://repo1.maven.org/maven2/io/openliberty/openliberty-microProfile4/)  | Open Liberty runtime with features for a MicroProfile 4 runtime. |
| [`io.openliberty:openliberty-microProfile3`](https://repo1.maven.org/maven2/io/openliberty/openliberty-microProfile3/)  | Open Liberty runtime with features for a MicroProfile 3 runtime. |
| [`io.openliberty.beta:openliberty-runtime `](https://repo1.maven.org/maven2/io/openliberty/beta/openliberty-runtime/)  | Open Liberty runtime beta. |
| [`com.ibm.websphere.appserver.runtime:wlp-jakartaee9`](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-jakartaee9/)  | WebSphere Liberty runtime with all Jakarta EE 9 features. |
| [`com.ibm.websphere.appserver.runtime:wlp-javaee8`](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-javaee8/)  | WebSphere Liberty runtime with all Java EE 8 Full Platform features. |
| [`com.ibm.websphere.appserver.runtime:wlp-javaee7`](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-javaee7/)  | WebSphere Liberty runtime with all Java EE 7 Full Platform features. |
| [`com.ibm.websphere.appserver.runtime:wlp-webProfile9`](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-webProfile9/) | WebSphere Liberty runtime with Jakarta EE 9 Web Profile features. |
| [`com.ibm.websphere.appserver.runtime:wlp-webProfile8`](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-webProfile8/) | WebSphere Liberty runtime with Java EE 8 Web Profile features. |
| [`com.ibm.websphere.appserver.runtime:wlp-webProfile7`](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-webProfile7/) | WebSphere Liberty runtime with Java EE 7 Web Profile features. |
| [`com.ibm.websphere.appserver.runtime:wlp-kernel`](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-kernel/)  | WebSphere Liberty runtime kernel. |
| [`com.ibm.websphere.appserver.runtime:wlp-osgi`](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-osgi/) |  WebSphere Liberty runtime with features that support OSGi applications. |
| [`com.ibm.websphere.appserver.runtime:wlp-microProfile2`](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-microProfile2/) | WebSphere Liberty with features for a MicroProfile 2 runtime. |
| [`com.ibm.websphere.appserver.runtime:wlp-microProfile1`](https://repo1.maven.org/maven2/com/ibm/websphere/appserver/runtime/wlp-microProfile1/) | WebSphere Liberty with features for a MicroProfile 1 runtime. |

Example for using the `runtimeArtifact` parameter:

```xml
<plugin>
    <groupId>io.openliberty.tools</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <configuration>
        <runtimeArtifact>
            <groupId>com.ibm.websphere.appserver.runtime</groupId>
            <artifactId>wlp-webProfile8</artifactId>
            <version>22.0.0.3</version>
            <type>zip</type>
        </runtimeArtifact>
    </configuration>
</plugin>
```

The coordinates for `runtimeArtifact` can be overridden using `libertyRuntimeGroupId`, `libertyRuntimeArtifactId`, and `libertyRuntimeVersion`. These can be set using command line properties, pom.xml properties, or additional plugin configuration. Empty or `null` values will result in a default value overriding the respective `runtimeArtifact` coordinate value. More information on these properties can be found in [common parameters](common-parameters.md#common-parameters).

Example of overriding the `runtimeArtifact` parameter through the command line:

```
mvn clean install -Dliberty.runtime.groupId=io.openliberty -Dliberty.runtime.artifactId=openliberty-runtime -Dliberty.runtime.version=21.0.0.9
```

Example of overriding the `runtimeArtifact` parameter with pom properties:

```xml
<properties>
    <liberty.runtime.groupId>io.openliberty</liberty.runtime.groupId>
    <liberty.runtime.artifactId>openliberty-runtime</liberty.runtime.artifactId>
    <liberty.runtime.version>21.0.0.9</liberty.runtime.version>
</properties>

<plugin>
    <groupId>io.openliberty.tools</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <configuration>
        <runtimeArtifact>
            <groupId>com.ibm.websphere.appserver.runtime</groupId>
            <artifactId>wlp-webProfile8</artifactId>
            <version>21.0.0.3</version>
            <type>zip</type>
        </runtimeArtifact>
    </configuration>
</plugin>
```

Example of overriding the `runtimeArtifact` parameter with plugin configuration:

```xml
<plugin>
    <groupId>io.openliberty.tools</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <configuration>
        <libertyRuntimeGroupId>io.openliberty</libertyRuntimeGroupId>
        <libertyRuntimeArtifactId>openliberty-runtime</libertyRuntimeArtifactId>
        <libertyRuntimeVersion>21.0.0.9</libertyRuntimeVersion>
        <runtimeArtifact>
            <groupId>com.ibm.websphere.appserver.runtime</groupId>
            <artifactId>wlp-webProfile8</artifactId>
            <version>21.0.0.3</version>
            <type>zip</type>
        </runtimeArtifact>
    </configuration>
</plugin>
```

#### Calculating the runtime artifact

The `runtimeArtifact` configuration is very flexible. It includes the use of properties that can be specified on the command line or in the `pom.xml` file, configuration parameters, use of dependencies and dependencyManagement, as well as default values. The `groupId`, `artifactId` and `version` are determined as follows in order:

1. groupId
* Check for the `liberty.runtime.groupId` property from the command line or `pom.xml` file.
* Check for the `libertyRuntimeGroupId` configuration parameter in the `pom.xml` file.
* Use the one specified in `runtimeArtifact`.
* Default to `io.openliberty`.

2. artifactId
* Check for the `liberty.runtime.artifactId` property from the command line or `pom.xml` file.
* Check for the `libertyRuntimeArtifactId` configuration parameter in the `pom.xml` file.
* Use the one specified in `runtimeArtifact`.
* Default to `openliberty-kernel`.

3. version
* Check for the `liberty.runtime.version` property from the command line or `pom.xml` file.
* Check for the `libertyRuntimeVersion` configuration parameter in the `pom.xml` file.
* Use the one specified in `runtimeArtifact`.
* Use the version from a matching `dependency`, if found, for the calculated `groupId:artifactId` from steps 1 and 2.
* Use the version from a matching `dependencyManagement`, if found, for the calculated `groupId:artifactId` from steps 1 and 2.
* Default to the latest available using a range like `[22.0.0.3,)`.

##### Example

Example "resolving" the `runtimeArtifact` version from matching `dependencyManagement` configuration:

```xml
<dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>io.openliberty</groupId>
        <artifactId>openliberty-runtime</artifactId>
        <version>22.0.0.2</version>
        <type>zip</type>
      </dependency>
    </dependencies>
</dependencyManagement>

<plugins>  
   <plugin>
    <groupId>io.openliberty.tools</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <configuration>
        <runtimeArtifact>
            <groupId>io.openliberty</groupId>
            <artifactId>openliberty-runtime</artifactId>
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

### Using a repository

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
