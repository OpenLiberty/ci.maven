#### generate-features
---

**Note:** This goal has a runtime dependency on IBM WebSphere Application Server Migration Toolkit for Application Binaries, which is separately licensed under  IBM License Agreement for Non-Warranted Programs. For more information, see the [license](https://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/wasdev/license/wamt).

Scan the class files of an application and create a new `generated-features.xml` Liberty configuration file in the server configuration directory that contains the Liberty features the application requires. You can alternatively save the file in the source configuration directory of your application using the `generateToSrc` parameter. 

This feature is best accessed through [dev mode](dev.md) during development. When you start `liberty:dev` your application will be compiled and the class files will be scanned to verify that all the required Liberty features are included in your server configuration. Then as you work, dev mode will continue to monitor the project to confirm the Liberty features configured are up to date. If you implement a new interface in Java, the scanner will determine if that API is connected to a Liberty feature, then update the server configuration and install the feature. If you remove a feature from `server.xml`, dev mode will determine if that feature is actually necessary, and if so, add it to the generated configuration file as described below.

Feature generation is enabled through dev mode by default. If you need to disable feature generation, you can start dev mode with the parameter `-DgenerateFeatures=false`. The generated features file will be saved in the server configuration directory. If you wish to save the file in the source configuration directory, you can start dev mode with the parameter `-DgenerateToSrc=true`. When running dev mode, you can toggle the generation of features off and on by typing 'g' and pressing Enter. You can toggle the location of the generated file by typing 's' and pressing Enter.

Normally dev mode only scans a class file that has just been updated, but you can tell dev mode to rescan all class files by typing 'o' and pressing Enter. This will optimize the feature list in the generated configuration file.

###### Command Line Parameter

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| generateToSrc  | If set to `true`, place the new file `generated-features.xml` in the `src/main/liberty` directory tree rather than in the server configuration directory. The default value is `false`. | No |

##### Lifecycle

This goal is not part of the Maven lifecycle, so to use it in your build you will need to understand its dependencies. Since it will scan the class files of your application, it must be run after the `compile` goal. Since it will use the server configuration directory, it must be run after the `liberty:create` goal. The list of features that it generates will be used by the `liberty:install-feature` goals, so run this goal before installing features.

If this goal detects Liberty features used in your project but not present in your Liberty configuration, it will create a new file `configDropins/overrides/generated-features.xml` in the `target/liberty/wlp/usr/servers/defaultServer` directory or optionally in the `src/main/liberty/config` directory of your project. The `generated-features.xml` file will contain a list of features required for your project. If the `generated-features.xml` file has been created in the past and no additional features have been detected, this file will be retained.

If you are using [devc](dev.md#devc-container-mode), ensure that the `generated-features.xml` configuration file is copied to your Docker image via your Dockerfile.
```dockerfile
COPY --chown=1001:0  target/liberty/wlp/usr/servers/defaultServer/configDropins/overrides/generated-features.xml /config/configDropins/overrides/
```
If on Linux, it is recommended that you copy the entire `configDropins/overrides` directory to your Docker image via your Dockerfile.
```dockerfile
COPY --chown=1001:0  target/liberty/wlp/usr/servers/defaultServer/configDropins/overrides /config/configDropins/overrides
```

This goal examines the `pom.xml` dependencies to determine which version of Jakarta EE, MicroProfile or Java EE API you may be using. Compatible features will then be generated. 

For Jakarta EE API, this goal looks for a `jakarta.platform:jakarta.jakartaee-api` dependency and generates features according to the version number.

For MicroProfile API, this goal looks for a `org.eclipse.microprofile:microprofile` dependency and generates features according to the version number.

For Java EE API, this goal looks for a `javax:javaee-api` dependency with versions `6.0`, `7.0` or `8.0`. 

For example, if you have the following Jakarta EE and MicroProfile dependencies in your `pom.xml` file, features compatible with Jakarta EE `9.1.0` and MicroProfile `5.0` will be generated.
```xml
<dependency>
    <groupId>jakarta.platform</groupId>
    <artifactId>jakarta.jakartaee-api</artifactId>
    <version>9.1.0</version>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>org.eclipse.microprofile</groupId>
    <artifactId>microprofile</artifactId>
    <version>5.0</version>
    <type>pom</type>
    <scope>provided</scope>
</dependency>
```

This goal also considers the features you have already specified in `server.xml` or other Liberty server configuration files (e.g. `include` elements and `configDropins` files). This goal will attempt to find a working set of features that are compatible with each other.

If there are conflicts with features specified in Liberty configuration files or features used in the application code, this goal will print an error message. If available, this goal will also print a list of suggested features with no conflicts.

##### Example (outside of dev mode)

Compile the application code and generate Liberty features.
* `mvn compile liberty:create liberty:generate-features`

##### Limitations

* When using the `serverXmlFile` parameter in the POM, if you specify a file not in the directory `src/main/liberty/config` and that file uses relative paths to include other files, any features in those files will not be considered for feature generation
* Any features accessed using property variables (e.g. `${custom.key}/configFile.xml`) are not considered for feature generation

See issues tagged with [`generateFeatures`](https://github.com/OpenLiberty/ci.maven/issues?q=is%3Aissue+is%3Aopen+label%3AgenerateFeatures) for futher information.

