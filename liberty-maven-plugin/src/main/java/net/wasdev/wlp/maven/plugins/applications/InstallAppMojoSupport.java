/**
 * (C) Copyright IBM Corporation 2016, 2017.
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
package net.wasdev.wlp.maven.plugins.applications;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.taskdefs.Copy;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import net.wasdev.wlp.maven.plugins.BasicSupport;

/**
 * Install artifact into Liberty server support.
 */
public class InstallAppMojoSupport extends BasicSupport {

    /**
     * Application directory. 
     */
    @Parameter(property = "appsDirectory", defaultValue = "dropins")
    protected String appsDirectory = null;
    
    /**
     * Strip version. 
     */
    @Parameter(property = "stripVersion", defaultValue = "false")
    protected boolean stripVersion;
    
    /**
     * Loose configuration. 
     */
    @Parameter(property = "looseConfig", defaultValue = "false")
    protected boolean looseConfig = false;
    
    protected void installApp(Artifact artifact) throws Exception {
        if (artifact.getFile() == null) {
            throw new MojoExecutionException(messages.getString("error.install.app.missing"));
        }
        
        File destDir = new File(serverDirectory, appsDirectory);
        log.info(MessageFormat.format(messages.getString("info.install.app"), artifact.getFile().getCanonicalPath()));
        
        Copy copyFile = (Copy) ant.createTask("copy");
        copyFile.setFile(artifact.getFile());
        if (stripVersion) {
            String file = stripVersionFromName(artifact.getFile().getCanonicalPath(), artifact.getVersion());
            file = file.substring(file.lastIndexOf(File.separator) + 1);
            copyFile.setTofile(new File(destDir, file));
        } else {
            copyFile.setTodir(destDir);
        }
        copyFile.execute();
    }
    
    // install project artifact using looseconfig file 
    protected void installLooseConfigApp() throws Exception {
        //Artifact artifact = project.getArtifact();
        File destDir = new File(serverDirectory, appsDirectory);
        File looseConfigFile = new File(destDir, getLooseConfigFileName(project));
        LooseConfigData config = new LooseConfigData();
        
        log.info(MessageFormat.format(messages.getString("info.install.app"), getLooseConfigFileName(project)));
        
        File dir = new File(project.getBasedir() + "/src/main/webapp");
        if (dir.exists()) {
            config.addDir(project.getBasedir() + "/src/main/webapp", "/");
        }
        
        dir = new File(project.getBuild().getOutputDirectory());
        if (dir.exists()) {
            config.addDir(project.getBuild().getOutputDirectory(), "/WEB-INF/classes");
        } else {
            // TODO: need to revise the message
            throw new MojoExecutionException(
                    "Project has not be compiled yet. Please run mvn install or mvn compile to build the project first.");
        }
        
        // retrieves dependent library jar files
        List<Artifact> libraries = getDependentLibraries();
        if (!libraries.isEmpty()) {
            // get a list of dependent-modules from eclipse project deployment
            // assembly if running in eclipse
            List<String> eclipseModules = getEclipseDependentMods();

            // referencing dependent library jar file from mvn repository or set
            // loose config reference to dependent eclipse project output classpath
            if (eclipseModules.isEmpty()) {
                addLibraryFromM2(libraries, config);
            } else {
                for (Artifact library : libraries) {
                    if (library.getFile() != null && eclipseModules.contains(library.getFile().getName())) {
                        MavenProject module = getSiblingModule(library);
                        config.addArchive(module.getBuild().getOutputDirectory(),
                                "/WEB-INF/lib/" + module.getBuild().getFinalName() + "." + module.getPackaging());
                    } else {
                        addLibraryFromM2(library, config);
                    }
                }
            }
        }
        
        config.toXmlFile(looseConfigFile);
    }
    
    // get loose configuration file name for project artifact
    private String getLooseConfigFileName(MavenProject project) {
        String name = project.getBuild().getFinalName() + "." + project.getPackaging();
        if (stripVersion) {
            return stripVersionFromName(name, project.getVersion()) + ".xml";
        } else {
            return name + ".xml";
        }
    }
    
    private void addLibraryFromM2(List<Artifact> libraries, LooseConfigData config) throws Exception {
        for (Artifact library : libraries) {
            addLibraryFromM2(library, config);
        }
    }    
    
    private void addLibraryFromM2(Artifact library, LooseConfigData config) throws Exception {
        // use dependency from local m2 repository
        if (library.getFile() != null) {
            config.addFile(library.getFile().getCanonicalPath(), "/WEB-INF/lib/" + library.getFile().getName());
        } else {
            // TODO: revise the message.
            throw new MojoExecutionException("Dependency can not be found in Maven repositoy, " + library.getId());
        }
    }
    
    private MavenProject getSiblingModule(Artifact artifact) {
        @SuppressWarnings("unchecked")
        List<MavenProject> modules = (List<MavenProject>)project.getParent().getModules();
        for (MavenProject module : modules) {
            if (module.getId().equals(artifact.getId())) {
                return module;
            }
        }
        return null;
    }
      
    private List<String> getEclipseDependentMods() throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        List<String> modules = new ArrayList<String>();
        
        File f = new File(project.getBasedir(), ".settings/org.eclipse.wst.common.component");
        if (f.exists()) {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setIgnoringComments(true);
            builderFactory.setCoalescing(true);
            builderFactory.setIgnoringElementContentWhitespace(true);
            builderFactory.setValidating(false);
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document doc = builder.parse(f);
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = "/wb-module/dependent-module";
            NodeList nodes = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
            
            for (int i = 0; i < nodes.getLength(); i++) {
                modules.add(nodes.item(i).getAttributes().getNamedItem("archiveName").getNodeValue());
            }
        } 
        
        return modules;
    }
    
    private List<Artifact> getDependentLibraries() {
        List<Artifact> libraries = new ArrayList<Artifact>(); 
        
        @SuppressWarnings("unchecked")
        List<Artifact> artifacts = (List<Artifact>) project.getCompileArtifacts();
        for (Artifact artifact : artifacts) {
            if (artifact.getScope().equals("compile") && !artifact.isOptional()) {
                libraries.add(artifact);
            }
        }
        return libraries;
    }
}
