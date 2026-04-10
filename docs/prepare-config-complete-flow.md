# Liberty Config Language Server Integration - Complete Flow

## Overview
This document describes the integration between Liberty Maven/Gradle plugins and the Liberty Config Language Server (LCLS), focusing on the prepare-config goal/task and on-demand configuration generation.

## Metadata Used by LCLS from liberty-plugin-config.xml

### lemminx-liberty (Language Server)
- **installDirectory** - Load Liberty runtime metadata for variable resolution
- **serverDirectory** - Locate server.xml and related config files
- **userDirectory** - Access shared configuration files
- **serverOutputDirectory** - Resolve output directory variables
- **configFile** - Custom server.xml location, used for non-standard paths

### liberty-ls (Legacy Language Server)
- **serverEnv** - Custom server.env file path
- **bootstrapPropertiesFile** - Custom bootstrap.properties file path
- **configDirectory** - Configuration directory location

---

## Part 1: Liberty Plugin Flow (Maven/Gradle)

### Command Execution
```bash
# Maven
mvn liberty:prepare-config

# Gradle
gradle libertyPrepareConfig
```

### Step 1: Plugin Invocation
User or IDE executes prepare-config command. Plugin framework initializes the goal/task and reads configuration from pom.xml (Maven) or build.gradle (Gradle).

### Step 2: Goal/Task Initialization
Reads build configuration and initializes plugin parameters:
- `includeServerInfo = true` (default) - Controls whether to include server-specific information
- `serverName` from configuration (default: "defaultServer")
- `installDirectory`, `configDirectory`, `userDirectory`, `outputDirectory` from configuration
- Validates skip parameter, exits early if skip is true

### Step 3: Create Mock Liberty Server Structure
Creates directory structure in build output folder:
- **Maven**: Creates `target/tmp/wlp/usr/servers/{serverName}/` directory structure
- **Gradle**: Creates `build/tmp/wlp/usr/servers/{serverName}/` directory structure
- Mimics actual Liberty server layout without installing Liberty
- All directories created with proper permissions
- Returns the mock server directory path for subsequent operations

### Step 4: Copy Configuration Files to Mock Server
Uses parent class `copyConfigFiles()` method which handles:
- Copies all files from configured `configDirectory` if specified
- Uses explicit file overrides (serverXmlFile, jvmOptionsFile, bootstrapPropertiesFile, serverEnvFile)
- Applies inline bootstrap properties and JVM options if configured in build file
- Executes `mergeServerEnv` logic if configured
- Resolves build property references (Maven: `${property}`, Gradle: `${property}`) in all config files
- Processes `<include>` elements in server.xml
- Temporarily sets `serverDirectory` to mock location before copying, then restores original value

### Step 5: Read Project Information
Extract project metadata from build configuration:
- Reads project coordinates: `groupId`, `artifactId`, `version`, `packaging` type
- Collects all compile-scope and runtime-scope dependencies with their coordinates
- Identifies active build profiles (Maven) or configurations (Gradle)

### Step 6: Generate Configuration XML

Builds mock directory paths and saves original directory values for restoration. Overrides directories to point to mock structure:
- **Maven**: Sets `installDirectory` to `target/tmp/wlp`
- **Gradle**: Sets `installDirectory` to `build/tmp/wlp`
- Sets `userDirectory` to `{buildDir}/tmp/wlp/usr`
- Sets `serverDirectory` to `{buildDir}/tmp/wlp/usr/servers/{serverName}`
- Sets `outputDirectory` to `{buildDir}/tmp/wlp/usr/servers`

Creates XML document with root element `<liberty-plugin-config>`:

**Always-Included Elements:**
- `<installDirectory>`, `<userDirectory>`, `<serverDirectory>`, `<serverOutputDirectory>` - All point to mock structure
- `<serverName>`, `<projectType>`, `<activeBuildProfiles>`, `<projectCompileDependency>` - Project metadata

**Server-Specific Information (if includeServerInfo=true):**
- `<configDirectory>`, `<configFile>`, `<bootstrapPropertiesFile>`, `<serverEnv>`, `<jvmOptionsFile>`
- `<appsDirectory>`, `<looseApplication>`, `<applicationFilename>`

