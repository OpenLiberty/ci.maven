#### display-url
---
Display an application URL in the default browser.

###### Additional Parameters

The following parameters are supported by this goal. 

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| applicationURL | URL to display in the default browser. There is no default. | Yes |

Examples:

1. To display a URL using the `liberty:display-url` goal, configure `applicationURL` with the plug-in configuration.

```xml
<plugin>
    <groupId>io.openliberty.tools</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <configuration>
        <applicationURL>http://localhost:8080/sample</applicationURL>
    </configuration>
</plugin>
```

```bash
$ mvn liberty:display-url
```

2. To bind the `display-url` goal to a phase, configure the phase and the particular URL to display at that phase.

```xml
<plugin>
    <groupId>io.openliberty.tools</groupId>
    <artifactId>liberty-maven-plugin</artifactId>   
    <executions>
        <execution>
           <id>open-ui-in-browser</id>
            <phase>package</phase>
            <configuration>
                <applicationURL>http://localhost:8080</applicationURL>
            </configuration>
            <goals>
                <goal>display-url</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```


