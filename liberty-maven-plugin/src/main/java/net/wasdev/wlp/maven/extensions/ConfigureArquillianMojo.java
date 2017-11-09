package net.wasdev.wlp.maven.extensions;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import net.wasdev.wlp.common.arquillian.util.HttpPortUtil;
import net.wasdev.wlp.maven.plugins.BasicSupport;

@Mojo(name = "configure-arquillian", requiresDependencyResolution=ResolutionScope.TEST)
public class ConfigureArquillianMojo extends BasicSupport {
	
	@Override
    protected void init() throws MojoExecutionException, MojoFailureException {
		super.init();
	}

	@Override
	public void doExecute() throws MojoExecutionException, MojoFailureException {
		System.out.println("serverName: " + serverName);
		System.out.println("wlpHome: " + installDirectory);
		try {
			File serverXML = new File(serverDirectory + "/server.xml");
			File bootstrapProperties = new File(serverDirectory + "/bootstrap.properties");
			int port = HttpPortUtil.getHttpPort(serverXML, bootstrapProperties);
			System.out.println("port: " + port);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
