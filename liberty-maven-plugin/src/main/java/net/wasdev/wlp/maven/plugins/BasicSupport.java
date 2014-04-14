package net.wasdev.wlp.maven.plugins;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
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
     * Enable forced install refresh.
     * 
     * @parameter expression="${refresh}" default-value="false"
     */
    protected boolean refresh = false;

    /**
     * Set the false to skip the installation of the assembly, re-using anything
     * that is already there.
     * 
     * @parameter expression="${isInstall}" default-value="true"
     */
    protected boolean isInstall = true;

    /**
     * Server Install Directory
     * 
     * @parameter expression="${assemblyInstallDirectory}" default-value="${project.build.directory}/liberty"
     */
    protected File assemblyInstallDirectory;
    
    /**
     * Installation directory of Liberty profile. 
     * 
     * @parameter expression="${installDirectory}"
     */
    protected File installDirectory;

    /**
     * @deprecated Use installDirectory parameter instead.
     * @parameter expression="${serverHome}"
     */
    private File serverHome;

    /**
     * Liberty server name, default is defaultServer
     * 
     * @parameter expression="${serverName}" default-value="defaultServer"
     */
    protected String serverName = null;
    
    /**
     * Liberty user directory (<tT>WLP_USER_DIR</tt>).
     * 
     * @parameter expression="${userDirectory}"
     */
    protected File userDirectory = null;

    /**
     * Liberty output directory (<tT>WLP_OUTPUT_DIR</tt>).
     * 
     * @parameter expression="${outputDirectory}"
     */
    protected File outputDirectory = null;
    
    /**
     * Server Directory: ${installDirectory}/usr/servers/${serverName}/
     */
    protected File serverDirectory;

    protected static enum InstallType {
        FROM_FILE, ALREADY_EXISTS
    }

    protected InstallType installType;

    /**
     * A file which points to a specific assembly ZIP archive. If this parameter
     * is set, then it will install server from archive
     * 
     * @parameter expression="${assemblyArchive}"
     */
    protected File assemblyArchive;

    /**
     * Maven coordinates of a server assembly. This is best listed as a dependency, in which case the version can
     * be omitted.
     * 
     * @parameter
     */
    protected ArtifactItem assemblyArtifact;

    @Override
    protected void init() throws MojoExecutionException, MojoFailureException {
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
            } else {
                if (assemblyArchive != null && assemblyArtifact != null) {
                    throw new MojoExecutionException("Server assembly specified twice: specify only one of maven coordinates in assemblyArtifact or a file in assemblyArchive");
                }
                if (assemblyArtifact != null) {
                    Artifact artifact = getArtifact(assemblyArtifact);
                    assemblyArchive = artifact.getFile();
                    log.info(MessageFormat.format(messages.getString("info.variable.set"), "artifact based assembly archive", assemblyArtifact));
                } else {
                    log.info(MessageFormat.format(messages.getString("info.variable.set"), "non-artifact based assembly archive", assemblyArchive));
                }
                if (assemblyArchive == null) {
                    throw new MojoExecutionException(MessageFormat.format(messages.getString("error.server.assembly.validate"), "artifact based assembly archive", ""));
                }

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

    /**
     * Performs assembly installation unless the install type is pre-existing.
     * 
     * @throws Exception
     */
    protected void installServerAssembly() throws Exception {
        if (installType == InstallType.ALREADY_EXISTS) {
            log.info(MessageFormat.format(messages.getString("info.install.type.preexisting"), ""));
            return;
        }

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

}
