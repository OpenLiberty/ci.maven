# toolchain-run-it

These integration tests start Liberty with `mvn liberty:run` using a JDK selected through Maven Toolchains. They depend on a JDK toolchain being configured in `~/.m2/toolchains.xml`.

## Prerequisites

Copy [`toolchains-template.xml`](../../../../docs/toolchains-template.xml) to `~/.m2/toolchains.xml` (or add its `<toolchain>` entries to your existing file) and replace each `<jdkHome>` value with the path to a JDK of that version on your machine. These tests require a JDK 8 entry. Other toolchain integration tests in `liberty-maven-plugin/src/it` require a JDK 11 entry. The template contains both, with comments on how to add more.

## Missing configuration

Before each test class starts, the tests verify that `~/.m2/toolchains.xml` exists and that it has a `jdk` toolchain, with an existing `<jdkHome>` directory, for the required Java version. If it does not, the test fails immediately with a message that names the missing file or Java version, instead of failing later with an empty server log.
