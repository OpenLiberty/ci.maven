/**
 * (C) Copyright IBM Corporation 2015, 2017.
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
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Component;

/**
 * This mojo installs a feature packaged as a Subsystem Archive (esa) to the
 * runtime.
 *
 * @Mojo( name = "install-feature" ) 
 */
@Mojo( name = "install-feature" ) 
public class InstallFeatureMojo extends BasicSupport {
    
    /**
     * Define a set of features to install in the server and the configuration
     * to be applied for all instances.
     *
     * @Component( role = Features.class ) 
     */
	@Component( role = Features.class )
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
        // whenFileExist is deprecated, but keep it to ensure backward compatibility
        installFeatureTask.setWhenFileExists(features.getWhenFileExists());
        installFeatureTask.setFeatures(features.getFeatures());
        installFeatureTask.setFrom(features.getFrom());
        installFeatureTask.execute();
    }

}
