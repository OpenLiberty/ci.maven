package net.wasdev.wlp.maven.plugins.server;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import net.wasdev.wlp.maven.plugins.BasicSupport;

/**
 * Install a liberty server
 * 
 * @goal install-server
 * 
 * 
 */
public class InstallServerMojo extends BasicSupport {

    protected void doExecute() throws MojoExecutionException,
                    MojoFailureException {

        try {
            installServerAssembly();
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage());
        }

    }

}
