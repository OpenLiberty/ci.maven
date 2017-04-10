#### undeploy
---
Undeploy an application to a Liberty Profile server. The server instance must exist and must be running. If appArtifact or appArchive are not defined, the goal will undeploy all applications from the server.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common parameters](common-parameters.md#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| appArtifact | Maven coordinates of an application to be undeployed. | No |
| appArchive | Name of an application to be undeployed. The application type can be war, ear, rar, eba, zip, or jar. | No |
| patternSet | Includes and excludes patterns of applications to be undeployed. | No |
| timeout | Maximum time to wait (in seconds) to verify that the undeployment has completed successfully. The default value is 40 seconds. | No |

Examples:

 1. Single undeploy from an application file.
  ```xml
    <execution>
        <id>undeploy-by-appArchive</id>
        <phase>post-integration-test</phase>
        <goals>
            <goal>undeploy</goal>
        </goals>
        <configuration>
            <appArchive>HelloWorld.war</appArchive>
        </configuration>
    </execution>
  ```

 2. Single undeploy from an application with maven coordinates.
   ```xml
    <execution>
        <id>undeploy-by-appArtifact</id>
        <phase>post-integration-test</phase>
        <goals>
            <goal>undeploy</goal>
        </goals>
        <configuration>
            <appArtifact>
                <groupId>com.mycompany.webapp</groupId>
                <artifactId>webapp</artifactId>
                <version>1.0</version>
                <type>war</type>
            </appArtifact>
        </configuration>
    </execution>
  ```
 3. Undeploy all.
  ```xml
    <execution>
        <id>undeploy-all</id>
        <phase>post-integration-test</phase>
        <goals>
            <goal>undeploy</goal>
        </goals>
    </execution>
  ```

 4. Undeploy from a patternSet.
  ```xml
    <execution>
        <id>undeploy-by-patternSet</id>
        <phase>post-integration-test</phase>
        <goals>
            <goal>undeploy</goal>
        </goals>
        <configuration>
            <patternSet>
                <includes>*.war</includes>
                <excludes>webapp.war</excludes>
            </patternSet>
        </configuration>
    </execution>
  ```
