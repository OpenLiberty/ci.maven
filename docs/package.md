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
