/**
 * (C) Copyright IBM Corporation 2020, 2021.
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

import io.openliberty.tools.ant.FeatureManagerTask.Feature;
import io.openliberty.tools.common.plugins.util.InstallFeatureUtil;
import io.openliberty.tools.common.plugins.util.PluginExecutionException;
import io.openliberty.tools.common.plugins.util.PluginScenarioException;
import io.openliberty.tools.common.plugins.util.InstallFeatureUtil.ProductProperties;
import io.openliberty.tools.maven.server.types.Features;


public class InstallFeatureSupport extends BasicSupport {

    /**
     * Define a set of features to install in the server and the configuration
     * to be applied for all instances.
     */
    @Parameter
    protected Features features;

    public boolean noFeaturesSection;

    public boolean installFromAnt;

    private InstallFeatureUtil util;
    
    public static final String FEATURES_JSON_ARTIFACT_ID = "features";

    protected class InstallFeatureMojoUtil extends InstallFeatureUtil {
        public InstallFeatureMojoUtil(Set<String> pluginListedEsas, List<ProductProperties> propertiesList, String openLibertyVerion, String containerName, List<String> additionalJsons)
                throws PluginScenarioException, PluginExecutionException {
            super(installDirectory, features.getFrom(), features.getTo(), pluginListedEsas, propertiesList, openLibertyVerion, containerName, additionalJsons);
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

    protected Set<String> getPluginListedFeatures(boolean findEsaFiles) {
        Set<String> result = new HashSet<String>();
        for (Feature feature : features.getFeatures()) {
            if ((findEsaFiles && feature.getFeature().endsWith(".esa"))
                    || (!findEsaFiles && !feature.getFeature().endsWith(".esa"))) {
                result.add(feature.getFeature());
                log.debug("Plugin listed " + (findEsaFiles ? "ESA" : "feature") + ": " + feature.getFeature());
            }
        }
        return result;
    }
    
    protected Set<String> getDependencyFeatures() {
        Set<String> result = new HashSet<String>();
        List<org.apache.maven.model.Dependency> dependencyArtifacts = project.getDependencies();
        for (org.apache.maven.model.Dependency dependencyArtifact: dependencyArtifacts){
            if (("esa").equals(dependencyArtifact.getType())) {
                result.add(dependencyArtifact.getArtifactId());
                log.debug("Dependency feature: " + dependencyArtifact.getArtifactId());
            }
        }
        return result;
    }
    
    protected List<String> getAdditionalJsonList() {
        //note in this method we use the BOM coordinate but replace the BOM artifactId
        //with the features JSON artifactId which by prepare-feature convention is "features"
        List<String> result = new ArrayList<String>();
        org.apache.maven.model.DependencyManagement dependencyManagement = project.getDependencyManagement();
        if(dependencyManagement == null) {
        	log.debug("Feature-bom is not provided by the user");
        	return null;
        }
        List<org.apache.maven.model.Dependency> dependencyManagementArtifacts = dependencyManagement.getDependencies();
        for (org.apache.maven.model.Dependency dependencyArtifact: dependencyManagementArtifacts){
            if (("pom").equals(dependencyArtifact.getType())) {
                String coordinate = String.format("%s:%s:%s",
                        dependencyArtifact.getGroupId(), FEATURES_JSON_ARTIFACT_ID, dependencyArtifact.getVersion());
                result.add(coordinate);
                log.info("Additional user feature json coordinate: " + coordinate);
            }
        }
        return result;
    }

    protected boolean initialize() throws MojoExecutionException {
        if (skip) {
            getLog().info("\nSkipping install-feature goal.\n");
            return false;
        }

        if (features == null) {
            // For liberty-assembly integration:
            // When using installUtility, if no features section was specified, 
            // then don't install features because it requires license acceptance
            noFeaturesSection = true;
            
            // initialize features section for all scenarios except for the above
            features = new Features();
        }

        checkServerHomeExists();

        return true;
    }

    /**
     * Get the current specified Liberty features.
     *
     * @param String containerName The container name if the features should be installed in a container. Otherwise null.
     * @return Set of Strings containing the specified Liberty features
     */
    protected Set<String> getSpecifiedFeatures(String containerName) throws PluginExecutionException {
        Set<String> pluginListedFeatures = getPluginListedFeatures(false);

        if (util == null) {
            Set<String> pluginListedEsas = getPluginListedFeatures(true);
            List<ProductProperties> propertiesList = null;
            String openLibertyVersion = null;
            if (containerName == null) {
                propertiesList = InstallFeatureUtil.loadProperties(installDirectory);
                openLibertyVersion = InstallFeatureUtil.getOpenLibertyVersion(propertiesList);
            }
            List<String> additionalJsons = getAdditionalJsonList();
            createNewInstallFeatureUtil(pluginListedEsas, propertiesList, openLibertyVersion, containerName, additionalJsons);
        }

        if (util == null && noFeaturesSection) {
            //No features were installed because acceptLicense parameter was not configured
            return new HashSet<String>();
        }
        else if (util == null && !noFeaturesSection) {
            Set<String> featuresToInstall = new HashSet<String>();
            for (Feature feature : features.getFeatures()) {
                featuresToInstall.add(feature.toString());
            }
            return featuresToInstall;
        }
        else {
            Set<String> dependencyFeatures = getDependencyFeatures();
            Set<String> serverFeatures = serverDirectory.exists() ? util.getServerFeatures(serverDirectory, getLibertyDirectoryPropertyFiles()) : null;
            return InstallFeatureUtil.combineToSet(pluginListedFeatures, dependencyFeatures, serverFeatures);
            
        }
    }

    private void createNewInstallFeatureUtil(Set<String> pluginListedEsas, List<ProductProperties> propertiesList, String openLibertyVerion, String containerName, List<String> additionalJsons) 
            throws PluginExecutionException {
        try {
            util = new InstallFeatureMojoUtil(pluginListedEsas, propertiesList, openLibertyVerion, containerName, additionalJsons);
        } catch (PluginScenarioException e) {
            log.debug(e.getMessage());
            if (noFeaturesSection) {
                log.debug("Skipping feature installation with installUtility because the "
                        + "features configuration element with an acceptLicense parameter "
                        + "was not specified for the install-feature goal.");
            } else if(additionalJsons != null && !additionalJsons.isEmpty()) {
            	log.debug("Skipping feature installation with installUtility because it is not supported for user feature");
        	}else {
                installFromAnt = true;
                log.debug("Installing features from installUtility.");
            }
        }
    }

    /**
     * Get a new instance of InstallFeatureUtil
     * 
     * @param pluginListedEsas The list of ESAs specified in the plugin configuration, or null if not specified
     * @param propertiesList The list of product properties installed with the Open Liberty runtime
     * @param openLibertyVersion The version of the Open Liberty runtime
     * @param containerName The container name if the features should be installed in a container. Otherwise null.
     * @return instance of InstallFeatureUtil
     */
    protected InstallFeatureUtil getInstallFeatureUtil(Set<String> pluginListedEsas, List<ProductProperties> propertiesList, String openLibertyVerion, String containerName, List<String> additionalJsons)
            throws PluginExecutionException {
        createNewInstallFeatureUtil(pluginListedEsas, propertiesList, openLibertyVerion, containerName, additionalJsons);
        return util;
    }
    
}
