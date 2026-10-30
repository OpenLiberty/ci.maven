# toolchain-run-it

These integration tests start Liberty with `mvn liberty:run` using a JDK selected through Maven Toolchains. They depend on a JDK toolchain being configured in `~/.m2/toolchains.xml`.

## Prerequisites

Create `~/.m2/toolchains.xml` (or add the `<toolchain>` entries to your existing file) with a JDK 8 entry, and replace the `<jdkHome>` value with the path to a JDK 8 installation on your machine:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<toolchains>
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>8</version>
    </provides>
    <configuration>
      <jdkHome>/Library/Java/JavaVirtualMachines/your-jdk8/Contents/Home</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
```

The path above is a macOS example. Use the equivalent JDK location for your operating system.

## Missing configuration

Before each test class starts, the tests verify that `~/.m2/toolchains.xml` exists and that it has a `jdk` toolchain, with an existing `<jdkHome>` directory, for the required Java version. If it does not, the test fails immediately with a message that names the missing file or Java version, instead of failing later with an empty server log.
