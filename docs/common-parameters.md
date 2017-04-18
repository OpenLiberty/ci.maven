#### Common Parameters

Parameters shared by all goals.

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| installDirectory | Local installation directory of the Liberty profile server. | Yes, only when `assemblyArchive`, `assemblyArtifact`, and `install` parameters are not set. |
| assemblyArchive | Location of the Liberty profile server compressed archive. The archive will be unpacked into a directory as specified by the `assemblyInstallDirectory` parameter. | Yes, only when `installDirectory`, `assemblyArtifact`, and `install` parameters are not set. |
| assemblyArtifact | Maven artifact name of the Liberty profile server assembly. The assembly will be installed into a directory as specified by the `assemblyInstallDirectory` parameter. | Yes, only when `installDirectory`, `assemblyArchive`, and `install` parameters are not set. |
| install | Install Liberty runtime from the [Liberty repository](docs/installation-configuration.md#using-a-repository). | Yes, only when `installDirectory`, `assemblyArchive`, and `assemblyArtifact` parameters are not set. |
| licenseArtifact | Maven artifact name of the Liberty license jar. It will be used to upgrade the installation at the location specified by the `assemblyInstallDirectory` parameter. | No |
| serverName | Name of the Liberty profile server instance. The default value is `defaultServer`. | No |
| userDirectory | Alternative user directory location that contains server definitions and shared resources (`WLP_USER_DIR`). | No |
| outputDirectory | Alternative location for server generated output such as logs, the _workarea_ directory, and other generated files (`WLP_OUTPUT_DIR`). | No |
| assemblyInstallDirectory | Local installation directory location of the Liberty profile server when the server is installed using the assembly archive, assembly artifact or repository option. The default value is `${project.build.directory}/liberty`.  | No |
| refresh | If true, re-install Liberty profile server into the local directory. This is only used when when the server is installed using the assembly archive or artifact option. The default value is false. | No |
| skip | If true, the specified goal is bypassed entirely. The default value is false. | No |
