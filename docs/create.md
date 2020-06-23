#### create
---
Create a Liberty server.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common server parameters](common-server-parameters.md#common-server-parameters) and the [common parameters](common-parameters.md#common-parameters).

If the server has already been created, the create goal will update the [server configuration](common-server-parameters.md#common-server-parameters) from source. 

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| template | Name of the template to use when creating a new server. | No |
| libertySettingsFolder | Path to directory containing liberty configuration files (*jvm.options* or *server.env*) to be copied to the server install "etc" directory. | No |
| noPassword | If true, disable generation of default keystore password by specifying the --no-password option when creating a new server. This option was added in 18.0.0.3. The default value is false. | No |

Example:
```xml
<plugin>
    <groupId>io.openliberty.tools</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <executions>
        ...
        <execution>
            <id>create-server</id>
            <phase>pre-integration-test</phase>
            <goals>
                <goal>create</goal>
            </goals>
        </execution>
        ...
    </executions>
    <configuration>
       <installDirectory>/opt/ibm/wlp</installDirectory>
       <serverName>test</serverName>
    </configuration>
</plugin>
```
