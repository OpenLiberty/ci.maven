# Liberty Config Language Server - Common Scenarios

This document describes common scenarios when using the Liberty Config Language Server with Maven or Gradle projects.

---

## Scenario 1: First Time Opening server.xml

### Maven Project
1. User opens server.xml in IDE for the first time
2. LemMinX triggers diagnostics
3. Check: liberty-plugin-config.xml doesn't exist in target directory
4. Execute: `mvn liberty:prepare-config` in background with 5-second timeout
5. Maven Plugin executes:
   - Creates `target/tmp/wlp/usr/servers/defaultServer/` directory structure
   - Copies server.xml from `src/main/liberty/config/` to mock server
   - Copies bootstrap.properties to mock server if exists
   - Resolves Maven properties in all config files
   - Generates `target/liberty-plugin-config.xml` with paths to mock server
6. Config generation completes successfully within timeout
7. Immediately reload variables for this workspace:
   - SettingsService reads paths from config (all point to target/tmp)
   - Creates ServerConfigDocument with mock server paths
   - Loads default Liberty properties from metadata
   - Loads custom variables from bootstrap.properties in mock server
   - Stores variables in map for this workspace
8. Diagnostics run with full language server features enabled
9. User sees in IDE:
   - Code completion for features, variables, file paths
   - Hover documentation for elements and attributes
   - Error and warning diagnostics for invalid configuration
   - Clickable file paths in include elements
   - All features work immediately without manual intervention

### Gradle Project
Same flow as Maven, but:
- Check: liberty-plugin-config.xml doesn't exist in build directory
- Execute: `gradle libertyPrepareConfig` or `./gradlew libertyPrepareConfig`
- Creates `build/tmp/wlp/usr/servers/defaultServer/` directory structure
- Generates `build/liberty-plugin-config.xml` with paths to mock server

---

## Scenario 2: After Clean Build

### Maven Project (mvn clean)
1. User executes: `mvn clean` from terminal or IDE
2. Maven deletes entire target directory including:
   - `target/tmp/` (mock server structure with all config files)
   - `target/liberty-plugin-config.xml` (generated config file)
3. User opens server.xml in IDE
4. LemMinX triggers diagnostics
5. Check: liberty-plugin-config.xml doesn't exist in target directory
6. Check: Mock server directory missing
7. Remove project from processedProjects cache:
   - Allows regeneration even though previously successful
   - Ensures fresh generation after clean
8. Execute: `mvn liberty:prepare-config`
9. Maven Plugin recreates everything from scratch:
   - Creates `target/tmp/wlp/usr/servers/defaultServer/` directory
   - Copies server.xml from source to mock server
   - Copies bootstrap.properties from source to mock server
   - Copies all other config files from source to mock server
   - Resolves Maven properties in all copied files
   - Generates `target/liberty-plugin-config.xml` with mock paths
10. Immediately reload variables after successful generation:
    - Reads newly generated config file
    - Extracts directory paths pointing to new mock server
    - Loads variables from new bootstrap.properties
    - Stores variables in map for workspace
11. Full language server features restored automatically
12. User continues working with no manual intervention required
13. All features work as if clean never happened

### Gradle Project (gradle clean)
Same flow as Maven, but:
- Gradle deletes entire build directory
- Check: liberty-plugin-config.xml doesn't exist in build directory
- Execute: `gradle libertyPrepareConfig` or `./gradlew libertyPrepareConfig`
- Creates `build/tmp/wlp/usr/servers/defaultServer/` directory
- Generates `build/liberty-plugin-config.xml` with mock paths

---

## Scenario 3: After Build File Change

### Maven Project (pom.xml modified)
1. User modifies pom.xml:
   - Adds new dependency
   - Changes plugin configuration
   - Updates Liberty version
   - Modifies server name or directories
2. User saves pom.xml (now newer than liberty-plugin-config.xml)
3. User opens server.xml in IDE
4. LemMinX triggers diagnostics
5. Check: liberty-plugin-config.xml exists in target directory
6. Check: Is config stale (older than pom.xml)?
   - Result: YES (pom.xml modified timestamp is newer than config timestamp)
7. Execute: `mvn liberty:prepare-config` to regenerate with updated settings
8. Maven Plugin regenerates config:
   - Recreates mock server structure
   - Copies config files with updated Maven properties resolved
   - Generates new liberty-plugin-config.xml with updated dependencies and settings
9. Immediately reload variables after successful generation:
   - Reads new config file
   - Extracts updated directory paths
   - Loads updated variables from new bootstrap.properties
   - Updates variable map for workspace
