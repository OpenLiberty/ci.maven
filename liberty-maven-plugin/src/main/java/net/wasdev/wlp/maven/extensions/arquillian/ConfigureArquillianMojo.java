package net.wasdev.wlp.maven.extensions.arquillian;

import java.io.File;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.xml.sax.SAXException;

import net.wasdev.wlp.common.arquillian.objects.LibertyManagedObject;
import net.wasdev.wlp.common.arquillian.objects.LibertyProperty;
import net.wasdev.wlp.common.arquillian.objects.LibertyRemoteObject;
import net.wasdev.wlp.common.arquillian.util.ArquillianConfigurationException;
import net.wasdev.wlp.common.arquillian.util.HttpPortUtil;
import net.wasdev.wlp.maven.plugins.BasicSupport;

@Mojo(name = "configure-arquillian", requiresDependencyResolution = ResolutionScope.TEST)
public class ConfigureArquillianMojo extends BasicSupport {

    @Parameter
    private Map<String, String> arquillianProperties;
    public TypeProperty type = TypeProperty.NOTFOUND;

    public enum TypeProperty {
        MANAGED, REMOTE, NOTFOUND;
    }

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
        Set<Artifact> artifacts = project.getArtifacts();
        for (Artifact artifact : artifacts) {
            if (artifact.getArtifactId().equals("arquillian-wlp-remote-8.5")) {
                type = TypeProperty.REMOTE;
            } else if (artifact.getArtifactId().equals("arquillian-wlp-managed-8.5")) {
                type = TypeProperty.MANAGED;
            }
        }
        if (type == TypeProperty.NOTFOUND) {
            throw new MojoExecutionException("Arquillian WLP Managed or Remote Dependency not found");
        }

        if (skipIfArquillianXmlExists && arquillianXml.exists()) {
            log.info(
                    "Skipping configure-arquillian goal because arquillian.xml already exists in \"target/test-classes\".");
            return;
        }

        switch (type) {
        case MANAGED:
            configureArquillianManaged(arquillianXml);
            break;
        case REMOTE:
            configureArquillianRemote(arquillianXml);
            break;
        default:
            throw new MojoExecutionException("This should never happen.");
        }
    }

    private void configureArquillianManaged(File arquillianXml) throws MojoExecutionException {
        try {
            LibertyManagedObject arquillianManaged = new LibertyManagedObject(installDirectory.getCanonicalPath(),
                    serverName, getHttpPort(), LibertyProperty.getArquillianProperties(arquillianProperties,
                            LibertyManagedObject.LibertyManagedProperty.class));
            arquillianManaged.build(arquillianXml);
        } catch (Exception e) {
            throw new MojoExecutionException("Error configuring Arquillian.", e);
        }
    }

    private void configureArquillianRemote(File arquillianXml) throws MojoExecutionException {
        try {
            LibertyRemoteObject arquillianRemote = new LibertyRemoteObject(LibertyProperty
                    .getArquillianProperties(arquillianProperties, LibertyRemoteObject.LibertyRemoteProperty.class));
            arquillianRemote.build(arquillianXml);
        } catch (Exception e) {
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
    private int getHttpPort() throws FileNotFoundException, XPathExpressionException, IOException,
            ParserConfigurationException, SAXException, ArquillianConfigurationException {
        File serverXML = new File(serverDirectory + "/server.xml");
        File bootstrapProperties = new File(serverDirectory + "/bootstrap.properties");
        return HttpPortUtil.getHttpPort(serverXML, bootstrapProperties);
    }

}
