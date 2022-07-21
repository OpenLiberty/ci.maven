#### prepare-feature
---
Generates `features.json` file for user feature(s). The `features.json` file is JSON file that contains the information found within a feature's ESA manifest file. JSONs are a key requirement for the installation of any Liberty features(s) from a Maven repository. 


In Open Liberty and WebSphere Liberty runtime versions 21.0.0.11 and above, this goal can prepare the user feature to generate the JSON file in the following way:


1. Place your user feature ESA file in Maven local repository or Maven Central repository.
2. Create a `features-bom` file for the user feature in Maven local repository or Maven Central repository. The `features-bom` artifact in each groupId provides the bill of materials (BOM) for each Maven artifacts. 
 ```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>userTest.user.test.features</groupId>
  <artifactId>features-bom</artifactId>
  <version>1.0</version>
  <packaging>pom</packaging>
  <name>user feature bill of materials</name>
  <description>user feature bill of materials</description>
 
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>userTest.user.test.features</groupId>
        <artifactId>testesa1</artifactId>
        <version>1.0</version>
        <type>esa</type>
        <scope>provided</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
</project>

 ```

3. Use the `prepare-feature` goal. 

 * Provide the Maven coordinate of the custom made `features-bom` file:
 ```xml
<plugin>
    <groupId>io.openliberty.tools</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <executions>
        ...
        <execution>
            <id>prepare-feature</id>
            <phase>prepare-package</phase>
            <goals>
                <goal>prepare-feature</goal>
            </goals>
        </execution>
        ...
    </executions>
    <configuration>
       <installDirectory>/opt/ibm/wlp</installDirectory>
       <serverName>test</serverName>
    </configuration>
</plugin>

 <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>userTest.user.test.features</groupId>
        <artifactId>features-bom</artifactId>
        <version>1.0</version>
        <type>pom</type>
      </dependency>
    </dependencies>
  </dependencyManagement>
 ```
4. Install the user feature using the `install-feature` goal.