10. Diagnostics run with updated configuration and variables
11. User sees updated features reflecting new dependencies and configuration
12. No manual intervention required - all automatic

### Gradle Project (build.gradle modified)
Same flow as Maven, but:
- User modifies build.gradle or build.gradle.kts
- Check: Is config stale (older than build.gradle)?
- Execute: `gradle libertyPrepareConfig` or `./gradlew libertyPrepareConfig`
- Gradle Plugin regenerates config with updated settings

---

## Scenario 4: Config Generation Fails

### Maven Project
1. User opens server.xml in IDE
2. LemMinX triggers diagnostics
3. Check: liberty-plugin-config.xml doesn't exist in target directory
4. Execute: `mvn liberty:prepare-config`
5. Maven Plugin fails due to:
   - Invalid pom.xml syntax
   - Missing liberty-maven-plugin configuration
   - Incorrect plugin version
   - Network error downloading dependencies
   - Missing required parameters
6. Capture error output from Maven command:
   - Reads all output lines from process
   - Captures both standard output and error streams
   - Stores complete output for logging
7. Save detailed error log to `.libertyls/prepare-config/build.log`:
   - Creates `.libertyls/prepare-config` directory if doesn't exist
   - Writes timestamp to log file
   - Writes full Maven output to log file
   - Writes error messages to log file
   - Writes stack traces if available to log file
8. Mark project as failed in LibertyConfigGenerationService:
   - Stores error message in failed projects map
   - Stores log file path in failed projects map
   - Associates failure with workspace URI
9. Warning diagnostic shown in server.xml at line 1:
   ```
   Liberty configuration generation failed. Full language server features 
   are unavailable until this is resolved.
   
   Error: [Brief error summary from Maven output]
   
   Resolution steps:
   1. Ensure liberty-maven-plugin version 3.11 or later
   2. Run 'mvn liberty:prepare-config' manually to see full error
   3. Check pom.xml configuration
   4. Verify Maven is installed and accessible
   
   [View detailed error log](.libertyls/prepare-config/build.log)
   ```
10. User clicks "View detailed error log" link in diagnostic
11. IDE opens `.libertyls/prepare-config/build.log` in editor
12. User reads full error details:
    - Sees complete Maven output
    - Identifies specific error (e.g., missing plugin version in pom.xml)
    - Understands what needs to be fixed
13. User fixes issue in pom.xml (e.g., adds correct plugin version)
14. User saves pom.xml (now newer than failed config attempt)
15. User reopens server.xml or triggers diagnostics
16. Config check detects stale config (pom.xml newer than last attempt)
17. Regenerates config successfully this time:
    - Maven plugin executes without errors
    - Creates mock server structure
    - Generates config file
18. Reloads variables immediately after successful generation
19. Project removed from failed projects list
20. Warning diagnostic automatically disappears from server.xml
21. Full language server features now available
22. User continues working normally

### Gradle Project
Same flow as Maven, but:
- Execute: `gradle libertyPrepareConfig` or `./gradlew libertyPrepareConfig`
- Gradle Plugin fails due to similar reasons (invalid build.gradle, missing plugin, etc.)
- Warning diagnostic shows Gradle-specific resolution steps:
  ```
  Liberty configuration generation failed. Full language server features 
  are unavailable until this is resolved.
  
  Error: [Brief error summary from Gradle output]
  
  Resolution steps:
  1. Ensure liberty-gradle-plugin version 3.11 or later
  2. Run 'gradle libertyPrepareConfig' manually to see full error
  3. Check build.gradle configuration
  4. Verify Gradle is installed and accessible
  
  [View detailed error log](.libertyls/prepare-config/build.log)
  ```
- User fixes issue in build.gradle or build.gradle.kts
- Regenerates successfully after fix

---

## Scenario 5: Config File Change Detection

### Maven Project
1. User modifies bootstrap.properties in `src/main/liberty/config/`
2. User adds new variable: `myapp.port=9080`
3. User saves bootstrap.properties
4. FileWatchService detects file modification
5. Listener calls SettingsService `populateVariablesForWorkspace()` method
6. SettingsService checks if config needs regeneration:
   - Calls `ensureConfigIsUpToDate()` method
   - Checks if pom.xml is newer than liberty-plugin-config.xml
   - If stale, regenerates config automatically
