#### compile-jsp
---
Compile the JSP files in the `src/main/webapp` directory. This goal relies on a running server, so a Liberty server must be configured. This goal is designed to run during the normal compile phase of the Maven build.

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
    <groupId>net.wasdev.wlp.maven.plugins</groupId>
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
