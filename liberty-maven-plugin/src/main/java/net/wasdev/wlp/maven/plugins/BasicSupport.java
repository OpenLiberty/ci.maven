/**
 * (C) Copyright IBM Corporation 2014, 2017.
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
package net.wasdev.wlp.maven.plugins;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.wasdev.wlp.ant.install.InstallLibertyTask;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.apache.tools.ant.taskdefs.Chmod;
import org.apache.tools.ant.taskdefs.Expand;
import org.codehaus.mojo.pluginsupport.util.ArtifactItem;
import org.codehaus.plexus.util.FileUtils;

/**
 * Basic Liberty Mojo Support
 * 
 * 
 */
public class BasicSupport extends AbstractLibertySupport {

    //Note these next two are regular expressions, not just the code.
    protected static final String START_APP_MESSAGE_REGEXP = "CWWKZ0001I.*";

    protected static final ResourceBundle messages = ResourceBundle.getBundle("net.wasdev.wlp.maven.plugins.MvnMessages");

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
    @Parameter(property = "assemblyInstallDirectory", defaultValue = "${project.build.directory}/liberty")
    protected File assemblyInstallDirectory;
    
    /**
     * Installation directory of Liberty profile. 
     */
    @Parameter(property = "installDirectory")
    protected File installDirectory;

    /**
     * @deprecated Use installDirectory parameter instead.
     */
    @Parameter(property = "serverHome")
    private File serverHome;

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

    /**
     * Liberty output directory (<tT>WLP_OUTPUT_DIR</tt>).
     */
    @Parameter(property = "outputDirectory")
    protected File outputDirectory = null;
    
    /**
     * Server Directory: ${installDirectory}/usr/servers/${serverName}/
     */
    protected File serverDirectory;

    /**
     * Location of customized configuration directory
     */
    @Parameter(property = "configDirectory")
    protected File configDirectory;    
    
    /**
     * Location of customized configuration file server.xml
     */
    @Parameter(property = "configFile", defaultValue = "${basedir}/src/test/resources/server.xml")
    protected File configFile;    
    
    /**
     * Application directory.
     */
    @Parameter(property = "appsDirectory", readonly = true)
    protected String appsDirectory = null;

    protected static enum InstallType {
        FROM_FILE, ALREADY_EXISTS, FROM_ARCHIVE
    }

    protected InstallType installType;

    /**
     * A file which points to a specific assembly ZIP archive. If this parameter
     * is set, then it will install server from archive
     */
    @Parameter(property = "assemblyArchive")
    protected File assemblyArchive;

    /**
     * Maven coordinates of a server assembly. This is best listed as a dependency, in which case the version can
     * be omitted.
     */
    @Parameter
    protected ArtifactItem assemblyArtifact;
    
    /**
     * Liberty install option. If set, Liberty will be downloaded and installed from the WASdev repository or 
     * the given URL.
     */
    @Parameter
    protected Install install;

