#### compile-jsp
---
Compile the JSP files in the `src/main/webapp` directory. This goal relies on a running server, so a Liberty server must be configured. This goal is designed to run during the normal compile phase of the Maven build. The Java version used for the compilation comes from either the `maven.compiler.release` or the `maven.compiler.source` with the first taking precedence.

Note: As of Liberty version 24.0.0.1, this goal only works with Long Term Service (LTS) releases of Java. See the [documentation](https://openliberty.io/docs/latest/reference/config/jspEngine.html) for the valid values for the `javaSourceLevel` attribute on the `jspEngine` configuration element. Prior to version 24.0.0.1, the `jdkSourceLevel` attribute was used on the `jspEngine` [element](https://openliberty.io/docs/23.0.0.12/reference/config/jspEngine.html) and only supported up to and including Java 8 (specified as 18).

###### Additional Parameters

The following parameters are supported by this goal in addition to the [common parameters](common-parameters.md#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| jspVersion | Sets the JSP version to use. Valid values are `2.2` or `2.3`. The default value is `2.3`. | No 
| timeout | Maximum time to wait (in seconds) for all the JSP files to compile. The server is stopped and the goal ends after this specified time. The default value is `30` seconds. | No 

Example:
```xml
<pluginManagement>
  <plugin>
    <groupId>io.openliberty.tools</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
     <executions>
          <execution>
              <goals>
                  <goal>compile-jsp</goal>
              </goals>
          </execution>
      </executions>
  </plugin>
</pluginManagement>
```

```bash
$ mvn clean install
```
