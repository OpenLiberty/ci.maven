#### Archetypes

New versions of the archetypes are no longer being produced. The last published version is `3.7.1`. There are multiple [starters](https://github.com/OpenLiberty/ci.maven/blob/main/README.md#getting-started) available as a preferred alternative to the archetypes. However, the previously published archetypes are still available for use.

By default, all archetypes that specify a Liberty runtime use the latest version of the Open Liberty runtime. You can use a different runtime by setting the `runtimeGroupId` and `runtimeArtifactId`. For example, you can use `wlp-webProfile7` by setting `-DruntimeGroupId=com.ibm.websphere.appserver.runtime` and `-DruntimeArtifactId=wlp-webProfile7`. 

The runtime version can also be set dynamically. For example, you can specify version `20.0.0.3` of the runtime by setting `-DruntimeVersion=20.0.0.3`.

Finally, the default Liberty Maven Plugin version is set to be the same as the version of the archetype. To specify a different version of the plugin, use the `libertyPluginVersion` parameter. For example, you could set `-DlibertyPluginVersion=3.2`.

##### liberty-plugin-archetype

The `liberty-plugin-archetype` is used to generate a basic multi-module project that builds a simple web application then deploys and tests it on a Liberty server. It also creates a Liberty server package that includes the application.

###### Usage

    mvn archetype:generate \
        -DarchetypeGroupId=io.openliberty.tools \
        -DarchetypeArtifactId=liberty-plugin-archetype \
        -DarchetypeVersion=3.7.1  \
        -DlibertyPluginVersion=3.7.1  \
        -DgroupId=test \
        -DartifactId=test \
        -Dversion=1.0-SNAPSHOT

##### liberty-archetype-webapp

The `liberty-archetype-webapp` is used to generate a basic single-module project that builds a simple web application then deploys and tests on a Liberty server. It also creates a minified, runnable Liberty server package that includes the application. The generated project includes the [`liberty-maven-app-parent`](docs/parent-pom.md) parent pom that binds Liberty Maven Plugin goals to the Maven default build lifecycle.

###### Usage

    mvn archetype:generate \
        -DarchetypeGroupId=io.openliberty.tools \
        -DarchetypeArtifactId=liberty-archetype-webapp \
        -DarchetypeVersion=3.7.1 \
        -DgroupId=test \
        -DartifactId=test \
        -Dversion=1.0-SNAPSHOT

##### liberty-archetype-ear

The `liberty-archetype-ear` is used to generate a multi-module project that includes an EJB module, a web application module and an EAR module. In the EAR module, it packages the application in a Java EE 7 Enterprise Archive then deploys and tests on a Liberty server. It also creates a minified, runnable Liberty server package that includes the application EAR file. The generated project includes [`liberty-maven-app-parent`](docs/parent-pom.md) parent pom that binds Liberty Maven Plugin goals to the Maven default build lifecycle.

###### Usage

    mvn archetype:generate \
        -DarchetypeGroupId=io.openliberty.tools \
        -DarchetypeArtifactId=liberty-archetype-ear \
        -DarchetypeVersion=3.7.1  \
        -DgroupId=test \
        -DartifactId=test \
        -Dversion=1.0-SNAPSHOT