    @Override
    protected void init() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            return;
        }        
        super.init();
        // for backwards compatibility
        if (installDirectory == null) {
            installDirectory = serverHome;
        }
        try {
            // First check if installDirectory is set, if it is, then we can skip this
            if (installDirectory != null) {
                installDirectory = installDirectory.getCanonicalFile();

                // Quick sanity check
                File file = new File(installDirectory, "lib/ws-launch.jar");
                if (!file.exists()) {
                    throw new MojoExecutionException(MessageFormat.format(messages.getString("error.server.home.validate"), ""));
                }

                log.info(MessageFormat.format(messages.getString("info.variable.set"), "pre-installed assembly", installDirectory));
                installType = InstallType.ALREADY_EXISTS;
            } else if (assemblyArchive != null) {
                log.info(MessageFormat.format(messages.getString("info.variable.set"), "non-artifact based assembly archive", assemblyArchive));
                assemblyArchive = assemblyArchive.getCanonicalFile();
                installType = InstallType.FROM_FILE;
                installDirectory = checkServerHome(assemblyArchive);
                log.info(MessageFormat.format(messages.getString("info.variable.set"), "installDirectory", installDirectory));
            } else if (assemblyArtifact != null) {
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
            } else {
                if (install == null) {
                    install = new Install();
                }
                installType = InstallType.FROM_ARCHIVE;
                installDirectory = new File(assemblyInstallDirectory, "wlp");
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
            }
            
            File serversDirectory = new File(userDirectory, "servers");
            
            // Set server directory
            serverDirectory = new File(serversDirectory, serverName);
            
            log.info(MessageFormat.format(messages.getString("info.variable.set"), "serverDirectory", serverDirectory));
            
            // Set output directory
            if (outputDirectory == null) {
                outputDirectory = serversDirectory; 
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

    protected void setAppsDirectory(Artifact artifact) throws Exception{

        boolean bAppConfigured = isApplicationConfigured();
        
        if (appsDirectory == null || appsDirectory.isEmpty()) {
            appsDirectory = bAppConfigured ? "apps" : "dropins";
            log.info(MessageFormat.format(messages.getString("info.install.app.directory"), appsDirectory));
        }
        else if (appsDirectory.equalsIgnoreCase("dropins")) {
            if (bAppConfigured) {
                throw new MojoExecutionException(messages.getString("error.install.app.dropins.directory"));
            }
        }
        // appsDirectory is not null nor 'dropins' 
        //     i.e. 'apps' or other values
        else {
            if (!bAppConfigured && artifact != null) {        
            	// Produce a warning message that the application is not configured in the source server.xml or included config.
                log.warn(messages.getString("info.install.app.not.configured"));
               
            	// Add webApplication configuration into the target server.xml. 
            	File serverXML = new File(serverDirectory, "server.xml");
        		ServerXmlDocument.addAppElment(serverXML, artifact.getArtifactId());

        		log.info(MessageFormat.format(messages.getString("info.install.app.add.configuration"), artifact.getArtifactId()));
            }
        }
    }
    
    protected boolean isApplicationConfigured() throws Exception {
    	boolean bAppConfigured = false;

    	File serverXML = new File(serverDirectory, "server.xml");
	   
        bAppConfigured = ServerXmlDocument.isFoundTagNames(
				    		serverXML.getCanonicalPath(), 	
				    		new String[] {"application", "webApplication"});

        return bAppConfigured;
    }
    
    /* 
     * Get the file from configDrectory if it exists;
     * otherwise return def only if it exists, or null if not
     */
    protected File getFileFromConfigDirectory(String file, File def) {
        File f = new File(configDirectory, file);
        if (configDirectory != null && f.exists()) { 
            return f;
        }
        if (def != null && def.exists()) {
            return def;
        } 
        return null;
    }
    /**
     * Performs assembly installation unless the install type is pre-existing.
     * 
     * @throws Exception
     */
    protected void installServerAssembly() throws Exception {
        if (installType == InstallType.ALREADY_EXISTS) {
            log.info(MessageFormat.format(messages.getString("info.install.type.preexisting"), ""));
        } else if (installType == InstallType.FROM_ARCHIVE) {
            installFromArchive();
        } else {
            installFromFile();
        }
    }
    
    protected void installFromFile() throws Exception {
        // Check if there is a newer archive or missing marker to trigger assembly install
        File installMarker = new File(installDirectory, ".installed");

        if (!refresh) {
            if (!installMarker.exists()) {
                refresh = true;
            } else if (assemblyArchive.lastModified() > installMarker.lastModified()) {
                log.debug(MessageFormat.format(messages.getString("debug.detect.assembly.archive"), ""));
                refresh = true;
            }
        } else {
            log.debug(MessageFormat.format(messages.getString("debug.request.refresh"), ""));
        }

        if (refresh) {
            if (installDirectory.exists()) {
                log.info(MessageFormat.format(messages.getString("info.uninstalling.server.home"), installDirectory));
                FileUtils.forceDelete(installDirectory);
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

        } else {
            log.info(MessageFormat.format(messages.getString("info.reuse.installed.assembly"), ""));
        }
    }

    protected void installFromArchive() throws Exception {
        InstallLibertyTask installTask = (InstallLibertyTask) ant.createTask("antlib:net/wasdev/wlp/ant:install-liberty");
        if (installTask == null) {
            throw new NullPointerException("install-liberty task not found");
        }
        installTask.setBaseDir(assemblyInstallDirectory.getAbsolutePath());
        installTask.setLicenseCode(install.getLicenseCode());
        installTask.setVersion(install.getVersion());
        installTask.setRuntimeUrl(install.getRuntimeUrl());
        installTask.setVerbose(install.isVerbose());
        installTask.setMaxDownloadTime(install.getMaxDownloadTime());
        installTask.setType(install.getType());
        installTask.setOffline(settings.isOffline());
        
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
}
