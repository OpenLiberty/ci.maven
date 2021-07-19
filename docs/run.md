#### run
---
Start a Liberty server in the foreground. The run goal implicitly creates the server, installs features referenced by the server.xml file, and deploys the application before starting the Liberty server. 
**Note:** This goal is designed to be executed directly from the Maven command line.

###### Multiple Modules

The `run` goal is supported with multi module Maven projects in the same way as the [`dev` goal](dev.md#multiple-modules) but substitute `dev` for `run` in the examples.  This will build the multi module project and start the Liberty server in the foreground, using Liberty configuration from the module that does not have other modules depending on it.

###### Additional Parameters

The following are the parameters supported by this goal in addition to the [common server parameters](common-server-parameters.md#common-server-parameters) and the [common parameters](common-parameters.md#common-parameters).

The run goal will propagate changes from source to the [server configuration](common-server-parameters.md#common-server-parameters). 

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| clean | Clean all cached information on server start up. The default value is `false`. | No |
| embedded | Whether the server is [embedded](https://www.ibm.com/support/knowledgecenter/SSD28V_9.0.0/com.ibm.websphere.wlp.core.doc/ae/twlp_extend_embed.html) in the Maven JVM. If not, the server will run as a separate process. The default value is `false`. | No |

Example:
```xml
<plugin>
    <groupId>io.openliberty.tools</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <configuration>
       <installDirectory>/opt/ibm/wlp</installDirectory>
       <serverName>test</serverName>
    </configuration>
</plugin>
```

```bash
$ mvn liberty:run
```
