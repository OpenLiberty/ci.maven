/**
 * (C) Copyright IBM Corporation 2014, 2020.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openliberty.tools.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.apache.tools.ant.taskdefs.Chmod;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Commandline.Argument;
import org.codehaus.mojo.pluginsupport.util.ArtifactItem;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

import io.openliberty.tools.ant.install.InstallLibertyTask;
import io.openliberty.tools.common.plugins.util.ServerFeatureUtil;

/**
 * Basic Liberty Mojo Support
 * 
 * 
 */
public class BasicSupport extends AbstractLibertySupport {

    //Note these next two are regular expressions, not just the code.
    protected static final String START_APP_MESSAGE_REGEXP = "CWWKZ0001I.*";

    protected static final ResourceBundle messages = ResourceBundle.getBundle("io.openliberty.tools.maven.MvnMessages");

    protected boolean defaultOutputDirSet = false;

    /**
     * Skips the specific goal
     */
    @Parameter(property = "skip", defaultValue = "false")
    protected boolean skip = false;

    /**
     * Enable forced install refresh.
     */
    @Parameter(property = "refresh", defaultValue = "false")
    protected boolean refresh = false;

    /**
     * Set the false to skip the installation of the assembly, re-using anything
     * that is already there.
     */
    @Parameter(property = "isInstall", defaultValue = "true")
    protected boolean isInstall = true;

    /**
     * Server Install Directory
     */
    @Parameter(alias = "runtimeInstallDirectory", property = "runtimeInstallDirectory", defaultValue = "${project.build.directory}/liberty")
    protected File assemblyInstallDirectory;
    
    /**
     * Installation directory of Liberty profile. 
     */
    @Parameter(property = "installDirectory")
    protected File installDirectory;

    /**
     * Liberty server name, default is defaultServer
     */
    @Parameter(property = "serverName", defaultValue = "defaultServer")
    protected String serverName = null;
    
    /**
     * Liberty user directory (<tT>WLP_USER_DIR</tt>).
     */
    @Parameter(property = "userDirectory")
    protected File userDirectory = null;

    protected boolean userDirectorySpecified = false;  // indicates whether userDirectory was configured

    /**
     * Liberty output directory (<tT>WLP_OUTPUT_DIR</tt>).
     */
    @Parameter(property = "outputDirectory")
    protected File outputDirectory = null;
    
    @Parameter(property = "useOpenLiberty")
    private boolean useOpenLiberty = true;
    
    /**
     * Server Directory: ${installDirectory}/usr/servers/${serverName}/
     */
    protected File serverDirectory;

    protected static enum InstallType {
        FROM_FILE, ALREADY_EXISTS, FROM_ARCHIVE
    }

    protected InstallType installType;

    /**
     * A file which points to a specific assembly ZIP archive. If this parameter
     * is set, then it will install server from archive
     */
    @Parameter(alias="runtimeArchive", property = "runtimeArchive")
    protected File assemblyArchive;

    /**
     * Maven coordinates of a server assembly. This is best listed as a dependency, in which case the version can
     * be omitted.
     */
    @Parameter(alias="runtimeArtifact", property="runtimeArtifact")
    protected ArtifactItem assemblyArtifact;
    
    /**
     * Liberty install option. If set, Liberty will be downloaded and installed from the WASdev repository or 
     * the given URL.
     */
    @Parameter
    protected Install install;

    /**
     * Maven coordinates of a liberty license artifact. This is best listed as a dependency, in which case the version can
     * be omitted.
     */
    @Parameter
    protected ArtifactItem licenseArtifact;

    /**
     * Location of customized configuration directory
     */
    @Parameter(property = "configDirectory", defaultValue = "${basedir}/src/main/liberty/config")
    protected File configDirectory;

    /**
     * Location of customized server environment file server.env
     */
    @Parameter(alias="serverEnv", property = "serverEnvFile")
    protected File serverEnvFile;

    /**
     * If set, merges server.env contents from default server.env, configDir server.env, inline properties,  and specified server.env
     */
    @Parameter(property = "mergeServerEnv", defaultValue = "false")
    protected boolean mergeServerEnv;

    /**
     * Version to override any specified in the assemblyArtifact
     */
    @Parameter(alias = "libertyRuntimeVersion", property = "liberty.runtime.version")
    protected String libertyRuntimeVersion = null;
    
