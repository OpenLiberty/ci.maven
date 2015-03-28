/**
 * (C) Copyright IBM Corporation 2015.
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

import net.wasdev.wlp.ant.InstallFeatureTask;
import net.wasdev.wlp.maven.plugins.BasicSupport;
import net.wasdev.wlp.maven.plugins.server.types.Features;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * This mojo installs a feature packaged as a Subsystem Archive (esa) to the
 * runtime.
 *
 * @goal install-feature
 */
public class InstallFeatureMojo extends BasicSupport {
    
    /**
     * Define a set of features to install in the server and the configuration
     * to be applied for all instances.
     *
     * @parameter
     */
    private Features features;

    /*
     * (non-Javadoc)
     * @see org.codehaus.mojo.pluginsupport.MojoSupport#doExecute()
     */
    @Override
    protected void doExecute() throws Exception {
        if (skip == true) {
            return;
        }
        
        // for liberty-assembly integration
        if (features == null) {
            return;
        }
        
        if (features.getFeatures().size() <= 0) {
            throw new MojoExecutionException(
                    messages.getString("error.install.feature.set.validate"));
        }

        InstallFeatureTask installFeatureTask = (InstallFeatureTask) ant
                .createTask("antlib:net/wasdev/wlp/ant:install-feature");

        if (installFeatureTask == null) {
            throw new NullPointerException("Install feature task not found.");
        }

        installFeatureTask.setInstallDir(installDirectory);
        installFeatureTask.setServerName(serverName);
        installFeatureTask.setUserDir(userDirectory);
        installFeatureTask.setOutputDir(outputDirectory);
        installFeatureTask.setAcceptLicense(features.isAcceptLicense());
        installFeatureTask.setTo(features.getTo());
        installFeatureTask.setWhenFileExists(features.getWhenFileExists());
        installFeatureTask.setName(features.getFeaturesAsString());
        installFeatureTask.execute();
    }

}
