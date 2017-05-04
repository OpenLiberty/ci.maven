#### deploy
---
Deploy an application to a Liberty Profile server. The server instance must exist and must be running.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common parameters](common-parameters.md#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| appArtifact | Maven coordinates of an application to be deployed. | Yes, if appArchive is not set. |
| appArchive | Location of an application file to be deployed. The application type can be war, ear, rar, eba, zip, or jar. | Yes, if appArtifact is not set. |
| timeout | Maximum time to wait (in seconds) to verify that the deployment has completed successfully. The default value is 40 seconds. | No |
| appDeployName| The file name of the deployed application in the `dropins` directory. It is possible to use Maven properties to define it, for example `${project.artifactId}` or `${project.name}`. | No. By default, the `deployName` is the same as the original file name.|

Examples:

 1. Single deploy of an application with the path of a file.

  ```xml
    <execution>
        <id>deploy-app</id>
        <phase>pre-integration-test</phase>
        <goals>
            <goal>deploy</goal>
        </goals>
        <configuration>
            <appArchive>HelloWorld.war</appArchive>
        </configuration>
    </execution>
   ```

 2. Single deploy of an application with Maven coordinates using the Maven property `${project.artifactId}` to change the name, this will be `webapp.war` instead of `webapp-1.0.war` .

  ```xml
    <execution>
        <id>deploy-by-appArtifact</id>
        <phase>pre-integration-test</phase>
        <goals>
            <goal>deploy</goal>
        </goals>
        <configuration>
            <appArtifact>
                <groupId>com.mycompany.webapp</groupId>
                <artifactId>webapp</artifactId>
                <version>1.0</version>
                <type>war</type>
            </appArtifact>
            <appDeployName>${project.artifactId}.war</appDeployName>
        </configuration>
    </execution>
  ```
