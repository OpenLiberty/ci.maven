/**
 * (C) Copyright IBM Corporation 2015, 2021.
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
package io.openliberty.tools.maven.server;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.openliberty.tools.ant.InstallFeatureTask;
import io.openliberty.tools.ant.FeatureManagerTask.Feature;
import io.openliberty.tools.maven.InstallFeatureSupport;
import io.openliberty.tools.common.plugins.util.DevUtil;
import io.openliberty.tools.common.plugins.util.InstallFeatureUtil;
import io.openliberty.tools.common.plugins.util.PluginExecutionException;
import io.openliberty.tools.common.plugins.util.InstallFeatureUtil.ProductProperties;

/**
 * This mojo installs a feature packaged as a Subsystem Archive (esa) to the
 * runtime.
 */
@Mojo(name = "install-feature")
public class InstallFeatureMojo extends InstallFeatureSupport {
    
    /**
     * The container name if the features should be installed in a container.
     * Otherwise null.
     */
    @Parameter
    private String containerName;

    /*
     * (non-Javadoc)
     * @see org.codehaus.mojo.pluginsupport.MojoSupport#doExecute()
     */
    @Override
    protected void doExecute() throws Exception {
        if(!initialize()) {
            return;
        }
        installFeatures();
    }

    private void installFeatures() throws PluginExecutionException {
        // If non-container mode, check for Beta version and skip if needed.  Container mode does not need to check since featureUtility will check when it is called.
        List<ProductProperties> propertiesList = null;
        String openLibertyVersion = null;
        if (containerName == null) {
            propertiesList = InstallFeatureUtil.loadProperties(installDirectory);
            openLibertyVersion = InstallFeatureUtil.getOpenLibertyVersion(propertiesList);
    
            boolean skipBetaInstallFeatureWarning = Boolean.parseBoolean(System.getProperty(DevUtil.SKIP_BETA_INSTALL_WARNING));
            if (InstallFeatureUtil.isOpenLibertyBetaVersion(openLibertyVersion)) {
                if (!skipBetaInstallFeatureWarning) {
                    log.warn("Features that are not included with the beta runtime cannot be installed. Features that are included with the beta runtime can be enabled by adding them to your server.xml file.");
                }
                return; // do not install features if the runtime is a beta version
            }
        }

        Set<String> pluginListedEsas = getPluginListedFeatures(true);
        List<String> additionalJsons = getAdditionalJsonList();
        InstallFeatureUtil util = getInstallFeatureUtil(pluginListedEsas, propertiesList, openLibertyVersion, containerName, additionalJsons);
        Set<String> featuresToInstall = getSpecifiedFeatures(containerName);
        
        if(installFromAnt) {
            installFeaturesFromAnt(features.getFeatures());
        }
        else if(util != null) {
            util.installFeatures(features.isAcceptLicense(), new ArrayList<String>(featuresToInstall));
        }else if(util == null) {
        	throw new IllegalStateException(MessageFormat.format("Could not install feature", "install-feature"));
        }
        
       
    }

    @SuppressWarnings("deprecation")
    private void installFeaturesFromAnt(List<Feature> installFeatures) {
        // Set default outputDirectory to liberty-alt-output-dir for install-feature goal.
        if (defaultOutputDirSet) {
            outputDirectory = new File(project.getBuild().getDirectory(), "liberty-alt-output-dir");
        }

        InstallFeatureTask installFeatureTask = (InstallFeatureTask) ant
                .createTask("antlib:io/openliberty/tools/ant:install-feature");

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