Calls parent implementation with mock directories, then restores original directory values.

### Step 7: Write Configuration File
Writes XML content to build output directory:
- **Maven**: `target/liberty-plugin-config.xml`
- **Gradle**: `build/liberty-plugin-config.xml`

Closes file handles and verifies file was written successfully.

### Step 8: Completion
Logs success messages and returns success status to build tool.

---

## Part 2: Liberty Config Language Server (LCLS) Flow

### A. Extension Startup

#### Step 1: Initialize Extension
LemMinX (XML Language Server) starts and loads Liberty extension. Registers completion, hover, diagnostics, code actions, and document link participants.

#### Step 2: Discover Workspaces
LibertyProjectsManager receives workspace folders from IDE, recursively searches for XML files with `<server>` root element, detects multi-module Maven and Gradle projects, and creates LibertyWorkspace instances.

#### Step 3: Initialize Variables Map
Calls SettingsService to initialize empty variables map. Map is created but not populated with values yet. Actual variable population happens on-demand when needed. This ensures map is never null when accessed later.

#### Step 4: Start File Monitoring
FileWatchService monitors workspace directories for changes to server.xml, bootstrap.properties, server.env, jvm.options, and files referenced via `<include>` elements. When changes detected, triggers variable reload for affected workspace.

### B. File Open Detection (Diagnostics Trigger)

#### Step 1: File Open Event
User opens server.xml or any Liberty config file in IDE. LemMinX triggers LibertyDiagnosticParticipant `doDiagnostics()` method.

#### Step 2: Trigger Automatic Config Generation
Gets workspace folder for the opened document. Checks if project has Liberty Maven or Gradle plugin configured in build file. Calls `needsConfigGeneration()` to determine if regeneration is needed. If needed, executes prepare-config command with 5-second timeout.

#### Step 3: Check for Configuration File (needsConfigGeneration)
Performs four checks to determine if config generation is needed:

**Check 1: Config file doesn't exist**
- **Maven**: Looks for liberty-plugin-config.xml in target directory
- **Gradle**: Looks for liberty-plugin-config.xml in build directory
- If not found, removes project from processed cache and returns true

**Check 2: Config points to mock server but mock directory missing**
- Reads config file and verifies mock directory exists
- **Maven**: Checks for `target/tmp/wlp/usr/servers/{serverName}` directory
- **Gradle**: Checks for `build/tmp/wlp/usr/servers/{serverName}` directory
- If missing (e.g., after clean), returns true

**Check 3: Already processed in this session**
- Checks if project path exists in processedProjects set
- If found, returns false to skip generation

**Check 4: Config is stale (older than build file)**
- Compares timestamps of liberty-plugin-config.xml and build file
- **Maven**: Compares with pom.xml timestamp
- **Gradle**: Compares with build.gradle or build.gradle.kts timestamp
- If config older, returns true

If all checks pass, returns false indicating no generation needed.

#### Step 4: Execute Config Generation
Detects build tool and constructs appropriate command:
- **Maven**: `mvn liberty:prepare-config`
- **Gradle**: `./gradlew libertyPrepareConfig` or `gradlew.bat libertyPrepareConfig` (Windows)

Creates ProcessBuilder, executes command, captures output, and checks exit code. Returns ConfigGenerationResult with success status, error message, and duration.

#### Step 5: Handle Generation Result

**If Successful:**
- Config file now exists with mock server structure created
- Adds project to processedProjects set
- **Immediately calls SettingsService to reload variables for this workspace**
- Variables are populated from newly generated config
- Proceeds with full diagnostic features

**If Failed:**
- Creates `.libertyls/prepare-config` directory and saves detailed error log to `build.log`
- Marks project as failed with error message and log file path
- Proceeds to show failure diagnostic to user

**If Timeout (>5 seconds):**
- Allows generation process to continue in background
- Config will be checked again on next diagnostic trigger or variable access

