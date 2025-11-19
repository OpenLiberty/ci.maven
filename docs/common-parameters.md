#### Common Parameters

Parameters shared by all goals.

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| installDirectory | Local installation directory of the Liberty server. | Yes, only when `runtimeArchive`, `runtimeArtifact`, and `install` parameters are not set. |
| runtimeArchive | Location of the Liberty server compressed archive. The archive will be unpacked into a directory as specified by the `runtimeInstallDirectory` parameter. | Yes, only when `installDirectory`, `runtimeArtifact`, and `install` parameters are not set. |
| runtimeArtifact | Maven artifact name of the Liberty runtime. The runtime will be installed into a directory as specified by the `runtimeInstallDirectory` parameter. The default runtime is the latest version of `io.openliberty:openliberty-kernel`. | No |
| libertyRuntimeGroupId | Liberty runtime groupId to use instead of the `runtimeArtifact` groupId. This can also be specified with `-Dliberty.runtime.groupId` from the command line. | No |
| libertyRuntimeArtifactId | Liberty runtime artifactId to use instead of the `runtimeArtifact` artifactId. This can also be specified with `-Dliberty.runtime.artifactId` from the command line. | No |
| libertyRuntimeVersion | Liberty runtime version to use instead of the `runtimeArtifact` version. This can also be specified with `-Dliberty.runtime.version` from the command line. | No |
| install | Install Liberty runtime from the [Liberty repository](installation-configuration.md#using-the-install-liberty-ant-task). | Yes, only when `installDirectory`, `runtimeArchive`, and `runtimeArtifact` parameters are not set. |
| licenseArtifact | Maven artifact name of the Liberty license jar. It will be used to upgrade the installation at the location specified by the `runtimeInstallDirectory` parameter. | No |
| serverName | Name of the Liberty server instance. The default value is `defaultServer`. | No |
| userDirectory | Alternative user directory location that contains server definitions and shared resources. The default value is `usr` located in the Liberty runtime installation directory. | No |
| outputDirectory | Alternative location for server generated output such as logs, the _workarea_ directory, and other generated files (`WLP_OUTPUT_DIR`). The default value for the `package` and `install-feature` goals is `${project.build.directory}/liberty-alt-output-dir`. | No |
| runtimeInstallDirectory | Local installation directory location of the Liberty server when the server is installed using the runtime archive, runtime artifact or repository option. The default value is `${project.build.directory}/liberty`.  | No |
| refresh | If true, re-install Liberty server into the local directory. This is only used when when the server is installed using the runtime archive or runtime artifact option. The default value is false. | No |
| skip | If true, the specified goal is bypassed entirely. The default value is false. | No |

#### Backward Compatibility

The following parameter names from version 2.x are still supported for backward compatibility, but the new parameter names listed above should be used:

| 2.x Parameter Name       | 3.0+ Parameter Name     |
|--------------------------|-------------------------|
| assemblyArchive          | runtimeArchive          |
| assemblyArtifact         | runtimeArtifact         |
| assemblyInstallDirectory | runtimeInstallDirectory |

See the [version 3.0 migration guide](version_3.0_differences.md) for more information about parameter name changes.