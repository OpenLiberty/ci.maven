## prepare-config

---

Prepare Liberty configuration and generate `liberty-plugin-config.xml` with a mock Liberty server structure. This lightweight goal creates a temporary Liberty server structure, copies configuration files, and generates metadata needed by IDE tools and language servers.

**What this goal does:**
1. Creates a mock Liberty server structure in `target/.libertyls-var-cache/wlp/usr/servers/{serverName}/` (configurable)
2. Copies all configuration files (server.xml, bootstrap.properties, server.env, jvm.options, etc.) to the mock server
3. Generates `liberty-plugin-config.xml` pointing to the mock server structure

**Note:** This goal does NOT install Liberty runtime. It only creates a minimal directory structure and copies configuration files.

---

### Usage

Run directly from the command line:

```bash
mvn liberty:prepare-config
```

Or configure it to run automatically:

```xml
<plugin>
    <groupId>io.openliberty.tools</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>prepare-config</id>
            <phase>initialize</phase>
            <goals>
                <goal>prepare-config</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

---

### Configuration

The goal uses the [common parameters](common-parameters.md) and [common server parameters](common-server-parameters.md).

The temporary directory for the mock server structure defaults to `.libertyls-var-cache` (a hidden directory). To override this, use the system property:

```bash
mvn liberty:prepare-config -DprepareConfigTempDir=my-temp-dir
```

This will create the mock server structure in `target/my-temp-dir/wlp/usr/servers/{serverName}/` instead of the default location.

---

### Generated Files

The goal generates:

1. **Mock Liberty Server Structure** in `target/.libertyls-var-cache/`:
   ```
   target/.libertyls-var-cache/
   └── wlp/
       └── usr/
           └── servers/
               └── {serverName}/
                   ├── server.xml
                   ├── bootstrap.properties
                   ├── server.env
                   └── jvm.options
   ```

2. **Configuration Metadata File** `target/liberty-plugin-config.xml` containing project metadata, dependencies, and configuration file paths for IDE tools and language servers.

---

### Use Cases

- **IDE Language Server Support**: Provides metadata for code completion, validation, and diagnostics in Liberty configuration files
- **Quick Configuration Validation**: Validate configuration without full project build
- **CI/CD Integration**: Generate configuration metadata early in the pipeline

**For full variable resolution features**, install Liberty first:

```bash
mvn liberty:create
mvn liberty:prepare-config
```

---

### See Also

- [Common Parameters](common-parameters.md)
- [Common Server Parameters](common-server-parameters.md)
- [create goal](create.md) - Install Liberty and create server
- [dev goal](dev.md) - Development mode with hot reload