7. Re-reads config from `target/liberty-plugin-config.xml`
8. Reads bootstrap.properties from `target/tmp/wlp/usr/servers/defaultServer/bootstrap.properties`
9. Parses configuration files for variable definitions
10. Extracts new variable `myapp.port=9080`
11. Updates variable values in workspace variable map
12. Next diagnostic run uses updated values:
    - Variable references in server.xml validated against new values
    - Completion suggestions include new variable
    - Hover shows new variable value
13. No IDE restart required - changes take effect immediately

### Gradle Project
Same flow as Maven, but:
- Reads config from `build/liberty-plugin-config.xml`
- Reads bootstrap.properties from `build/tmp/wlp/usr/servers/defaultServer/bootstrap.properties`
---

## Scenario 7: Source Config File Modified (Automatic Sync)

This scenario demonstrates the automatic synchronization when a user modifies a config file in the source directory with the new file watching feature.

### Maven Project
1. User modifies `src/main/liberty/config/bootstrap.properties` (source directory)
2. User adds new variable: `myapp.database.url=jdbc:mysql://localhost:3306/mydb`
3. User saves bootstrap.properties
4. **FileWatchService automatically detects the change** in `src/main/liberty/config/` directory
5. FileWatchService triggers prepare-config automatically:
   - Executes `mvn liberty:prepare-config` in background
   - Maven Plugin copies updated bootstrap.properties from source to mock server
   - Resolves Maven properties in the updated file
   - Generates new liberty-plugin-config.xml
6. After successful regeneration, FileWatchService immediately reloads variables:
   - Calls SettingsService `populateVariablesForWorkspace()` method
   - Reads updated bootstrap.properties from `target/tmp/wlp/usr/servers/defaultServer/`
   - Extracts new variable `myapp.database.url=jdbc:mysql://localhost:3306/mydb`
   - Updates variable map for workspace
7. Next diagnostic run (or current if in progress) uses updated values:
   - Variable reference `${myapp.database.url}` now resolves correctly
   - Code completion suggests new variable
   - Hover shows new variable value
8. **No manual intervention required** - all automatic!

### Gradle Project
Same flow as Maven, but:
- FileWatchService detects change in `src/main/liberty/config/`
- Executes `gradle libertyPrepareConfig` or `./gradlew libertyPrepareConfig` automatically
- Gradle Plugin copies updated file to `build/tmp/wlp/usr/servers/defaultServer/`
- Variables reloaded from updated mock server

### Key Benefits

**Seamless Developer Experience:**
- Changes to source config files automatically sync to mock server
- No need to manually run prepare-config
- No need to delete config files or directories
- No need to restart IDE or language server
- Variables update immediately after file save

**How It Works:**
- FileWatchService monitors `src/main/liberty/config/` directory
- Detects ANY file change (create, modify) in that directory
- Automatically triggers prepare-config to sync changes
- Reloads variables after successful sync
- All happens in background without user intervention


---

## Scenario 6: Multi-Module Project

### Maven Multi-Module Project
1. Project structure:
   ```
   parent-project/
   ├── pom.xml (parent)
   ├── module-a/
   │   ├── pom.xml
   │   └── src/main/liberty/config/server.xml
   └── module-b/
       ├── pom.xml
       └── src/main/liberty/config/server.xml
   ```
2. User opens server.xml in module-a
3. LemMinX discovers workspace and identifies module-a as Liberty project
4. Check: liberty-plugin-config.xml doesn't exist in module-a/target
5. Execute: `mvn liberty:prepare-config` in module-a directory
6. Maven Plugin generates config for module-a:
   - Creates `module-a/target/tmp/wlp/usr/servers/defaultServer/`
   - Generates `module-a/target/liberty-plugin-config.xml`
7. Variables loaded for module-a workspace
8. User opens server.xml in module-b
9. LemMinX identifies module-b as separate Liberty project
10. Check: liberty-plugin-config.xml doesn't exist in module-b/target
11. Execute: `mvn liberty:prepare-config` in module-b directory
12. Maven Plugin generates config for module-b:
    - Creates `module-b/target/tmp/wlp/usr/servers/defaultServer/`
    - Generates `module-b/target/liberty-plugin-config.xml`
13. Variables loaded for module-b workspace
14. Each module has independent config and variables
15. Changes in one module don't affect the other

### Gradle Multi-Module Project
Same flow as Maven, but:
- Project structure uses build.gradle files
- Execute: `gradle libertyPrepareConfig` in each module directory
- Creates `module-a/build/tmp/` and `module-b/build/tmp/` directories
- Generates separate config files in each module's build directory