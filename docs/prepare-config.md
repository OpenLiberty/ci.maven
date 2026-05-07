l## prepare-config

---

Prepare Liberty configuration and generate `liberty-plugin-config.xml` with a mock Liberty server structure. This lightweight goal evaluates project configuration, creates a temporary Liberty server structure in a configurable temporary directory, copies all configuration files, and generates metadata needed by IDE tools and language servers.

This goal is particularly useful for:
- Enabling IDE support for Liberty configuration files (server.xml, bootstrap.properties, server.env)
- Providing configuration metadata to language servers for code completion and diagnostics
- Supporting Liberty Tools and other IDE extensions
- Quick configuration validation without full project build
- Creating a mock Liberty server structure for language server integration

**What this goal does:**
1. Creates a mock Liberty server structure in `target/tmp/liberty-var-cache/wlp/usr/servers/{serverName}/` (configurable)
2. Copies all configuration files (server.xml, bootstrap.properties, server.env, jvm.options, etc.) to the mock server
3. Generates `liberty-plugin-config.xml` pointing to the mock server structure

The goal does NOT install Liberty runtime. It only creates a minimal directory structure that mimics a Liberty server and copies configuration files.

---

### Usage

The `prepare-config` goal is typically used in IDE scenarios where you need configuration metadata before building the project:

```xml
<plugin>
    <groupId>io.openliberty.tools</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <version>3.12.1-SNAPSHOT</version>
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

Or run it directly from the command line:

```bash
mvn liberty:prepare-config
```

---

### Configuration Parameters

The `prepare-config` goal supports the following configuration parameters in addition to the [common parameters](common-parameters.md) and [common server parameters](common-server-parameters.md):

| Parameter | Description | Required | Default |
| --------- | ----------- | -------- | ------- |
| prepareConfigTempDir | Name of the temporary directory used for mock Liberty server structures. This directory is created under the build output directory (`target/` for Maven, `build/` for Gradle). | No | `tmp/liberty-var-cache` |

---

### Examples

#### Example 1: Basic usage (default)

Generate configuration metadata with server information:

```bash
mvn liberty:prepare-config
```

This will create `target/liberty-plugin-config.xml` with project metadata, dependencies, and configuration file references.

#### Example 2: Custom temporary directory

Use a custom temporary directory name:

```bash
mvn liberty:prepare-config -DprepareConfigTempDir=my-temp-dir
```

This will create the mock server structure in `target/my-temp-dir/wlp/usr/servers/{serverName}/` instead of the default location.

#### Example 3: IDE integration

Configure the goal to run automatically during project initialization:

```xml
<plugin>
    <groupId>io.openliberty.tools</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <version>3.12.1-SNAPSHOT</version>
    <executions>
        <execution>
            <id>prepare-config-on-init</id>
            <phase>initialize</phase>
            <goals>
                <goal>prepare-config</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

---

### Generated Configuration File and Mock Server Structure

The `prepare-config` goal generates:

1. **Mock Liberty Server Structure** in `target/tmp/liberty-var-cache/` (or custom directory):
   ```
   target/tmp/liberty-var-cache/
   └── wlp/
       └── usr/
           └── servers/
               └── {serverName}/
                   ├── server.xml
                   ├── bootstrap.properties
                   ├── server.env
                   ├── jvm.options
                   └── (other config files)
   ```

2. **Configuration Metadata File** `target/liberty-plugin-config.xml` containing:

- Install directory (points to `target/tmp/liberty-var-cache/wlp`)
- User directory (points to `target/tmp/liberty-var-cache/wlp/usr`)
- Server directory (points to `target/tmp/liberty-var-cache/wlp/usr/servers/{serverName}`)
- Server name and output directory paths
- Project type (packaging)
- Active build profiles
- Project compile dependencies
- Aggregator parent information (for multi-module projects)
- Configuration directory
- Server configuration file path (in mock server)
- Bootstrap properties file path (in mock server)
- JVM options file path (in mock server)
- Server environment file path (in mock server)
- Applications directory (`apps` or `dropins`)
- Loose application configuration
- Strip version settings
- Application filename

---

### Use Cases

#### 1. IDE Language Server Support

IDEs using Liberty language servers can invoke this goal to get configuration metadata:

```bash
mvn liberty:prepare-config
```

The generated `liberty-plugin-config.xml` provides language servers with information needed to offer:
- Code completion for Liberty configuration
- Validation of server configuration
- Quick fixes and diagnostics
- Custom file path resolution (for non-standard locations)

**For full variable resolution features**, first install Liberty using `liberty:create`:

```bash
mvn liberty:create
mvn liberty:prepare-config
```

#### 2. CI/CD Pipeline Validation

Validate Liberty configuration early in the pipeline without full build:

```bash
mvn liberty:prepare-config
# Parse and validate liberty-plugin-config.xml
```

#### 3. Multi-Module Project Setup

For multi-module projects, run at the parent level to prepare configuration for all modules:

```bash
mvn liberty:prepare-config -pl :module-name
```

#### 4. Pre-Build Configuration Analysis

Analyze project configuration before committing to a full build:

```bash
mvn liberty:prepare-config
# Analyze target/liberty-plugin-config.xml for issues
```

---

### Comparison with Other Goals

| Goal | Liberty Install | Server Creation | Mock Server Structure | Config Files Copied | Use Case |
|------|----------------|-----------------|----------------------|---------------------|----------|
| `prepare-config` | No | No | Yes (in liberty-var-cache) | Yes (to mock server) | Generate config metadata and mock structure for tools |
| `create` | Yes | Yes | No | Yes (to real server) | Create and configure Liberty server |
| `install-server` | Yes | No | No | No | Install Liberty runtime only |
| `dev` | Yes | Yes | No | Yes (to real server) | Development mode with hot reload |

---

### Performance Considerations

The `prepare-config` goal is designed to be lightweight and fast:

- **Typical execution time**: ~1-2 seconds
- **No Liberty download**: Does not download or install Liberty
- **No server creation**: Does not create server directories
- **Minimal I/O**: Only reads Maven configuration and writes one XML file

This makes it ideal for IDE integration where responsiveness is critical.

---

### Language Server Integration

The `prepare-config` goal is designed to work with two Liberty language servers:

#### 1. lemminx-liberty (XML Language Server)
Provides features for `server.xml` and related XML configuration files:
- Feature completion and validation
- Configuration element completion
- Variable resolution (requires Liberty installation)
- Hover documentation

#### 2. liberty-ls (Properties Language Server)
Provides features for `bootstrap.properties` and `server.env` files:
- Property name completion
- Property value validation
- Custom file path detection

**Note**: For full variable resolution in `server.xml`, Liberty must be installed first using `liberty:create` or `liberty:dev`. The `prepare-config` goal will include the Liberty installation paths in the generated config if Liberty is already present.

---

### Troubleshooting

#### Configuration file not generated

Check that the goal executed successfully:

```bash
mvn liberty:prepare-config -X
```

The debug output will show the exact location where the file is being written.

#### Variable resolution not working in IDE

Variable resolution requires Liberty to be installed. Run:

```bash
mvn liberty:create
mvn liberty:prepare-config
```

This will install Liberty and generate the config file with Liberty installation paths.

---

### See Also

- [Common Parameters](common-parameters.md)
- [Common Server Parameters](common-server-parameters.md)
- [create goal](create.md) - Install Liberty and create server
- [install-server goal](install-server.md) - Install Liberty runtime only
- [dev goal](dev.md) - Development mode with hot reload