#### Step 6: Reload Variables After Successful Generation
After successful config generation, calls SettingsService `populateVariablesForWorkspace()` method. Reads newly generated liberty-plugin-config.xml, extracts directory paths, creates ServerConfigDocument, loads default Liberty properties and custom properties from bootstrap.properties, and stores variables in map.

#### Step 7: Show Failure Diagnostic (If Generation Failed)
If project marked as failed, creates warning diagnostic at line 1 of server.xml with error summary, resolution steps specific to Maven or Gradle, and clickable link to detailed error log at `.libertyls/prepare-config/build.log`.

**Maven Resolution Steps:**
1. Ensure liberty-maven-plugin version 3.11 or later
2. Run 'mvn liberty:prepare-config' manually
3. Check pom.xml configuration
4. Verify Maven is installed

**Gradle Resolution Steps:**
1. Ensure liberty-gradle-plugin version 3.11 or later
2. Run 'gradle libertyPrepareConfig' manually
3. Check build.gradle configuration
4. Verify Gradle is installed

#### Step 8: Perform Standard Diagnostics
After handling config generation, proceeds with standard Liberty diagnostics: validates XML structure, checks feature names, validates variable references, checks file paths in `<include>` elements, validates configuration values, and checks for deprecated features. All diagnostics use data from mock server structure.

### C. Variable Access (On-Demand Population)

#### Step 1: Variable Access Trigger
Any language server feature that needs variables triggers this flow: code completion for `${variable}` references, hover documentation, validation of variable references, or document link resolution.

#### Step 2: Get Variables for Server XML
Receives server.xml URI, calls LibertyProjectsManager to get workspace folder, checks if variables exist for workspace, and returns cached variables or empty properties.

#### Step 3: Populate Variables If Needed
Called when variables don't exist for workspace:
- Initializes variables map if null (lazy initialization)
- Calls `ensureConfigIsUpToDate()` to check if regeneration needed
- Reads config file from build output directory (target or build)
- Extracts directory paths (installDirectory, serverDirectory, userDirectory, serverOutputDirectory)
- Creates ServerConfigDocument with extracted paths (all pointing to mock structure)
- Loads default Liberty properties and custom properties from bootstrap.properties in mock server
- Stores variables in map with workspace URI as key

#### Step 4: Return Variables
Returns properties object with all variables for workspace. Calling feature uses variables for completion, hover, or validation.

### D. File Change Detection (FileWatchService)

FileWatchService monitors changes to Liberty configuration files in two locations:

**1. Source Config Directory** (`src/main/liberty/config/`):
- Monitors for ANY file changes in the source config directory
- When change detected, automatically triggers prepare-config to sync changes to mock server
- After successful regeneration, immediately reloads variables from updated mock server
- Provides seamless developer experience - changes take effect automatically

**2. Mock Server Directory** (`target/tmp/wlp/usr/servers/{serverName}/` or `build/tmp/wlp/usr/servers/{serverName}/`):
- Monitors for changes to config files in the mock server
- When change detected, reloads variables without regenerating config
- Handles cases where mock server is modified directly

**Flow when source config file changes:**
1. FileWatchService detects file modification in `src/main/liberty/config/`
2. Triggers prepare-config goal/task to regenerate mock server
3. Maven/Gradle plugin copies updated files from source to mock server
4. Resolves build properties in updated files
5. Generates new liberty-plugin-config.xml
6. Immediately calls SettingsService `populateVariablesForWorkspace()` method
7. Re-reads config and configuration files from updated mock server
8. Updates variable values in workspace variable map
9. Next diagnostic run uses updated values - no IDE restart required

---

## Benefits

✅ **Zero configuration** - Works immediately after project clone without setup  
✅ **Always up-to-date** - Config checked before every use, never stale  
✅ **Self-healing** - Automatically fixes stale or missing configs  
✅ **No Liberty installation required** - Mock server structure eliminates dependency  
✅ **Simpler architecture** - No file watcher complexity for config file  
✅ **No race conditions** - Synchronous on-demand checks  
✅ **Easier to debug** - Straightforward flow without async events  
✅ **Less resource usage** - No continuous monitoring overhead  
✅ **Build tool agnostic** - Works with both Maven and Gradle projects
