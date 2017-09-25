#### run-server
---
Display an application URL in the default browser.

###### Additional Parameters

The following are the parameters supported by this goal. 

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| applicationURL | URL to display in the default browser. There is no default. | Yes |

Examples:

1. To display a URL using the `liberty:display-url` goal, configure `applicationURL` with the plug-in configuration.

```xml
<plugin>
    <groupId>net.wasdev.wlp.maven.plugins</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <configuration>
        <applicationURI>http://localhost:8080/sample</applicationURI>
    </configuration>
</plugin>
```

```bash
$ mvn liberty:display-url
```

2. To bind the `display-url` goal to a phase, configure the phase and the particular URL to display at that phase.

```xml
<plugin>
    <groupId>net.wasdev.wlp.maven.plugins</groupId>
    <artifactId>liberty-maven-plugin</artifactId>   
    <executions>
        <execution>
           <id>open-ui-in-browser</id>
            <phase>package</phase>
            <configuration>
                <applicationURI>http://localhost:8080</applicationURI>
            </configuration>
            <goals>
                <goal>display-url</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```


