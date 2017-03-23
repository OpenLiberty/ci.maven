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
     * Strip version. 
     */
    @Parameter(property = "stripVersion", defaultValue = "false")
    protected boolean stripVersion;
    
    /**
     * Loose application. 
     */
    @Parameter(property = "looseApplication", defaultValue = "false")
    protected boolean looseApplication;
    
    protected void installApp(Artifact artifact) throws Exception {
        if (artifact.getFile() == null) {
            throw new MojoExecutionException(messages.getString("error.install.app.missing"));
        }
        
        setAppsDirectory();
        
        if (!appsDirectory.equalsIgnoreCase("dropins") && !isApplicationConfigured()) {
            addAppConfiguration(artifact.getArtifactId());
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
    
    // install project artifact using loose application configuration file 
    protected void installLooseConfigApp() throws Exception {
    
        setAppsDirectory(); 
    
        if (!appsDirectory.equalsIgnoreCase("dropins") && !isApplicationConfigured()) {
            addAppConfiguration(project.getBuild().getFinalName());
        }
    
        File destDir = new File(serverDirectory, appsDirectory);
        File looseConfigFile = new File(destDir, getLooseConfigFileName(project));
        LooseConfigData config = new LooseConfigData();
        
        log.info(MessageFormat.format(messages.getString("info.install.app"), getLooseConfigFileName(project)));
        
        File dir = new File(project.getBasedir() + "/src/main/webapp");
        if (dir.exists()) {
            config.addDir(dir.getCanonicalPath(), "/");
        }
        
        dir = new File(project.getBuild().getOutputDirectory());
        if (dir.exists()) {
            config.addDir(dir.getCanonicalPath(), "/WEB-INF/classes");
        } else {
            throw new MojoExecutionException(MessageFormat.format(messages.getString("error.project.not.compile"),
                    project.getId()));
        }
        
        // retrieves dependent library jar files
        List<Artifact> libraries = getDependentLibraries();
        if (!libraries.isEmpty()) {
            // get a list of dependent-modules from eclipse project deployment
            // assembly if running in eclipse
            List<String> eclipseModules = getEclipseDependentMods();

            // referencing dependent library jar file from mvn repository or set
            // loose application configuration reference to dependent eclipse project output classpath
            if (eclipseModules.isEmpty()) {
                addLibraries(libraries, config);
            } else {
                for (Artifact library : libraries) {
                    if (library.getFile() == null || !eclipseModules.contains(getFileName(library.getFile()))) {
                        addLibraryFromM2(library, config);
                    } else {
                        File classDir = new File(project.getBasedir() + "/../" + library.getArtifactId() + "/target/classes");
                        log.debug("sibling module target class directory pathname: " + classDir.getCanonicalPath());
                        
                        if (classDir.exists()) {
                            config.addArchive(classDir.getCanonicalPath(), "/WEB-INF/lib/" + getFileName(library.getFile()));
                        } else {
                            addLibraryFromM2(library, config);
                        }
                    }
                }
            }
        }
        
        config.toXmlFile(looseConfigFile);
    }
    
    // get loose application configuration file name for project artifact
    private String getLooseConfigFileName(MavenProject project) {
        String name = project.getBuild().getFinalName() + "." + project.getPackaging();
        if (project.getPackaging().equals("liberty-assembly")) {
            name = project.getBuild().getFinalName() + ".war";
        }
        if (stripVersion) {
            return stripVersionFromName(name, project.getVersion()) + ".xml";
        } else {
            return name + ".xml";
        }
    }
    
    // add dependent library loose config element from sibling project or from m2 repository
    private void addLibraries(List<Artifact> libraries, LooseConfigData config) throws Exception {
        for (Artifact library : libraries) {
            if (library.getFile() != null) {
                File f = new File(library.getFile().getParentFile(), "classes");
                if (f.exists()) {
                    config.addArchive(f.getCanonicalPath(),
                            "/WEB-INF/lib/" + getFileName(library.getFile()));
                } else {
                    addLibraryFromM2(library, config);
                }
            } else {
                addLibraryFromM2(library, config);
            }
        }
    } 
    
    private String getFileName(File f) throws IOException {
        String name = f.getCanonicalPath().substring(f.getCanonicalPath().lastIndexOf(File.separator) + 1);
        return name;
    }
    
    private void addLibraryFromM2(Artifact library, LooseConfigData config) throws Exception {
        // use dependency from local m2 repository
        if (library.getFile() != null) {
            config.addFile(library.getFile().getCanonicalPath(), "/WEB-INF/lib/" + library.getFile().getName());
        } else {
            throw new MojoExecutionException(MessageFormat.format(messages.getString("error.app.dependency.not.found"),
                    library.getId()));
        }
    }
    
    private String getSiblingModule(Artifact artifact) {
        if (project.getParent() == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        List<String> modules = (List<String>)project.getParent().getModules();
        for (String module : modules) {
            if (module.equals(artifact.getArtifactId())) {
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
            String expression = "/project-modules/wb-module/dependent-module";
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
