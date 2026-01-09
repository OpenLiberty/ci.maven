#### toolchain
---
Use Maven toolchains to run Liberty Maven Plugin goals with a specific JDK. This allows you to control the Java version used by Liberty during build, dev mode, and server operations, even when the system default JDK is different.

Maven Toolchains are used to ensure consistent and reproducible builds by allowing projects to explicitly select a required JDK, independent of the system’s default Java installation. For more details, see the [Maven Toolchains Plugin documentation](https://maven.apache.org/plugins/maven-toolchains-plugin/).

Note: Support for Maven Toolchains in Liberty Maven Plugin is available from version 3.12.0 onwards.


###### Overview

Maven toolchains allow you to select a JDK from `~/.m2/toolchains.xml` based on requirements (for example, a Java version).

The Liberty Maven Plugin supports toolchains through the `jdkToolchain` configuration element.
When `jdkToolchain` is configured:

- Liberty server goals that run server commands (for example, `create`, `start`, `stop`, `status`) attempt to run those commands with the selected toolchain JDK.
- In dev mode (`dev`), the plugin uses the configured toolchain version when determining effective Java compiler options and when running tests through the Surefire and Failsafe plugins.


###### Prerequisites

1. Configure a JDK toolchain in `~/.m2/toolchains.xml`.

Example:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<toolchains>
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>11</version>
      <vendor>ibm</vendor>
    </provides>
    <configuration>
      <jdkHome>/path/to/jdk-11</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
```


###### Configuration

Configure the Liberty Maven Plugin with `jdkToolchain`.

The `jdkToolchain` element is a set of requirements used to find a matching JDK toolchain in `~/.m2/toolchains.xml`.
In addition to `version` and `vendor`, you can specify other toolchain requirements supported by Maven toolchains if any.

| Parameter | Description | Required       |
| --------  | ----------- |----------------|
| version | JDK toolchain version to use. | Typically set. |
| vendor | JDK toolchain vendor to use. | No             |

Example:
```xml
<plugin>
  <groupId>io.openliberty.tools</groupId>
  <artifactId>liberty-maven-plugin</artifactId>
  <version>...</version>
  <configuration>
    <jdkToolchain>
      <version>11</version>
      <!-- Optional: include vendor if you need to distinguish between toolchains -->
      <!-- <vendor>ibm</vendor> -->
    </jdkToolchain>
  </configuration>
</plugin>
```


###### How it works

When `jdkToolchain` is configured, the Liberty Maven Plugin uses Maven's toolchain lookup to find a matching JDK in `~/.m2/toolchains.xml`.

- If a matching toolchain is found, the plugin uses that toolchain to determine a `JAVA_HOME` and runs Liberty server commands using that `JAVA_HOME`.
- If no matching toolchain is found (or `jdkHome` is not available), the plugin logs a warning and runs using the same JDK that is used to run Maven.

If `JAVA_HOME` is already set in `server.env` or `jvm.options`, that configuration takes precedence. In that case, the plugin logs a warning and does not apply the toolchain.


###### Dev mode

Dev mode (`liberty:dev`) uses the configured `jdkToolchain` in two places:

1. Java compilation settings

   When a Liberty toolchain version is configured, dev mode uses that version when determining the effective Java compiler options (`release`, `source`, `target`). If the Maven Compiler Plugin is configured with a different toolchain version, dev mode logs a warning and uses the Liberty toolchain version as the effective Java version for compiler options.

2. Unit and integration test toolchains

   Dev mode integrates with:

   When a Liberty toolchain version is configured, dev mode sets the effective Surefire/Failsafe `jdkToolchain` version used for test execution. If Surefire/Failsafe has no toolchain configuration, dev mode applies the Liberty toolchain version. If Surefire/Failsafe specifies a different toolchain version, dev mode logs a warning and uses the Liberty toolchain version.

   - `maven-surefire-plugin` (`test`)
   - `maven-failsafe-plugin` (`integration-test`)


###### Rules and flows

- **No `jdkToolchain` configured**

  Liberty Maven Plugin does not perform toolchain lookup. Server goals and dev mode run using the Maven JVM and plugin defaults.

- **`jdkToolchain` configured, but no matching toolchain is available**

  Liberty Maven Plugin logs a warning and runs using the same JDK that is used to run Maven.

- **`JAVA_HOME` set in `server.env` or `jvm.options`**

  `JAVA_HOME` takes precedence over the toolchain. Liberty Maven Plugin logs a warning and does not apply the toolchain.

- **Dev mode compilation**

  If a Liberty toolchain version is configured, dev mode aligns compiler options to that version. The Liberty toolchain has higher precedence, so if the Maven Compiler Plugin specifies a different Java version, the plugin logs a warning and falls back to the Liberty toolchain JDK.

- **Dev mode tests (Surefire/Failsafe)**

  If a Liberty toolchain version is configured, dev mode uses that version for Surefire/Failsafe toolchain settings. The Liberty toolchain has higher precedence, so if the Surefire/Failsafe Plugin specifies a different Java version, the plugin logs a warning and falls back to the Liberty toolchain JDK.


###### Troubleshooting

**If you see a warning indicating that no toolchain is available, verify:**
- Your `~/.m2/toolchains.xml` exists and contains a `<jdkHome>` entry.
- The `version` (and optional `vendor`) matches your Liberty `jdkToolchain` configuration.

**Example - Different version of toolchain JDK in toolchain.xml**

Configured toolchain JDK 11 for Liberty Maven Plugin, but added only JDK 17 in the toolchain.xml
```
[WARNING] CWWKM4100W: Toolchain configured for liberty server but jdkHome is not configured in .m2/toolchain.xml.
```

**If you see a warning that toolchain is not honored because JAVA_HOME is configured:**

- Check `server.env` and `jvm.options` for `JAVA_HOME`.
- Remove `JAVA_HOME` if you want the toolchain JDK to be used.

**Example - JAVA_HOME specified in server.env**

Added a `JAVA_HOME` variable in the server.env
```
[WARNING] CWWKM4101W: The toolchain JDK configuration for goal stop is not honored because the JAVA_HOME property is specified in the server.env or jvm.options file
```