/**
 * (C) Copyright IBM Corporation 2017, 2021.
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

package io.openliberty.tools.maven.extensions.arquillian;

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

import io.openliberty.tools.maven.BasicSupport;
import io.openliberty.tools.common.arquillian.objects.LibertyManagedObject;
import io.openliberty.tools.common.arquillian.objects.LibertyProperty;
import io.openliberty.tools.common.arquillian.objects.LibertyRemoteObject;
import io.openliberty.tools.common.arquillian.util.ArquillianConfigurationException;
import io.openliberty.tools.common.arquillian.util.ArtifactCoordinates;
import io.openliberty.tools.common.arquillian.util.Constants;
import io.openliberty.tools.common.arquillian.util.HttpPortUtil;

@Mojo(name = "configure-arquillian", requiresDependencyResolution = ResolutionScope.TEST)
public class ConfigureArquillianMojo extends BasicSupport {

    @Parameter
    private Map<String, String> arquillianProperties;
    
    private TypeProperty type = TypeProperty.NOTFOUND;
    private enum TypeProperty {
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
        
        outerloop:
        for (Artifact artifact : artifacts) {
            for(ArtifactCoordinates coors : Constants.ARQUILLIAN_REMOTE_DEPENDENCY) {
                String groupId = artifact.getGroupId();
                String artifactId = artifact.getArtifactId();
                if (groupId.equals(coors.getGroupId()) && artifactId.equals(coors.getArtifactId())) {
                    type = TypeProperty.REMOTE;
                    log.info("Automatically detected the Arquillian Liberty Remote container at the following coordinates: " + groupId + ":" + artifactId + ".");
                    break outerloop;
                }
            }
            for(ArtifactCoordinates coors : Constants.ARQUILLIAN_MANAGED_DEPENDENCY) {
                String groupId = artifact.getGroupId();
                String artifactId = artifact.getArtifactId();
                if (groupId.equals(coors.getGroupId()) && artifactId.equals(coors.getArtifactId())) {
                    type = TypeProperty.MANAGED;
                    log.info("Automatically detected the Arquillian Liberty Managed container at the following coordinates: " + groupId + ":" + artifactId + ".");
                    break outerloop;
                }
            }
        }
        
        if (type == TypeProperty.NOTFOUND) {
            log.warn("Arquillian Liberty Managed and Remote dependencies were not found. Defaulting to use the Liberty Managed container.");
            type = TypeProperty.MANAGED;
        }

        if (skipIfArquillianXmlExists && arquillianXml.exists()) {
            log.info("Skipping configure-arquillian goal because arquillian.xml already exists in \"target/test-classes\".");
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
            String userDir = userDirectorySpecified ? userDirectory.getCanonicalPath() : null;
            LibertyManagedObject arquillianManaged = new LibertyManagedObject(installDirectory.getCanonicalPath(),
                    serverName, userDir, getHttpPort(), LibertyProperty.getArquillianProperties(arquillianProperties,
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
        File configVariableXML = new File(serverDirectory + "/configDropins/overrides/liberty-plugin-variable-config.xml");
        File bootstrapProperties = new File(serverDirectory + "/bootstrap.properties");
        return HttpPortUtil.getHttpPort(serverXML, bootstrapProperties, configVariableXML);
    }

}
