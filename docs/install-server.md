#### install-server
---
Installs Liberty runtime and optionally upgrades the runtime installation to a supported production edition.

To upgrade the runtime installation, the Liberty license jar file which is available to download from IBM Fix Central or the Passport
Advantage website, must be installed to Maven local repository or an internal custom repository using the `maven-install-plugin` version 2.5 and up. See [Installing an artifact with a custom POM](http://maven.apache.org/plugins/maven-install-plugin/examples/custom-pom-installation.html)

This goal only supports the [common parameters](common-parameters.md#common-parameters), and is implicitly invoked by `create-server`, `dump-server`, `java-dump-server`, `package-server`, `run-server`, `start-server`, `test-start-server` and `debug-server` goals.

Examples:
 1. Install from a packaged server using `assemblyArchive` parameter.
 
  ```xml
    <plugin>
        <groupId>net.wasdev.wlp.maven.plugins</groupId>
        <artifactId>liberty-maven-plugin</artifactId>
        <executions>
            ...
            <execution>
                <id>install-server</id>
                <phase>pre-integration-test</phase>
                <goals>
                    <goal>install-server</goal>
                </goals>
                <configuration>
                    <assemblyArchive>/opt/ibm/wlp.zip</assemblyArchive>
                </configuration>
            </execution>
            ...
        </executions>
    </plugin>
  ```
  
 2. Install from a Maven artifact using `assemblyArtifact` parameter, and upgrade to production supported Core edition using the `licenseArtifact` parameter.
 See [Using Maven artifact](installation-configuration.md#using-maven-artifact) for more information.
 
  ```xml
    <plugin>
        <groupId>net.wasdev.wlp.maven.plugins</groupId>
        <artifactId>liberty-maven-plugin</artifactId>
        <executions>
            ...
            <execution>
                <id>install-server</id>
                <phase>pre-integration-test</phase>
                <goals>
                    <goal>install-server</goal>
                </goals>
                <configuration>
                    <assemblyArtifact>
                        <groupId>com.ibm.websphere.appserver.runtime</groupId>
                        <artifactId>wlp-webProfile7</artifactId>
                        <version>17.0.0.1</version>
                        <type>zip</type>
                    </assemblyArtifact>
                    <licenseArtifact>
                        <groupId>com.ibm.websphere.appserver.license</groupId>
                        <artifactId>wlp-core-license</artifactId>
                        <version>17.0.0.1</version>
                    </licenseArtifact>
                </configuration>
            </execution>
            ...
        </executions>
    </plugin>
  ```
 3. Install from a given location using `install` parameter. See [Using a repository](installation-configuration.md#using-a-repository)  for more information. 
 
  ```xml
    <plugin>
        <groupId>net.wasdev.wlp.maven.plugins</groupId>
        <artifactId>liberty-maven-plugin</artifactId>
        <executions>
            ...
            <execution>
                <id>install-server</id>
                <phase>pre-integration-test</phase>
                <goals>
                    <goal>install-server</goal>
                </goals>
                <configuration>
                    <install>
                        <runtimeUrl><url to .jar or .zip file></runtimeUrl>
                        <licenseCode><license code></licenseCode>
                    </install>
                </configuration>
            </execution>
            ...
        </executions>
    </plugin>
  ```
4. This goal is implicitly invoked by `create-server`, `dump-server`, `java-dump-server`, `package-server`, `run-server`,
`start-server`, `test-start-server` and `debug-server` goals.

  ```xml
    <plugin>
        <groupId>net.wasdev.wlp.maven.plugins</groupId>
        <artifactId>liberty-maven-plugin</artifactId>
        <configuration>
            <assemblyArtifact>
                <groupId>com.ibm.websphere.appserver.runtime</groupId>
                <artifactId>wlp-webProfile7</artifactId>
                <version>17.0.0.1</version>
                <type>zip</type>
            </assemblyArtifact>
            <licenseArtifact>
                <groupId>com.ibm.websphere.appserver.license</groupId>
                 <artifactId>wlp-core-license</artifactId>
                <version>17.0.0.1</version>
            </licenseArtifact>
        </configuration>
        <executions>
            <execution>
                <id>start-server</id>
                <phase>pre-integration-test</phase>
                <goals>
                    <goal>start-server</goal>
                </goals>
                <configuration>
                    <serverName>test</serverName>
                </configuration>
            </execution>
            ...
        </executions>
    </plugin>
  ```

5. Install the Websphere Application Server Liberty Core production license jar file.

  ``` bash
    mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file -Dfile=wlp-core-license.jar
  ```

