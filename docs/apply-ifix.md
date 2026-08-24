#### apply-ifix
---
Applies WebSphere Liberty iFix JAR files to the Liberty runtime installation.

This goal reads iFix JAR files from the directory specified by `libertyifixDir` (default:
`${project.basedir}/src/main/liberty/ifixes`) and applies each one to the Liberty runtime by
executing `java -jar <ifix>.jar --installLocation <installDirectory>`.

**Important:** This goal only supports WebSphere Liberty (i.e. `runtimeArtifact` with
`groupId` set to `com.ibm.websphere.appserver.runtime`). It will not run against Open Liberty
or other runtimes.

iFix processing is **disabled by default**. Set `<applyLibertyiFix>true</applyLibertyiFix>`
in the plugin `<configuration>` to enable it.

This goal is automatically invoked during the `prepare-package` phase of the
`liberty-assembly` lifecycle after `install-feature`.

#### Parameters

| Parameter | Description | Required | Default |
| --------- | ----------- | -------- | ------- |
| `applyLibertyiFix` | Set to `true` to enable iFix application. When `false`, the goal exits immediately without performing any work. | No | `false` |
| `libertyifixDir` | Directory containing the iFix JAR files to apply. All `*.jar` files in this directory are treated as iFix archives. | No | `${project.basedir}/src/main/liberty/ifixes` |
| `stopOniFixApplyError` | When `true`, any error during iFix processing (wrong runtime, missing files, version mismatch, or non-zero exit code) causes the build to fail with an error. When `false`, errors are reported as warnings and the build continues. | No | `true` |

The following [common parameters](common-parameters.md#common-parameters) are also supported:
`runtimeArtifact`, `runtimeInstallDirectory`, `installDirectory`, `libertyRuntimeGroupId`,
`libertyRuntimeArtifactId`, `libertyRuntimeVersion`.

#### iFix JAR naming convention

iFix JARs supplied by IBM follow the naming pattern:

```
{YYMMNN}-wlp-archive-{APAR_ID}.jar
```

For example: `250012-wlp-archive-IFPH69485.jar` targets WebSphere Liberty 25.0.0.12 and
resolves APAR PH69485.

The goal validates each iFix against the installed Liberty version by reading the
`Applies-To` attribute from the JAR's `META-INF/MANIFEST.MF`:

```
Applies-To: io.openliberty; productVersion=25.0.0.12; productInstallType=Archive
```

If the `productVersion` does not match the installed Liberty version the behaviour is
controlled by `stopOniFixApplyError`.

#### Examples

1. Enable iFix application via `<configuration>`:

    Place iFix JAR files in `src/main/liberty/ifixes/` and add the following to the plugin
    configuration:

    ```xml
    <plugin>
        <groupId>io.openliberty.tools</groupId>
        <artifactId>liberty-maven-plugin</artifactId>
        <configuration>
            <runtimeArtifact>
                <groupId>com.ibm.websphere.appserver.runtime</groupId>
                <artifactId>wlp-webProfile10</artifactId>
                <version>25.0.0.12</version>
                <type>zip</type>
            </runtimeArtifact>
            <applyLibertyiFix>true</applyLibertyiFix>
        </configuration>
    </plugin>
    ```

2. Specify a custom iFix directory and continue on errors:

    ```xml
    <plugin>
        <groupId>io.openliberty.tools</groupId>
        <artifactId>liberty-maven-plugin</artifactId>
        <configuration>
            <runtimeArtifact>
                <groupId>com.ibm.websphere.appserver.runtime</groupId>
                <artifactId>wlp-webProfile10</artifactId>
                <version>25.0.0.12</version>
                <type>zip</type>
            </runtimeArtifact>
            <applyLibertyiFix>true</applyLibertyiFix>
            <libertyifixDir>${project.basedir}/ifixes</libertyifixDir>
            <stopOniFixApplyError>false</stopOniFixApplyError>
        </configuration>
    </plugin>
    ```

3. Run the goal explicitly (outside the `liberty-assembly` lifecycle):

    ```xml
    <plugin>
        <groupId>io.openliberty.tools</groupId>
        <artifactId>liberty-maven-plugin</artifactId>
        <executions>
            <execution>
                <id>apply-ifix</id>
                <phase>prepare-package</phase>
                <goals>
                    <goal>apply-ifix</goal>
                </goals>
                <configuration>
                    <applyLibertyiFix>true</applyLibertyiFix>
                </configuration>
            </execution>
        </executions>
    </plugin>
    ```
