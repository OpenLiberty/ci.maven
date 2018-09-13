/**
 * (C) Copyright IBM Corporation 2015, 2018.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package net.wasdev.wlp.maven.plugins.server;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import net.wasdev.wlp.ant.FeatureManagerTask.Feature;
import net.wasdev.wlp.ant.InstallFeatureTask;
import net.wasdev.wlp.common.plugins.util.InstallFeatureUtil;
import net.wasdev.wlp.common.plugins.util.PluginExecutionException;
import net.wasdev.wlp.common.plugins.util.PluginScenarioException;
import net.wasdev.wlp.maven.plugins.BasicSupport;
import net.wasdev.wlp.maven.plugins.server.types.Features;

/**
 * This mojo installs a feature packaged as a Subsystem Archive (esa) to the
 * runtime.
 */
@Mojo(name = "install-feature")
public class InstallFeatureMojo extends BasicSupport {
    
    /**
     * Define a set of features to install in the server and the configuration
     * to be applied for all instances.
     */
    @Parameter
    private Features features;

    private class InstallFeatureMojoUtil extends InstallFeatureUtil {
        public InstallFeatureMojoUtil(Set<String> pluginListedEsas)  throws PluginScenarioException, PluginExecutionException {
            super(installDirectory, features.getFrom(), features.getTo(), pluginListedEsas);
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
        public File downloadArtifact(String groupId, String artifactId, String type, String version) throws PluginExecutionException {
            try {
                return getArtifact(groupId, artifactId, type, version).getFile();
            } catch (MojoExecutionException e) {
                throw new PluginExecutionException(e);
            }
        }
    }
        
    /*
     * (non-Javadoc)
     * @see org.codehaus.mojo.pluginsupport.MojoSupport#doExecute()
     */
    @Override
    protected void doExecute() throws Exception {
        if (skip) {
            return;
        }

        // for liberty-assembly integration
        if (features == null) {
            features = new Features();
        }

        checkServerHomeExists();
        installFeatures();
    }

    private void installFeatures() throws PluginExecutionException {       
        Set<String> pluginListedFeatures = getPluginListedFeatures(false);
        Set<String> pluginListedEsas = getPluginListedFeatures(true);
        
        InstallFeatureUtil util;
        try {
            util = new InstallFeatureMojoUtil(pluginListedEsas);
        } catch (PluginScenarioException e) {
            log.debug(e.getMessage());
            log.debug("Installing features from installUtility.");
            installFeaturesFromAnt(features.getFeatures());
            return;
        }

        Set<String> dependencyFeatures = getDependencyFeatures();
        Set<String> serverFeatures = serverDirectory.exists() ? util.getServerFeatures(serverDirectory) : null;

        Set<String> featuresToInstall = InstallFeatureUtil.combineToSet(pluginListedFeatures, dependencyFeatures, serverFeatures);

        util.installFeatures(features.isAcceptLicense(), new ArrayList<String>(featuresToInstall));
    }
    
    private Set<String> getPluginListedFeatures(boolean findEsaFiles) {
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
    
    private Set<String> getDependencyFeatures() {
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

    private void installFeaturesFromAnt(List<Feature> installFeatures) {
        InstallFeatureTask installFeatureTask = (InstallFeatureTask) ant
                .createTask("antlib:net/wasdev/wlp/ant:install-feature");

        if (installFeatureTask == null) {
            throw new IllegalStateException(MessageFormat.format(messages.getString("error.dependencies.not.found"), "install-feature"));
        }

        installFeatureTask.setInstallDir(installDirectory);
        installFeatureTask.setServerName(serverName);
        installFeatureTask.setUserDir(userDirectory);
        installFeatureTask.setOutputDir(outputDirectory);
        installFeatureTask.setAcceptLicense(features.isAcceptLicense());
        installFeatureTask.setTo(features.getTo());
        // whenFileExist is deprecated, but keep it to ensure backward compatibility
        installFeatureTask.setWhenFileExists(features.getWhenFileExists());
        installFeatureTask.setFeatures(installFeatures);
        installFeatureTask.setFrom(features.getFrom());
        installFeatureTask.execute();
    }

}
