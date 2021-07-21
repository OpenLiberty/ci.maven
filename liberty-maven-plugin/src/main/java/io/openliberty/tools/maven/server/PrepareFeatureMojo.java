/**
 * (C) Copyright IBM Corporation 2021.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugins.annotations.Mojo;

import io.openliberty.tools.common.plugins.util.InstallFeatureUtil;
import io.openliberty.tools.common.plugins.util.PluginExecutionException;
import io.openliberty.tools.common.plugins.util.PrepareFeatureUtil;
import io.openliberty.tools.common.plugins.util.InstallFeatureUtil.ProductProperties;
import io.openliberty.tools.maven.PrepareFeatureSupport;


/**
 * This mojo generates JSON files so user features can be installed
 * from a specified BOM
 */
@Mojo(name = "prepare-feature")
public class PrepareFeatureMojo extends PrepareFeatureSupport {
	
	
    @Override
    protected void doExecute() throws Exception {
        if (skip) {
            getLog().info("\nSkipping prepare-feature goal.\n");
            return;
        }
        
        prepareFeatures();
    }
    
    private void prepareFeatures() throws PluginExecutionException {
    	List<ProductProperties> propertiesList = InstallFeatureUtil.loadProperties(installDirectory);
        String openLibertyVersion = InstallFeatureUtil.getOpenLibertyVersion(propertiesList);
        PrepareFeatureUtil util = getPrepareFeatureUtil(openLibertyVersion);
        //Get list of BOM dependencies from dependencyManagement section
        List<String> dependencyListedBOMs = getDependencyBOMs();
        if(util != null) {
            util.prepareFeatures(dependencyListedBOMs);
        }
    }

}
