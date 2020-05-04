/**
 * (C) Copyright IBM Corporation 2018.
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
package io.openliberty.tools.maven.utils;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import io.openliberty.tools.common.plugins.util.PluginScenarioException;

public class MavenProjectUtil {
    
    /**
     * Get a configuration value from a plugin
     * @param proj the Maven project
     * @param pluginGroupId the plugin group id
     * @param pluginArtifactId the plugin artifact id
     * @param key the configuration key to get from
     * @return the value corresponding to the configuration key
     */
    public static String getPluginConfiguration(MavenProject proj, String pluginGroupId, String pluginArtifactId, String key) {
        return getPluginExecutionConfiguration(proj, pluginGroupId, pluginArtifactId, null, key);
    }

    /**
     * Get a configuration value from an execution of a plugin
     * @param proj the Maven project
     * @param pluginGroupId the plugin group id
     * @param pluginArtifactId the plugin artifact id
     * @param executionId the plugin execution id
     * @param key the configuration key to get from
     * @return the value corresponding to the configuration key
     */
    public static String getPluginExecutionConfiguration(MavenProject proj, String pluginGroupId, String pluginArtifactId, String executionId, String key) {
        Xpp3Dom dom = proj.getGoalConfiguration(pluginGroupId, pluginArtifactId, executionId, null);
        if (dom != null) {
            Xpp3Dom val = dom.getChild(key);
            if (val != null) {
                return val.getValue();
            }
        }
        return null;
    }

    /**
     * Get directory and targetPath configuration values from the Maven WAR plugin
     * @param proj the Maven project
     * @return a Map of source and target directories corresponding to the configuration keys
     * or null if the war plugin or its webResources or resource elements are not used. 
     * The map will be empty if the directory element is not used. The value field of the 
     * map is null when the targetPath element is not used.
     */
    public static Map<String,String> getWebResourcesConfiguration(MavenProject proj) {
        Xpp3Dom dom = proj.getGoalConfiguration("org.apache.maven.plugins", "maven-war-plugin", null, null);
        if (dom != null) {
            Xpp3Dom web = dom.getChild("webResources");
            if (web != null) {
                Xpp3Dom resources[] = web.getChildren("resource");
                if (resources != null) {
                    Map<String, String> result = new HashMap<String, String>();
                    for (int i = 0; i < resources.length; i++) {
                        Xpp3Dom dir = resources[i].getChild("directory");
                        if (dir != null) {
                            Xpp3Dom target = resources[i].getChild("targetPath");
                            if (target != null) {
                                result.put(dir.getValue(), target.getValue());
                            } else {
                                result.put(dir.getValue(), null);
                            }
                        }
                    }
                    return result;
                }
            }
        }
        return null;
    }
    
    /**
     * Get a configuration value from a goal from a plugin
     * @param project
     * @param pluginKey
     * @param goal
     * @param configName
     * @return the value of the configuration parameter
     * @throws PluginScenarioException
     */
    public static String getPluginGoalConfigurationString(MavenProject project, String pluginKey, String goal, String configName) throws PluginScenarioException {
        PluginExecution execution = getPluginGoalExecution(project, pluginKey, goal);
        
        final Xpp3Dom config = (Xpp3Dom)execution.getConfiguration();
        if(config != null) {
            Xpp3Dom configElement = config.getChild(configName);
            if(configElement != null) {
                String value = configElement.getValue().trim();
                return value;
            }
        }
        throw new PluginScenarioException("Could not find configuration string " + configName + " for goal " + goal + " on plugin " + pluginKey);
    }
    
    /**
     * Get an execution of a plugin
     * @param plugin
     * @param goal
     * @return the execution object
     * @throws PluginScenarioException
     */
    public static PluginExecution getPluginGoalExecution(Plugin plugin, String goal) throws PluginScenarioException {
        List<PluginExecution> executions = plugin.getExecutions();
        
        for(Iterator<PluginExecution> iterator = executions.iterator(); iterator.hasNext();) {
            PluginExecution execution = (PluginExecution) iterator.next();
            if(execution.getGoals().contains(goal)) {
                return execution;
            }
        }
        throw new PluginScenarioException("Could not find goal " + goal + " on plugin " + plugin.getKey());
    }
    
    /**
     * Get an execution of a plugin
     * @param project
     * @param pluginKey
     * @param goal
     * @return the execution object
     * @throws PluginScenarioException
     */
    public static PluginExecution getPluginGoalExecution(MavenProject project, String pluginKey, String goal) throws PluginScenarioException {
        Plugin plugin = project.getPlugin(pluginKey);
        return getPluginGoalExecution(plugin, goal);
    }
    
    /**
     * Get manifest file from plugin configuration
     * @param proj
     * @param pluginArtifactId
     * @return the manifest file
     */
    public static File getManifestFile(MavenProject proj, String pluginArtifactId) {
        Xpp3Dom dom = proj.getGoalConfiguration("org.apache.maven.plugins", pluginArtifactId, null, null);
        if (dom != null) {
            Xpp3Dom archive = dom.getChild("archive");
            if (archive != null) {
                Xpp3Dom val = archive.getChild("manifestFile");
                if (val != null) {
                    return new File(proj.getBasedir().getAbsolutePath() + "/" + val.getValue());
                }
            }
        }
        return null;
    }

     /**
     * Get major plugin version
     * @param project
     * @param pluginKey
     * @return the major plugin version
     * @throws PluginScenarioException
     */
    public static int getMajorPluginVersion(MavenProject project, String pluginKey) throws PluginScenarioException {
        Plugin plugin = project.getPlugin(pluginKey);
        return Character.getNumericValue(plugin.getVersion().charAt(0));
    }

}