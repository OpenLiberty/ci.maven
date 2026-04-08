## prepare-config

---

Prepare Liberty configuration and generate `liberty-plugin-config.xml` without creating the server or installing Liberty. This lightweight goal evaluates project configuration and generates metadata needed by IDE tools and language servers.

This goal is particularly useful for:
- Enabling IDE support for Liberty configuration files (server.xml, bootstrap.properties, server.env)
- Providing configuration metadata to language servers for code completion and diagnostics
- Supporting Liberty Tools and other IDE extensions
- Quick configuration validation without full project build

The goal does NOT install Liberty or create a server. It only generates the configuration metadata file based on your project's Maven configuration.

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

The `prepare-config` goal supports the following configuration parameter in addition to the [common parameters](common-parameters.md) and [common server parameters](common-server-parameters.md):

| Parameter | Description | Required | Default |
| --------- | ----------- | -------- | ------- |
| includeServerInfo | Whether to include server-specific information in the generated config. When `true`, includes `server.xml`, `bootstrap.properties`, `jvm.options`, etc. When `false`, only includes project and build metadata. | No | `true` |

---

### Examples

#### Example 1: Basic usage (default)

Generate configuration metadata with server information:

```bash
mvn liberty:prepare-config
```

This will create `target/liberty-plugin-config.xml` with project metadata, dependencies, and configuration file references.

#### Example 2: Generate minimal configuration

Generate only project metadata without server-specific information:

```bash
mvn liberty:prepare-config -DincludeServerInfo=false
```

This creates a minimal configuration file with just project and build information, and executes faster.

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
            <configuration>
                <includeServerInfo>true</includeServerInfo>
            </configuration>
        </execution>
    </executions>
</plugin>
```

---

### Generated Configuration File

The `prepare-config` goal generates `target/liberty-plugin-config.xml` containing:

**Always included:**
- Install directory (if Liberty is already installed)
- Server name and directory paths
- Project type (packaging)
- Active build profiles
- Project compile dependencies
- Aggregator parent information (for multi-module projects)

**Included when `includeServerInfo=true`:**
- Configuration directory
- Server configuration file (`server.xml`)
- Bootstrap properties file (`bootstrap.properties`)
- JVM options file (`jvm.options`)
- Server environment file (`server.env`)
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

| Goal | Liberty Install | Server Creation | Config Files Copied | Use Case |
|------|----------------|-----------------|---------------------|----------|
| `prepare-config` | No | No | No | Generate config metadata for tools |
| `create` | Yes | Yes | Yes | Create and configure Liberty server |
| `install-server` | Yes | No | No | Install Liberty runtime only |
| `dev` | Yes | Yes | Yes | Development mode with hot reload |

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

#### Missing server information

If server-specific information is missing, ensure `includeServerInfo=true`:

```bash
mvn liberty:prepare-config -DincludeServerInfo=true
```

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