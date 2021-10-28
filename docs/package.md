#### package
---
Package a Liberty server.

In Open Liberty and WebSphere Liberty versions since 8.5.5.9, it is possible to package a server into an executable jar file by setting the `include` parameter to `runnable`. The created jar file can be executed using the `java -jar` command.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common server parameters](common-server-parameters.md#common-server-parameters) and the [common parameters](common-parameters.md#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| packageType | Type of package. Can be used with values `zip`, `jar`, `tar`, or `tar.gz`. Defaults to `jar` if `runnable` is specified for the `include` parameter. Otherwise the default is `zip`. | No
| packageName | Name of the package. Defaults to `${project.build.finalName}` | No
| packageDirectory | Directory of the packaged file. Defaults to `${project.build.directory}` | No
| include | Controls the package contents. Can be used with values `all`, `usr`, `minify`, `wlp`, `runnable`, `all,runnable`, and `minify,runnable`. The default value is `all`. | Yes, only when the `os` option is set |
| os | A comma-delimited list of operating systems that you want the packaged server to support. To specify that an operating system is not to be supported, prefix it with a minus sign ("-"). The 'include' attribute __must__ be set to `minify`. | No |
| serverRoot | Specifies the root server folder name in the archive file. | No |
| skipLibertyPackage | If true, the `package-server` goal is bypassed entirely. The default value is false. | No |
| attach | If true, the packaged file is set as the project artifact. This is only valid if the `packageType` and the project `packaging` are the same. The default value is false. | No |

Examples:
1. Package test server into a zip file.
```xml
<plugin>
    <groupId>io.openliberty.tools</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>package-server</id>
            <phase>package</phase>
            <goals>
                <goal>package</goal>
            </goals>
            <configuration>
                <packageName>test</packageName>
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

2. Package test server into a runnable jar file. 
```xml
<plugin>
    <groupId>io.openliberty.tools</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <executions>
        ...
        <execution>
            <id>package-server</id>
            <phase>package</phase>
            <goals>
                <goal>package</goal>
            </goals>
            <configuration>
                <packageName>test</packageName>
                <packageType>jar</packageType>
                <include>runnable</include>
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

3. Multi-module example for deploying a `war` application on a Liberty server and packaging that server into a runnable jar file. The runnable jar file is set as the project artifact for the `package-server` module when the `attach` parameter is set to `true`.
```xml
    <artifactId>multi-module-package</artifactId>
    <packaging>pom</packaging>
    ...
    <modules>
        <module>deploy-war</module>
        <module>package-server</module>
    </modules>
```

pom.xml for the `deploy-war` module:
```xml
    <artifactId>my-war</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>war</packaging>
    ...
    <plugin>
        <groupId>io.openliberty.tools</groupId>
        <artifactId>liberty-maven-plugin</artifactId>
        <configuration>
            <serverName>myTestServer</serverName>
            <stripVersion>true</stripVersion>
            <looseApplication>false</looseApplication>
        </configuration>
        <executions>
            <execution>
                <id>create-and-setup-liberty-server</id>
                <phase>install</phase>
                <goals>
                    <goal>create</goal>
                    <goal>install-feature</goal>
                    <goal>deploy</goal>
                </goals>
            </execution>
        </executions>
    </plugin>
    ...
```
Note: A `server.xml` file must be located in the `src/main/liberty/config` directory with the required features for this `war` application. Those features are installed with the `install-feature` goal. If the `war` application is defined in the `server.xml` file, then the application is deployed to the `apps` directory. Otherwise it is deployed to the `dropins` directory.

pom.xml for the `package-server` module:
```xml
    <artifactId>my-liberty-server-package</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>
    ...
    <plugin>
        <groupId>io.openliberty.tools</groupId>
        <artifactId>liberty-maven-plugin</artifactId>
        <configuration>
            <installDirectory>../deploy-war/target/liberty/wlp</installDirectory>
            <serverName>myTestServer</serverName>
            <include>all,runnable</include>
            <packageType>jar</packageType>
            <attach>true</attach>
        </configuration>
        <executions>
            <execution>
                <id>package</id>
                <phase>package</phase>
                <goals>
                    <goal>package</goal>
                </goals>
            </execution>
        </executions>
    </plugin>
    ...
```
Note: The `<packaging>` value for the project and the `<packageType>` for the `package` goal must be the same when the `attach` configuration parameter is set to `true`.
