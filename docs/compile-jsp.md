#### compile-jsp
---
Compile the jsps in src/main/webapp. This relies on starting a server in the background so needs to have a liberty configured. This is designed to be run during the normal compile phase of the maven build.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common parameters](common-parameters.md#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| jspVersion | Sets the version of JSP to be used. Can be 2.2 or 2.3. The default value is `2.3`. | No |

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
