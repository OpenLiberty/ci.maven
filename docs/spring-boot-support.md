## Spring Boot Support
The `liberty-maven-plugin` provides support for Spring Boot applications, allowing you to install the [Spring Boot executable JAR](https://docs.spring.io/spring-boot/docs/current/reference/html/build-tool-plugins-maven-plugin.html) to Open Liberty and WebSphere Liberty runtime versions 18.0.0.2 and above. The `spring-boot-maven-plugin` should be configured before `liberty-maven-plugin` to create an executable jar.

### Additional Parameters

When installing a Spring Boot application via the Spring Boot executable JAR, the following are the parameters supported by the `deploy` goal in addition to the [common server parameters](common-server-parameters.md#common-server-parameters) and the [common parameters](common-parameters.md#common-parameters).

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| appsDirectory | The server's `apps` or `dropins` directory where the application files should be copied. The default value is set to `apps` if the application is defined in the server configuration, otherwise it is set to `dropins`.  | No |
| deployPackages | The Maven packages to copy to Liberty runtime's application directory. This parameter should be set to `spring-boot-project`. | Yes |

The `server.xml` provided by the `serverXmlFile` parameter or located in the `configDirectory` should enable the one of the following Spring Boot features.

| Feature | Description |
| ------- | ----------- |
| springBoot-1.5 | Required to support applications with Spring Boot version 1.5.x. |
| springBoot-2.0 | Required to support applications with Spring Boot version 2.0.x. |
| springBoot-3.0 | Required to support applications with Spring Boot version 3.x. |

The Liberty features that support the Spring Boot starters can be found [here](https://www.ibm.com/support/knowledgecenter/SSAW57_liberty/com.ibm.websphere.wlp.nd.multiplatform.doc/ae/rwlp_springboot.html). They should be enabled in the `server.xml` along with the appropriate Spring Boot feature.

### Java Support

The Spring Boot version 3.x requires Java 17 or above.

### Example

To use the `liberty-maven-plugin` to install a Spring Boot application packaged as a Spring Boot Uber JAR, include the appropriate XML in the `plugins` section of your `pom.xml`.

```xml
<build>
    <plugins> 
        ...   	  
        <plugin>
            <groupId>io.openliberty.tools</groupId>
            <artifactId>liberty-maven-plugin</artifactId>
            <executions>
               ...
                <execution>
                    <id>install-apps</id>
                    <phase>pre-integration-test</phase>
                    <goals>
                        <goal>deploy</goal>
                    </goals>
                    <configuration>
                        <appsDirectory>apps</appsDirectory>
                        <deployPackages>spring-boot-project</deployPackages>
                    </configuration>
                </execution>
                ...
            </executions>
            <configuration>
               <installDirectory>/opt/ibm/wlp</installDirectory>
               <serverName>test</serverName>
           </configuration>
        </plugin>
    </plugins>
</build>

```

