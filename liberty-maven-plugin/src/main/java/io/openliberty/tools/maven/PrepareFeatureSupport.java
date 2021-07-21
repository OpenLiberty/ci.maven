/**
 * (C) Copyright IBM Corporation 2021.
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
package io.openliberty.tools.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;

import io.openliberty.tools.common.plugins.util.PluginExecutionException;
import io.openliberty.tools.common.plugins.util.PluginScenarioException;
import io.openliberty.tools.common.plugins.util.PrepareFeatureUtil;

public class PrepareFeatureSupport extends BasicSupport {
	
	
	private PrepareFeatureUtil util;
	
    protected class PrepareFeatureMojoUtil extends PrepareFeatureUtil {
        public PrepareFeatureMojoUtil(String openLibertyVersion)
                throws PluginScenarioException, PluginExecutionException {
            super(installDirectory, openLibertyVersion);
        }

        @Override
        public void debug(String msg) {
            log.debug(msg);
        }
        
        @Override
        public void debug(String msg, Throwable e) {
            log.debug(msg, e);
        }
        
        @Override
        public void debug(Throwable e) {
            log.debug(e);
        }
        
        @Override
        public void warn(String msg) {
            log.warn(msg);
        }

        @Override
        public void info(String msg) {
            log.info(msg);
        }
        
        @Override
        public boolean isDebugEnabled() {
            return log.isDebugEnabled();
        }

        @Override
        public void error(String msg) {
            log.error(msg);
        }

        @Override
        public void error(String msg, Throwable e) {
            log.error(msg, e);
        }
        
        @Override
        public File downloadArtifact(String groupId, String artifactId, String type, String version) throws PluginExecutionException {
            try {
                return getArtifact(groupId, artifactId, type, version).getFile();
            } catch (MojoExecutionException e) {
                throw new PluginExecutionException(e);
            }
        }
    }
    
    private void createNewPrepareFeatureUtil(String openLibertyVersion) 
            throws PluginExecutionException {
        try {
            util = new PrepareFeatureMojoUtil(openLibertyVersion);
        } catch (PluginScenarioException e) {
        	throw new PluginExecutionException(e);
        }
    }
    
    /**
     * Retrieves list of DependencyManagement listed BOM artifacts
     * 
     * @return the list of DependencyManagement listed BOMs
     */
    protected List<String> getDependencyBOMs() {
        List<String> result = new ArrayList<String>();
        org.apache.maven.model.DependencyManagement dependencyManagement = project.getDependencyManagement();
        if (dependencyManagement != null) {
        	List<org.apache.maven.model.Dependency> dependencyManagementArtifacts = dependencyManagement.getDependencies();
            for (org.apache.maven.model.Dependency dependencyArtifact: dependencyManagementArtifacts){
                if (("pom").equals(dependencyArtifact.getType())) {
                	String coordinate = String.format("%s:%s:%s",
                    		dependencyArtifact.getGroupId(), dependencyArtifact.getArtifactId(), dependencyArtifact.getVersion());
                    result.add(coordinate);
                    log.debug("Dependency BOM: " + coordinate);
                }
            }
        }
        return result;
    }

    /**
     * Get a new instance of PrepareFeatureUtil
     * 
     * @param openLibertyVersion The version of the Open Liberty runtime
     * @return instance of PrepareFeatureUtil
     */
    protected PrepareFeatureUtil getPrepareFeatureUtil(String openLibertyVersion)
            throws PluginExecutionException {
    	createNewPrepareFeatureUtil(openLibertyVersion);
        return util;
    }

}
