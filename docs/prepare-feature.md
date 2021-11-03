#### prepare-feature
---
Generates `features.json` file for user feature(s). The `features.json` file is JSON file that contains the information found within a feature's ESA manifest file. JSONs are a key requirement for the installation of any liberty features(s) from a maven repository. 


In Open Liberty and WebSphere Liberty runtime versions 21.0.0.11 and above, this goal can prepare the user feature to generate the JSON file in the following way:


1. Create a `features-bom` file for the user feature. The `features-bom` artifact in each groupId provides the bill of materials (BOM) for each maven artifacts. 
 ```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>userTest.user.test.features</groupId>
  <artifactId>features-bom</artifactId>
  <version>21.0.0.11</version>
  <packaging>pom</packaging>
  <name>user feature bill of materials</name>
  <description>user feature bill of materials</description>
 
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>userTest.user.test.features</groupId>
        <artifactId>testesa1</artifactId>
        <version>21.0.0.11</version>
        <type>esa</type>
        <scope>provided</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
</project>

 ```

2. Use the `prepare-feature` goal. 

 * Provide the maven coordinate of the custom made `features-bom` file:
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
        <version>21.0.0.11</version>
        <type>pom</type>
      </dependency>
    </dependencies>
  </dependencyManagement>
 ```
3. Install the user feature using the `install-feature` goal.
