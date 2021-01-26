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
            getLog().info("\nSkipping prepare-server goal.\n");
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
