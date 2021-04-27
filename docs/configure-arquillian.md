#### configure-arquillian
---

The `configure-arquillian` goal is used to easily integrate Arquillian and the Arquillian Liberty Managed and Remote containers with your existing Liberty project. It provides two major advantages:

1. The ability to set `arquillian.xml` container properties directly within the `pom.xml`.
2. Automatic generation of `arquillian.xml` when using the Arquillian Liberty Managed container. 

For more convenient integration with the Arquillian Liberty Managed/Remote containers and JUnit/TestNG, you can use this goal with the [Arquillian Liberty Dependency Bundles](https://github.com/wasdev/arquillian-liberty-dependencies).

**Notice:** Be aware of false positive test results when using certain versions of TestNG with Arquillian. More information can be found [here](https://github.com/WASdev/arquillian-liberty-dependencies/blob/master/docs/testng-false-positives.md).

#### Automatic generation of `arquillian.xml`

For both Arquillian Liberty Managed and Remote containers, the `configure-arquillian` goal can automatically generate and configure the `arquillian.xml` that usually exists in the `src/test/resources` directory, eliminating the need for the user to manually create the file in projects with only a single Arquillian container. 

Furthermore, when using the Arquillian Liberty Managed container, `configure-arquillian` will also automatically specify the three required configuration parameters: `wlpHome`, `serverName`, and `httpPort`. These are set to values specified in the `liberty-maven-plugin`. Also, if the `userDirectory` configuration parameter is specified, `configure-arquillian` will set the `usrDir` property to its value.

The `configure-arquillian` goal will perform configuration for the Liberty Managed or Remote container based on which container exists on the classpath. Arquillian does not allow for more than one container on the classpath. In the event of neither container existing on the classpath, the `configure-arquillian` goal will default to providing configuration for the Liberty Managed container. 

**Common configuration parameters for the Arquillian Liberty Managed and Remote configurations:**

| Property | Type | Description | Default |
-----------| ------------ | ------- | ------- |
| `arquillianProperties` | Dictionary | Used to set key/value pairs of configuration parameters in `arquillian.xml`. | **Managed:** A dictionary containing values for `wlpHome`, `serverName`, and `httpPort` as specified in the `liberty-maven-plugin`. Optionally, `usrDir` is added if `userDirectory` is specified in the `liberty-maven-plugin`.<br>**Remote:** An empty dictionary when using the Arquillian Liberty Remote container. |
| `skipIfArquillianXmlExists` | Boolean | Skips the `configure-arquillian` goal if `arquillian.xml` already exists in the `target` directory. | False |

For documentation of the Liberty Arquillian Containers, please see the following links:

- [Liberty Managed Container Documentation](https://github.com/OpenLiberty/liberty-arquillian/blob/master/liberty-managed/README.md)
- [Liberty Remote Container Documentation](https://github.com/OpenLiberty/liberty-arquillian/blob/master/liberty-remote/README.md)

#### Usage Example

Specify the following `liberty-maven-plugin` configuration in `pom.xml`:

```
<plugin>
	<groupId>io.openliberty.tools</groupId>
	<artifactId>liberty-maven-plugin</artifactId>
	<version>2.2</version>
	<extensions>true</extensions>
	<!-- Specify configuration, executions for liberty-maven-plugin -->
	<configuration>
		<serverName>LibertyProjectServer</serverName>
		<packageName>package</packageName>
		<packageType>jar</packageType>
		<bootstrapProperties>
			<default.http.port>9080</default.http.port>
			<default.https.port>9443</default.https.port>
			<build.directory>${project.build.directory}</build.directory>
		</bootstrapProperties>
		<features>
			<acceptLicense>true</acceptLicense>
		</features>
		<deployPackages>all</deployPackages>
		<appsDirectory>apps</appsDirectory>
		<stripVersion>true</stripVersion>
		<looseApplication>true</looseApplication>
		<skipTestServer>true</skipTestServer>
		<!-- begin configure-arquillian -->
		<arquillianProperties>
			<verifyApps>arquillian-test</verifyApps>
		</arquillianProperties>
		<skipIfArquillianXmlExists>true</skipIfArquillianXmlExists>
		<!-- end configure-arquillian -->
	</configuration>
	<executions>
		<execution>
			<id>configure-arquillian-xml</id>
			<phase>pre-integration-test</phase>
			<goals>
				<goal>configure-arquillian</goal>
			</goals>
		</execution>
	</executions>
</plugin>
```

Note the `arquillianProperties` and `skipIfArquillianXmlExists` set in the configuration block, and the `configure-arquillian` goal in the `pre-integration-test` phase of the executions. You will also need to set `skipTestServer` to true unless you set `allowConnectingToRunningServer` in the `arquillianProperties`. This will ensure that the server is started and stopped by Arquillian. 

You can verify that the `arquillian.xml` is being generated properly by viewing it at `target/test-classes/arquillian.xml`. You'll know that the file was generated by the `configure-arquillian` goal if the following comment is at the end of the file:

```
<!-- This file was generated by the Liberty build plugin. -->
```
