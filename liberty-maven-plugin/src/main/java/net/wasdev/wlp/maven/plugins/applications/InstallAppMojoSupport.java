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
     * Loose application.
     */
    @Parameter(property = "looseApplication", defaultValue = "false")
    protected boolean looseApplication;
    
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
    
    // install project artifact using loose application configuration file
    protected void installLooseConfigApp() throws Exception {
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
            throw new MojoExecutionException(MessageFormat.format(messages.getString("error.project.not.compile"),
                    project.getId()));
        }
        
        // retrieves dependent library jar files
        List<Artifact> libraries = getDependentLibraries();
        for (Artifact library : libraries) {
            if (library.getFile() != null) {
                File f = new File(library.getFile().getParentFile(), "classes");
                if (f.exists()) {
                    config.addArchive(f.getCanonicalPath(),
                            "/WEB-INF/lib/" + library.getFile().getCanonicalPath().substring(library.getFile().getCanonicalPath().lastIndexOf(File.separator) + 1));
                } else {
                    addLibraryFromM2(library, config);
                }
            } else {
                addLibraryFromM2(library, config);
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
    
    private void addLibraryFromM2(Artifact library, LooseConfigData config) throws Exception {
        // use dependency from local m2 repository
        if (library.getFile() != null) {
            config.addFile(library.getFile().getCanonicalPath(), "/WEB-INF/lib/" + library.getFile().getName());
        } else {
            throw new MojoExecutionException(MessageFormat.format(messages.getString("error.app.dependency.not.found"),
                    library.getId()));
        }
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
