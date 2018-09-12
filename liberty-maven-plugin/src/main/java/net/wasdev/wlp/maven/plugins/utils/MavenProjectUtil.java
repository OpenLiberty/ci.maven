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
package net.wasdev.wlp.maven.plugins.utils;

import java.util.Iterator;
import java.util.List;

import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import net.wasdev.wlp.common.plugins.util.PluginScenarioException;

public class MavenProjectUtil {
    
    /**
     * Get a String configuration element from a goal from a plugin
     * @param project
     * @param pluginKey
     * @param goal
     * @param configName
     * @return the value of the configuration parameter
     * @throws PluginScenarioException
     */
    public static String getPluginConfigurationString(MavenProject project, String pluginKey, String goal, String configName) throws PluginScenarioException {
        PluginExecution execution = getPluginExecution(project, pluginKey, goal);
        
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
    public static PluginExecution getPluginExecution(Plugin plugin, String goal) throws PluginScenarioException {
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
    public static PluginExecution getPluginExecution(MavenProject project, String pluginKey, String goal) throws PluginScenarioException {
        Plugin plugin = project.getPlugin(pluginKey);
        return getPluginExecution(plugin, goal);
    }

}