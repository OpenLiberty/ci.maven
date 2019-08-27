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
package io.openliberty.tools.maven.server;

import java.text.MessageFormat;

import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.openliberty.tools.ant.UninstallFeatureTask;
import io.openliberty.tools.maven.BasicSupport;
import io.openliberty.tools.maven.server.types.Features;

/**
 * This mojo uninstalls a feature packaged as a Subsystem Archive (esa) from the
 * runtime.
 */
@Mojo(name = "uninstall-feature")

public class UninstallFeatureMojo extends BasicSupport {
    
    /**
     * Define a set of features to uninstall.
     */
    @Parameter
    private Features features;

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
            return;
        }
        
        if (features.getFeatures().size() <= 0) {
            throw new MojoExecutionException(
                    messages.getString("error.install.feature.set.validate"));
        }

        UninstallFeatureTask uninstallFeatureTask = (UninstallFeatureTask) ant
                .createTask("antlib:io/openliberty/tools/ant:uninstall-feature");

        if (uninstallFeatureTask == null) {
            throw new IllegalStateException(MessageFormat.format(messages.getString("error.dependencies.not.found"), "uninstall-feature"));
        }

        uninstallFeatureTask.setInstallDir(installDirectory);
        uninstallFeatureTask.setServerName(serverName);
        uninstallFeatureTask.setUserDir(userDirectory);
        uninstallFeatureTask.setOutputDir(outputDirectory);
        uninstallFeatureTask.setFeatures(features.getFeatures());
        uninstallFeatureTask.execute();
    }

}
