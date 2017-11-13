package net.wasdev.wlp.maven.extensions.arquillian;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import net.wasdev.wlp.common.arquillian.objects.WLPManagedObject;
import net.wasdev.wlp.common.arquillian.util.ArquillianProperty;
import net.wasdev.wlp.common.arquillian.util.HttpPortUtil;
import net.wasdev.wlp.maven.plugins.BasicSupport;

@Mojo(name = "configure-arquillian", requiresDependencyResolution = ResolutionScope.TEST)
public class ConfigureArquillianMojo extends BasicSupport {

	@Parameter
	private Map<String, String> arquillianProperties;

	/*
	 * Skips this goal if arquillian.xml already exists in the target (Maven) or
	 * build (Gradle) folders
	 */
	@Parameter(property = "skipIfArquillianXmlExists", defaultValue = "false")
	protected boolean skipIfArquillianXmlExists = false;

	@Override
	protected void init() throws MojoExecutionException, MojoFailureException {
		super.init();
	}

	@Override
	public void doExecute() throws MojoExecutionException, MojoFailureException {
		File arquillianXml = new File(project.getBuild().getDirectory(), "test-classes/arquillian.xml");
		if (skipIfArquillianXmlExists && arquillianXml.exists()) {
			log.info(
					"Skipping configure-arquillian goal because arquillian.xml already exists in \"target/test-classes\".");
		} else {
			try {
				configureArquillian(arquillianXml);
			} catch (IOException e) {
				e.printStackTrace();
				throw new MojoFailureException("IO Exception while configuring arquillian.xml. Please see the stack trace.");
			}
		}
	}

	private void configureArquillian(File arquillianXml) throws MojoExecutionException, MojoFailureException, IOException {
		WLPManagedObject arquillianManaged = new WLPManagedObject(installDirectory.getCanonicalPath(), serverName,
				getHttpPort(), getArquillianProperties());
		arquillianManaged.build(arquillianXml);
	}

	/**
	 * @return the HTTP port that the managed Liberty server is running on.
	 */
	private int getHttpPort() {
		try {
			File serverXML = new File(serverDirectory + "/server.xml");
			File bootstrapProperties = new File(serverDirectory + "/bootstrap.properties");
			return HttpPortUtil.getHttpPort(serverXML, bootstrapProperties);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return HttpPortUtil.ERROR_PORT;
	}

	private Map<ArquillianProperty, String> getArquillianProperties() throws MojoFailureException {
		Map<ArquillianProperty, String> props = new HashMap<ArquillianProperty, String>();
		for (Entry<String, String> entry : arquillianProperties.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			ArquillianProperty p = getArquillianProperty(key);
			props.put(p, value);
			System.out.println("--- Added ArquillianProperty with key " + key + " and value " + value);
		}
		return props;
	}

	/**
	 * Check that the given key exists in ArquillianProperties
	 * 
	 * @param key
	 * @return true if so, fail the build otherwise
	 * @throws MojoFailureException
	 */
	private ArquillianProperty getArquillianProperty(String key) throws MojoFailureException {
		try {
			return ArquillianProperty.valueOf(key);
		} catch (IllegalArgumentException e) {
			throw new MojoFailureException(
					"Property \"" + key + "\" in arquillianProperties does not exist. You probably have a typo.");
		}
	}

}
