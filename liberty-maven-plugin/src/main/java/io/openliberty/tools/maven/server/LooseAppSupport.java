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

package io.openliberty.tools.maven.server;

import java.io.File;

import org.apache.maven.project.MavenProject;

import io.openliberty.tools.common.plugins.util.DevUtil;
import io.openliberty.tools.maven.utils.MavenProjectUtil;

/**
 * Loose application Liberty server support.
 */
public abstract class LooseAppSupport extends PluginConfigSupport {

    // get loose application configuration file name for project artifact
    public String getLooseConfigFileName(MavenProject project) {
        return getPostDeployAppFileName(project) + ".xml";
    }

    // get loose application file name for project artifact
    protected String getPostDeployAppFileName(MavenProject project) {
        return getAppFileName(project, true);
    }

    // target ear/war produced by war:war, ear:ear, haven't stripped version yet
    protected String getPreDeployAppFileName(MavenProject project) {
        return getAppFileName(project, false);
    }

    protected String getAppFileName(MavenProject project, boolean stripVersionIfConfigured) {

        String name = project.getBuild().getFinalName();

        if (stripVersionIfConfigured && stripVersion) { // TODO stripVersion is set to false when called from dev mojo
            name = stripVersionFromName(name, project.getVersion());
        }

        String classifier = MavenProjectUtil.getAppNameClassifier(project);
        if (classifier != null) {
            name += "-" + classifier;
        }

        String packagingType = project.getPackaging();
        if (packagingType.equals("liberty-assembly")) {
            name += ".war";
        } else if (packagingType.equals("ejb") || packagingType.equals("bundle")) {
            name += ".jar";
        } else {
            name += "." + packagingType;
        }

        return name;
    }

    /**
     * Gets the loose application configuration xml file
     * 
     * @param proj      MavenProject
     * @param container whether the app is running in a container
     * @return File the loose application configuration xml file
     */
    public File getLooseAppConfigFile(MavenProject proj, boolean container) {
        String looseConfigFileName = getLooseConfigFileName(proj);
        if (container) {
            File devcDestDir = new File(new File(project.getBuild().getDirectory(), DevUtil.DEVC_HIDDEN_FOLDER),
                    getAppsDirectory(false));
            File devcLooseConfigFile = new File(devcDestDir, looseConfigFileName);
            return devcLooseConfigFile;
        } else {
            File destDir = new File(serverDirectory, getAppsDirectory(false));
            File looseConfigFile = new File(destDir, looseConfigFileName);
            return looseConfigFile;
        }
    }
    
}
