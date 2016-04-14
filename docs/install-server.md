#### install-server
---
Installs Liberty profile runtime.

This goal only supports the [common parameters](common-parameters.md#common-parameters).

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
  
 2. Install from a Maven artifact using `assemblyArtifact` parameter. See [Using Maven artifact](installation-configuration.md#using-maven-artifact)  for more information. 
 
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
                    <groupId>com.ibm.websphere.appserver.runtime</groupId>
                    <artifactId>wlp-webProfile7</artifactId>
                    <version>8.5.5.7</version>
                    <type>zip</type>
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
This goal is implicitly invoked by `create-server`, `dump-server`, `java-dump-server`, `package-server`, `run-server`, `start-server` and `stop-server` goals. Then, it is possible to install the Liberty profile runtime as follows: 

  ```xml
    <plugin>
        <groupId>net.wasdev.wlp.maven.plugins</groupId>
        <artifactId>liberty-maven-plugin</artifactId>
        <configuration>
            <install>
                <runtimeUrl><url to .jar or .zip file></runtimeUrl>
                <licenseCode><license code></licenseCode>
            </install>
        </configuration>
        <executions>
            ...
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
