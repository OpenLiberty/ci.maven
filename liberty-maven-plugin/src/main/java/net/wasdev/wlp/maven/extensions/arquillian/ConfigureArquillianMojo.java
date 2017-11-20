package net.wasdev.wlp.maven.extensions.arquillian;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.xml.sax.SAXException;

import net.wasdev.wlp.common.arquillian.objects.LibertyManagedObject;
import net.wasdev.wlp.common.arquillian.objects.LibertyManagedProperty;
import net.wasdev.wlp.common.arquillian.util.ArquillianConfigurationException;
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
			configureArquillian(arquillianXml);
		}
	}

	private void configureArquillian(File arquillianXml) throws MojoExecutionException {
		try {
			LibertyManagedObject arquillianManaged = new LibertyManagedObject(installDirectory.getCanonicalPath(), serverName,
				getHttpPort(), LibertyManagedProperty.getArquillianProperties(arquillianProperties));
			arquillianManaged.build(arquillianXml);
		} catch(Exception e) {
			throw new MojoExecutionException("Error configuring Arquillian.", e);
		}
	}

	/**
	 * @return the HTTP port that the managed Liberty server is running on.
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws XPathExpressionException 
	 * @throws FileNotFoundException 
	 * @throws ArquillianConfigurationException 
	 */
	private int getHttpPort() throws FileNotFoundException, XPathExpressionException, IOException, ParserConfigurationException, SAXException, ArquillianConfigurationException {
			File serverXML = new File(serverDirectory + "/server.xml");
			File bootstrapProperties = new File(serverDirectory + "/bootstrap.properties");
			return HttpPortUtil.getHttpPort(serverXML, bootstrapProperties);
	}

}