    @Override
    protected void init() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            return;
        }        
        super.init();

        try {
            // First check if installDirectory is set, if it is, then we can skip this
            if (installDirectory != null) {
                installDirectory = installDirectory.getCanonicalFile();

                // Quick sanity check
                File file = new File(installDirectory, "lib/ws-launch.jar");
                if (!file.exists()) {
                    throw new MojoExecutionException(MessageFormat.format(messages.getString("error.install.dir.validate"), ""));
                }

                log.info(MessageFormat.format(messages.getString("info.variable.set"), "pre-installed assembly", installDirectory));
                installType = InstallType.ALREADY_EXISTS;
            } else if (assemblyArchive != null) {
                log.info(MessageFormat.format(messages.getString("info.variable.set"), "non-artifact based assembly archive", assemblyArchive));
                assemblyArchive = assemblyArchive.getCanonicalFile();
                installType = InstallType.FROM_FILE;
                installDirectory = checkServerHome(assemblyArchive);
                log.info(MessageFormat.format(messages.getString("info.variable.set"), "installDirectory", installDirectory));
            } else if(install != null) {
                installType = InstallType.FROM_ARCHIVE;
                installDirectory = new File(assemblyInstallDirectory, "wlp");
                log.info(MessageFormat.format(messages.getString("info.variable.set"), "installDirectory", installDirectory));
            }
            else { // default to install from runtime artifact
                assemblyArtifact.setType("zip");
                if(assemblyArtifact.getGroupId() == null) {
                    log.debug("Defaulting runtimeArtifact group id to 'io.openliberty'");
                    assemblyArtifact.setGroupId("io.openliberty");
                }
                if(assemblyArtifact.getArtifactId() == null) {
                    log.debug("Defaulting runtimeArtifact artifact id to 'openliberty-kernel'");
                    assemblyArtifact.setArtifactId("openliberty-kernel");
                }
                
                // check for liberty.runtime.version property which overrides any version set in the assemblyArtifact
                if (libertyRuntimeVersion != null) {
                    if (assemblyArtifact.getVersion() != null) {
                        log.info("The runtimeArtifact version " + assemblyArtifact.getVersion() + " is overwritten by the liberty.runtime.version value "+ libertyRuntimeVersion +".");
                    } else {
                        log.info("The liberty.runtime.version property value "+ libertyRuntimeVersion +" is used for the runtimeArtifact version.");
                    }
                    assemblyArtifact.setVersion(libertyRuntimeVersion);
                }
                else {
                    if(assemblyArtifact.getVersion() == null) {
                        log.debug("Defaulting runtimeArtifact version to '[19.0.0.6,)'");
                        assemblyArtifact.setVersion("[19.0.0.6,)");
                    }
                }
                
                Artifact artifact = getArtifact(assemblyArtifact);                
                
                assemblyArchive = artifact.getFile();
                if (assemblyArchive == null) {
                    throw new MojoExecutionException(MessageFormat.format(messages.getString("error.server.assembly.validate"), "artifact based assembly archive", ""));
                }
                log.info(MessageFormat.format(messages.getString("info.variable.set"), "artifact based assembly archive", assemblyArtifact));
                assemblyArchive = assemblyArchive.getCanonicalFile();
                installType = InstallType.FROM_FILE;
                installDirectory = checkServerHome(assemblyArchive);
                log.info(MessageFormat.format(messages.getString("info.variable.set"), "installDirectory", installDirectory));
            }

            // set server name
            if (serverName == null) {
                serverName = "defaultServer";
            }

            log.info(MessageFormat.format(messages.getString("info.variable.set"), "serverName", serverName));
                                  
            // Set user directory
            if (userDirectory == null) {
                userDirectory = new File(installDirectory, "usr");
            } else {
                userDirectorySpecified = true;
            }
            
            File serversDirectory = new File(userDirectory, "servers");
            
            // Set server directory
            serverDirectory = new File(serversDirectory, serverName);
            
            log.info(MessageFormat.format(messages.getString("info.variable.set"), "serverDirectory", serverDirectory));
                     
            // Set output directory
            if (getWlpOutputDir() != null) {
                outputDirectory = new File(getWlpOutputDir());
            } else if (outputDirectory == null) {
                outputDirectory = serversDirectory;
                defaultOutputDirSet = true;
            }
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    protected void checkServerHomeExists() throws MojoExecutionException {
        if (!installDirectory.exists()) {
            throw new MojoExecutionException(MessageFormat.format(messages.getString("error.server.home.noexist"), installDirectory));
        }
    }
    
    protected void checkServerDirectoryExists() throws MojoExecutionException {
        if (!serverDirectory.exists()) {
            throw new MojoExecutionException(MessageFormat.format(messages.getString("error.server.noexist"), serverName));
        }
    }
    
    private File checkServerHome(final File archive) throws IOException,
                    MojoExecutionException {
        log.debug(MessageFormat.format(messages.getString("debug.discover.server.home"), ""));

        File dir = null;
        ZipFile zipFile = null;

        try {
            zipFile = new ZipFile(archive);

            Enumeration<?> n = zipFile.entries();
            while (n.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) n.nextElement();
                if (entry.getName().endsWith("lib/ws-launch.jar")) {
                    File file = new File(assemblyInstallDirectory, entry.getName());
                    dir = file.getParentFile().getParentFile();
                    break;
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException(MessageFormat.format(messages.getString("error.discover.server.home.fail"), archive), e);
        } finally {
            try {
                zipFile.close();
            } catch (Exception e) {
                //Ignore it.
            }

        }

        if (dir == null) {
            throw new MojoExecutionException(MessageFormat.format(messages.getString("error.archive.not.contain.server"), archive));
        }

        return dir.getCanonicalFile();
    }

    /**
     * Performs assembly installation unless the install type is pre-existing.
     * 
     * @throws Exception
     */
    protected void installServerAssembly() throws Exception {
        if (installType == InstallType.ALREADY_EXISTS) {
            log.info(MessageFormat.format(messages.getString("info.install.type.preexisting"), ""));
        } else {
            if (installType == InstallType.FROM_ARCHIVE) {
                installFromArchive();
            } else {
                installFromFile();
            }
            installLicense();
        }
    }
    
    protected void installFromFile() throws Exception {
        // Check if there is a different/newer archive or missing marker to trigger assembly install
        File installMarker = new File(installDirectory, ".installed");

        if (!refresh) {
            if (!installMarker.exists()) {
                refresh = true;
            } else if (assemblyArchive.lastModified() > installMarker.lastModified()) {
                log.debug(MessageFormat.format(messages.getString("debug.detect.assembly.archive"), ""));
                refresh = true;
            } else if(!assemblyArchive.getCanonicalPath().equals(FileUtils.fileRead(installMarker))) {
                refresh = true;
            }
        } else {
            log.debug(MessageFormat.format(messages.getString("debug.request.refresh"), ""));
        }

        String userDirectoryPath = userDirectory.getCanonicalPath();
        if (refresh && installDirectory.exists() && installDirectory.isDirectory()) {
            log.info(MessageFormat.format(messages.getString("info.uninstalling.server.home"), installDirectory));
            // Delete everything in the install directory except usr directory
            for(File f : installDirectory.listFiles()) {
                if(!(f.isDirectory() && f.getCanonicalPath().equals(userDirectoryPath))) {
                    FileUtils.forceDelete(f);
                }
            }
        }

        // Install the assembly
        if (!installMarker.exists()) {
            log.info("Installing assembly...");

            FileUtils.forceMkdir(installDirectory);

            Expand unzip = (Expand) ant.createTask("unzip");

            unzip.setSrc(assemblyArchive);
            unzip.setDest(assemblyInstallDirectory.getCanonicalFile());
            unzip.execute();

            // Make scripts executable, since Java unzip ignores perms
            Chmod chmod = (Chmod) ant.createTask("chmod");
            chmod.setPerm("ugo+rx");
            chmod.setDir(installDirectory);
            chmod.setIncludes("bin/*");
            chmod.setExcludes("bin/*.bat");
            chmod.execute();

            // delete installMarker first in case it was packaged with the assembly
            installMarker.delete();
            installMarker.createNewFile();
            
            // Write the assembly archive path so we can determine whether to install a different assembly in future invocations
            FileUtils.fileWrite(installMarker, assemblyArchive.getCanonicalPath());
        } else {
            log.info(MessageFormat.format(messages.getString("info.reuse.installed.assembly"), ""));
        }
    }

    protected void installFromArchive() throws Exception {
        InstallLibertyTask installTask = (InstallLibertyTask) ant.createTask("antlib:io/openliberty/tools/ant:install-liberty");
        if (installTask == null) {
            throw new IllegalStateException(MessageFormat.format(messages.getString("error.dependencies.not.found"), "install-liberty"));
        }
        installTask.setBaseDir(assemblyInstallDirectory.getAbsolutePath());
        installTask.setLicenseCode(install.getLicenseCode());
        installTask.setVersion(install.getVersion());
        installTask.setRuntimeUrl(install.getRuntimeUrl());
        installTask.setVerbose(install.isVerbose());
        installTask.setMaxDownloadTime(install.getMaxDownloadTime());
        installTask.setType(install.getType());
        installTask.setOffline(settings.isOffline());
        installTask.setUseOpenLiberty(useOpenLiberty);
        
        String cacheDir = install.getCacheDirectory();
        if (cacheDir == null) {
            File dir = new File(artifactRepository.getBasedir(), "wlp-cache");
            installTask.setCacheDir(dir.getAbsolutePath());
        } else {
            installTask.setCacheDir(cacheDir);
        }
        
        String serverId = install.getServerId();
        if (serverId != null) {
            Server server = settings.getServer(serverId);
            if (server == null) {
                throw new MojoExecutionException("Server id not found: " + serverId);
            }
            installTask.setUsername(server.getUsername());
            installTask.setPassword(server.getPassword());
        } else {
            installTask.setUsername(install.getUsername());
            installTask.setPassword(install.getPassword());
        }
        
        installTask.execute();
    }
    
    // Strip version string from name
    protected String stripVersionFromName(String name, String version) {
        int versionBeginIndex = name.lastIndexOf("-" + version);
        if ( versionBeginIndex != -1) {
            return name.substring(0, versionBeginIndex) + name.substring(versionBeginIndex + version.length() + 1);
        } else {
            return name;
        }
    }
    
    protected void installLicense() throws MojoExecutionException, IOException {
        if (licenseArtifact != null) {
            Artifact license = getArtifact(licenseArtifact);
            if (!hasSameLicense(license)) {
                log.info(MessageFormat.format(messages.getString("info.install.license"), 
                        licenseArtifact.getGroupId() + ":" + licenseArtifact.getArtifactId() + ":" + licenseArtifact.getVersion()));
                Java installLicenseTask = (Java) ant.createTask("java");
                installLicenseTask.setJar(license.getFile());
                Argument args = installLicenseTask.createArg();
                args.setLine("--acceptLicense " + assemblyInstallDirectory.getCanonicalPath());
                installLicenseTask.setTimeout(30000L);
                installLicenseTask.setFork(true);
                int rc = installLicenseTask.executeJava();
                if (rc != 0) {
                    throw new MojoExecutionException(MessageFormat.format(messages.getString("error.install.license"), 
                            licenseArtifact.getGroupId() + ":" + licenseArtifact.getArtifactId() + ":" + licenseArtifact.getVersion(), rc));
                }
            }
        }
    }
    
    protected void deleteApplication(File parent, File artifactFile) throws IOException {
        deleteApplication(parent, artifactFile.getName());
        if (artifactFile.getName().endsWith(".xml")) {
            deleteApplication(parent, artifactFile.getName().substring(0, artifactFile.getName().length() - 4));
        } else {
            deleteApplication(parent, artifactFile.getName() + ".xml");
        }
    }
    
    protected void deleteApplication(File parent, String filename) throws IOException {
        File application = new File(parent, filename);
        if (application.isDirectory()) {
            // application can be installed with expanded format
            FileUtils.deleteDirectory(application);
        } else {
            application.delete();
        }
    }
    
    private boolean hasSameLicense(Artifact license) throws MojoExecutionException, IOException {
        boolean sameLicense = false;
        if (license != null) {
            InputStream licenseInfo = getZipEntry(license.getFile(), "wlp/lafiles/LI_en");
            if (licenseInfo == null) {
                log.warn(MessageFormat.format(messages.getString("warn.install.license"), license.getId()));
                return sameLicense;
            } 
            
            File lic = new File(assemblyInstallDirectory, "wlp/lafiles/LI_en");
            if (lic.exists()) {  
                FileInputStream installedLicenseInfo = new FileInputStream(lic);
                sameLicense = IOUtil.contentEquals(licenseInfo, installedLicenseInfo);
                licenseInfo.close();
                installedLicenseInfo.close();
            }
        }
        return sameLicense;
    }
    
    private InputStream getZipEntry(File zip, String entry) throws IOException {
        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zip));
        for (ZipEntry e; (e = zipInputStream.getNextEntry()) != null;) {
            if (e.getName().equals(entry)) {
                return zipInputStream;
            }
        }
        return null;
    }

    // Read WLP_OUTPUT_DIR from server.env. Return null if server.env doesn't
    // exist or variable is not in server.env
    private String getWlpOutputDir() throws IOException {
        Properties envvars = new Properties();
        
        File serverEnvInstallDir = new File(installDirectory, "etc/server.env");
        if (serverEnvInstallDir.exists()) {
            envvars.load(new FileInputStream(serverEnvInstallDir));
        }

        if (serverEnvFile != null && serverEnvFile.exists()) {
            envvars.load(new FileInputStream(serverEnvFile));
        } else {
            File serverEnvFileConfigDir = new File(configDirectory, "server.env");
            if (serverEnvFileConfigDir.exists()) {
                envvars.load(new FileInputStream(serverEnvFileConfigDir));
            }    
        }
                
        return (String) envvars.get("WLP_OUTPUT_DIR");
    }

    static public Map<String,File> getLibertyDirectoryPropertyFiles(File installDir, File userDir, File serverDir) throws IOException {
        Map<String, File> libertyDirectoryPropertyToFile = new HashMap<String,File>();

        if (serverDir.exists()) {
            libertyDirectoryPropertyToFile.put(ServerFeatureUtil.SERVER_CONFIG_DIR, serverDir.getCanonicalFile());

            libertyDirectoryPropertyToFile.put(ServerFeatureUtil.WLP_INSTALL_DIR, installDir.getCanonicalFile());
 
            libertyDirectoryPropertyToFile.put(ServerFeatureUtil.WLP_USER_DIR, userDir.getCanonicalFile());

            File userExtDir = new File(userDir, "extension");
            libertyDirectoryPropertyToFile.put(ServerFeatureUtil.USR_EXTENSION_DIR, userExtDir.getCanonicalFile());

            File userSharedDir = new File(userDir, "shared");
            File userSharedAppDir = new File(userSharedDir, "app");
            File userSharedConfigDir = new File(userSharedDir, "config");
            File userSharedResourcesDir = new File(userSharedDir, "resources");
            File userSharedStackGroupsDir = new File(userSharedDir, "stackGroups");

            libertyDirectoryPropertyToFile.put(ServerFeatureUtil.SHARED_APP_DIR, userSharedAppDir.getCanonicalFile());
            libertyDirectoryPropertyToFile.put(ServerFeatureUtil.SHARED_CONFIG_DIR, userSharedConfigDir.getCanonicalFile());
            libertyDirectoryPropertyToFile.put(ServerFeatureUtil.SHARED_RESOURCES_DIR, userSharedResourcesDir.getCanonicalFile());
            libertyDirectoryPropertyToFile.put(ServerFeatureUtil.SHARED_STACKGROUP_DIR, userSharedStackGroupsDir.getCanonicalFile());
        }
        return libertyDirectoryPropertyToFile;
    }

    protected Map<String,File> getLibertyDirectoryPropertyFiles() {
        
        if (serverDirectory.exists()) {
            try {
                return getLibertyDirectoryPropertyFiles(installDirectory, userDirectory, serverDirectory);
            } catch (Exception e) {
                log.warn("The properties for directories could not be initialized because an error occurred when accessing the directories.");
                log.debug("Exception received: "+e.getMessage(), (Throwable) e);
            }
        } else {
            log.warn("The " + serverDirectory + " directory cannot be accessed. The properties for directories could not be initialized.");
        }

        return new HashMap<String,File> ();
    }

}
