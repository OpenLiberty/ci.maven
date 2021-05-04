package io.openliberty.tools.maven.applications;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.w3c.dom.DOMException;

import io.openliberty.tools.common.plugins.config.LooseApplication;
import io.openliberty.tools.common.plugins.config.LooseConfigData;
import io.openliberty.tools.maven.utils.MavenProjectUtil;

public class LooseWarApplication extends LooseApplication {
    
    protected final MavenProject project;
    protected final Log log;
    
    public LooseWarApplication(MavenProject project, LooseConfigData config, Log log) {
        super(project.getBuild().getDirectory(), config);
        this.project = project;
        this.log = log;
    }
    
    public void addSourceDir() throws Exception {
        Path warSourceDir = getWarSourceDirectory();
        config.addDir(warSourceDir.toFile(), "/");
    }
    
    public Path getWarSourceDirectory() {
        return getWarSourceDirectory(project);
    }

    private static Path getWarSourceDirectory(MavenProject project) {
        Path baseDir = Paths.get(project.getBasedir().getAbsolutePath());
        String warSourceDir = MavenProjectUtil.getPluginConfiguration(project, "org.apache.maven.plugins", "maven-war-plugin", "warSourceDirectory");
        if (warSourceDir == null) {
            warSourceDir = "src/main/webapp";
        }  
        // Use java.nio Paths to fix issue with absolute paths on Windows
        return baseDir.resolve(warSourceDir);
    }

    public Path getWebAppDirectory(MavenProject project) {
        Xpp3Dom dom = project.getGoalConfiguration("org.apache.maven.plugins", "maven-war-plugin", null, null);
        String webAppDirStr = null;
        if (dom != null) {
            Xpp3Dom webAppDirConfig = dom.getChild("webappDirectory");
            if (webAppDirConfig != null) {
                webAppDirStr = webAppDirConfig.getValue();
            }
        }

        if (webAppDirStr != null) {
            return Paths.get(webAppDirStr);
        } else {
            // Match plugin default (we could get the default programmatically via webAppDirConfig.getAttribute("default-value") but don't
            return Paths.get(project.getBuild().getDirectory(), project.getBuild().getFinalName());
        }
    }

    public static List<Path> getDynamicWebSourceDirectories(MavenProject project) {

        List<Path> retVal = new ArrayList<Path>();
        
        Path baseDirPath = Paths.get(project.getBasedir().getAbsolutePath());
        
        for (Xpp3Dom resource : getWebResourcesConfigurations(project)) {
            Xpp3Dom dir = resource.getChild("directory");
            Xpp3Dom filtering = resource.getChild("filtering");
            if (dir != null && filtering != null) {
                boolean filtered = Boolean.parseBoolean(filtering.getValue());
                if (filtered) {
                    retVal.add(baseDirPath.resolve(dir.getValue()));
                }
            }
        }
        
        // Now add warSourceDir
        if (isFilteringDeploymentDescriptors(project)) {
            retVal.add(getWarSourceDirectory(project));
        }
        
        return retVal;
    }
    

    private static boolean isFilteringDeploymentDescriptors(MavenProject project) {
        Boolean retVal = false;
        Xpp3Dom dom = project.getGoalConfiguration("org.apache.maven.plugins", "maven-war-plugin", null, null);
        if (dom != null) {
            Xpp3Dom fdd = dom.getChild("filteringDeploymentDescriptors");
            if (fdd != null) {
                retVal = Boolean.parseBoolean(fdd.getValue());
            }
        }
        return retVal;
    }
    
    public boolean isFilteringDeploymentDescriptors() {
        return isFilteringDeploymentDescriptors(project);
    }
    

    /**
     * Get directory and targetPath configuration values from the Maven WAR plugin
     * @param proj the Maven project
     * @return ALLOWS DUPS
     * @return a Map of source and target directories corresponding to the configuration keys
     * or null if the war plugin or its webResources or resource elements are not used. 
     * The map will be empty if the directory element is not used. The value field of the 
     * map is null when the targetPath element is not used.
     */
    private static List<Xpp3Dom> getWebResourcesConfigurations(MavenProject project) {
        List<Xpp3Dom> retVal = new ArrayList<Xpp3Dom>();
        Xpp3Dom dom = project.getGoalConfiguration("org.apache.maven.plugins", "maven-war-plugin", null, null);
        if (dom != null) {
            Xpp3Dom web = dom.getChild("webResources");
            if (web != null) {
                Xpp3Dom resources[] = web.getChildren("resource");
                if (resources != null) {
                    for (int i = 0; i < resources.length; i++) {
                        Xpp3Dom dir = resources[i].getChild("directory");
                        // put dir in List
                        if (dir != null) {
                            retVal.add(resources[i]);
                        }
                    }
                }
            }
        }
        return retVal;
    }

    private void addWebResourcesConfigurationPaths(boolean onlyUnfiltered) throws DOMException, IOException {
        Set<Path> handled = new HashSet<Path>();
        
        Path baseDirPath = Paths.get(project.getBasedir().getAbsolutePath());
        
        for (Xpp3Dom resource : getWebResourcesConfigurations(project)) {
            Xpp3Dom dir = resource.getChild("directory");
            Xpp3Dom target = resource.getChild("targetPath");
            Xpp3Dom filtering = resource.getChild("filtering");
            Path resolvedDir = baseDirPath.resolve(dir.getValue());
            if (handled.contains(resolvedDir)) {
                log.warn("Ignoring webResources dir: " + dir.getValue() + ", already have entry for path: " + resolvedDir);
            } else {
                if (onlyUnfiltered) {
                    if (filtering != null && Boolean.parseBoolean(filtering.getValue())) {
                        continue;
                    } else {
                        String targetPath = "/";
                        if (target != null) {
                            targetPath = "/" + target.getValue();
                        } 
                        addOutputDir(getDocumentRoot(), resolvedDir.toFile(), targetPath);
                        handled.add(resolvedDir);
                    }
                } else {
                    String targetPath = "/";
                    if (target != null) {
                        targetPath = "/" + target.getValue();
                    } 
                    addOutputDir(getDocumentRoot(), resolvedDir.toFile(), targetPath);
                    handled.add(resolvedDir);
                }
    /**
     *              String targetPath = webResources.get(directory)==null ? "/" : "/"+webResources.get(directory);
                    looseWar.addOutputDir(looseWar.getDocumentRoot(), directory.toFile(), targetPath);
                    */
            }
        }
    }

    /*
     * @return
     * @throws IOException 
     * @throws DOMException 
     */

    public void addAllWebResourcesConfigurationPaths() throws DOMException, IOException {
        addWebResourcesConfigurationPaths(false);
    }
    
    public void addNonFilteredWebResourcesConfigurationPaths() throws DOMException, IOException {
        addWebResourcesConfigurationPaths(true);
    }
        
    public Path getWebAppDirectory() {
        Xpp3Dom dom = project.getGoalConfiguration("org.apache.maven.plugins", "maven-war-plugin", null, null);
        String webAppDirStr = null;
        if (dom != null) {
            Xpp3Dom webAppDirConfig = dom.getChild("webappDirectory");
            if (webAppDirConfig != null) {
                webAppDirStr = webAppDirConfig.getValue();
            }
        }

        if (webAppDirStr != null) {
            return Paths.get(webAppDirStr);
        } else {
            // Match plugin default (we could get the default programmatically via webAppDirConfig.getAttribute("default-value") but don't
            return Paths.get(project.getBuild().getDirectory(), project.getBuild().getFinalName());
        }
    }

    
}
