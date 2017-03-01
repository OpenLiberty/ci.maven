#### compile-jsp
---
Compile the JSP files in src/main/webapp. This goal relies on starting a server in the background, so it requires a configured Liberty server. This goal is designed to run during the normal compile phase of the Maven build.

###### Additional Parameters

The following parameters are supported by this goal in addition to the [common parameters](common-parameters.md#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| jspVersion | Sets the version of JSP to be used. Can be 2.2 or 2.3. The default value is `2.3`. | No |
| timeout | Stop the server if the JSP files are not compiled within the specified time. The default value is `30` seconds. | No 

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
