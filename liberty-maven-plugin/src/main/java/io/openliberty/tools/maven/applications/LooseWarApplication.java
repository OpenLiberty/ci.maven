/**
 * (C) Copyright IBM Corporation 2019, 2021.
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
package io.openliberty.tools.maven.applications;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.w3c.dom.DOMException;

import io.openliberty.tools.maven.utils.MavenProjectUtil;
import io.openliberty.tools.common.plugins.config.LooseApplication;
import io.openliberty.tools.common.plugins.config.LooseConfigData;

public class LooseWarApplication extends LooseApplication {
    
    protected final MavenProject project;
    
    protected final Log log;

    public LooseWarApplication(MavenProject project, LooseConfigData config, Log log) {
        super(project.getBuild().getDirectory(), config);
        this.project = project;
        this.log = log;
    }
    
    public static boolean isExploded(MavenProject project) {
    	boolean isExplodedWar = false;
    	
    	// Check if filtering is enabled
    	List<Path> dynamicWebResources = getFilteredWebSourceDirectories(project);
    	
    	if (!dynamicWebResources.isEmpty() || isUsingOverlays(project)) {
    		isExplodedWar = true;
    	}
    	
    	// TODO: Check additional filtering options (properties?)
    	
    	return isExplodedWar;
    }
    
    
    public boolean isExploded() {
    	return isExploded(project);
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

    private Path getWebAppDirectory(MavenProject project) {
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

    public static List<Path> getFilteredWebSourceDirectories(MavenProject project) {

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
    
    public List<Path> getFilteredWebSourceDirectories() {
    	return getFilteredWebSourceDirectories(project);
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
    
    public static boolean isUsingOverlays(MavenProject project) {
    	boolean overlaysEnabled = false;
    	
    	// Get overlay dependencies
    	List<Dependency> overlayDependencies = getWarDependencies(project);
    	
    	// Get overlays configured in the Maven WAR plugin
    	List<Xpp3Dom> overlayConfigurations = getOverlayConfigurations(project);
    	
    	if (!overlayDependencies.isEmpty() || !overlayConfigurations.isEmpty()) {
    		overlaysEnabled = true;
    	}
    	
    	return overlaysEnabled;
    }
    
    /**
     * Get overlay configuration values from the Maven WAR plugin
     * @param proj the Maven project
     * @return ALLOWS DUPS
     * @return a List of war plugin overlay elements
     */
    private static List<Xpp3Dom> getOverlayConfigurations(MavenProject project) {
        List<Xpp3Dom> retVal = new ArrayList<Xpp3Dom>();
        Xpp3Dom dom = project.getGoalConfiguration("org.apache.maven.plugins", "maven-war-plugin", null, null);
        if (dom != null) {
            Xpp3Dom overlays = dom.getChild("overlays");
            if (overlays != null) {
                Xpp3Dom overlayList[] = overlays.getChildren("overlay");
                if (overlayList != null) {
                    for (int i = 0; i < overlayList.length; i++) {
                        retVal.add(overlayList[i]);
                    }
                }
            }
        }
        return retVal;
    }
    
    /**
     * Get overlay dependencies
     * @param proj the Maven project
     * @return ALLOWS DUPS
     * @return a List of war plugin overlay dependencies
     */
    private static List<Dependency> getWarDependencies(MavenProject project) {
    	List<Dependency> overlayDependencies = new ArrayList<Dependency>();
    	
    	List<Dependency> deps = project.getDependencies();
    	for (Dependency dep : deps) {
    		if (dep.getType().equals("war")) {
    			overlayDependencies.add(dep);
    		}
    	}
    	
    	return overlayDependencies;
    }


    /**
     * Get resource configuration values that have "directory" children elements from the Maven WAR plugin
     * @param proj the Maven project
     * @return a List of war plugin resource elements that contain a "directory" child element or empty list if none are found
     */
    public static List<Xpp3Dom> getWebResourcesConfigurations(MavenProject project) {
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
                getLog().warn("Ignoring webResources dir: " + dir.getValue() + ", already have entry for path: " + resolvedDir);
            } else {
                if (onlyUnfiltered && filtering != null && Boolean.parseBoolean(filtering.getValue())) {
                    continue;
                } else {
                    String targetPath = "/";
                    if (target != null) {
                        targetPath = "/" + target.getValue();
                    } 
                    addOutputDir(getDocumentRoot(), resolvedDir.toFile(), targetPath);
                    handled.add(resolvedDir);
                }
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
    	return getWebAppDirectory(project);
    }
}
