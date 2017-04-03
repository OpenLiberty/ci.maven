#### package-server
---
Package a Liberty Profile server.

Starting with WebSphere Liberty 8.5.5.9, it is possible to package a server into an executable jar file by setting the `include` parameter to `runnable`. The created jar file can be executed using the `java -jar` command.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common server parameters](common-server-parameters.md#common-server-parameters) and the [common parameters](common-parameters.md#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| packageFile | Location of the target file or directory. If the target location is a file, the contents of the server instance will be compressed into the specified file. If the target location is a directory, the contents of the server instance will be compressed into `${packageFile}/${serverName}.zip` file. If the target location is not specified, it defaults to `${project.build.directory}/${serverName}.zip`. | No |
| include | Packaging type. One of `all`, `usr`, `minify` or `runnable` (8.5.5.9 and above). The default value is `all`. | Yes, only when the `os` option is set |
| os | A comma-delimited list of operating systems that you want the packaged server to support. To specify that an operating system is not to be supported, prefix it with a minus sign ("-"). The 'include' attribute __must__ be set to `minify`. | No |

Examples:
1. Package test server into a zip file.
```xml
<plugin>
    <groupId>net.wasdev.wlp.maven.plugins</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <executions>
        ...
        <execution>
            <id>package-server</id>
            <phase>package</phase>
            <goals>
                <goal>package-server</goal>
            </goals>
            <configuration>
                <packageFile>${project.build.directory}/test.zip</packageFile>
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
    <groupId>net.wasdev.wlp.maven.plugins</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <executions>
        ...
        <execution>
            <id>package-server</id>
            <phase>package</phase>
            <goals>
                <goal>package-server</goal>
            </goals>
            <configuration>
                <packageFile>${project.build.directory}/test.jar</packageFile>
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
