/*******************************************************************************
 * (c) Copyright IBM Corporation 2018, 2024.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package net.wasdev.wlp.test.feature.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.Set;

import org.junit.Before;

import io.openliberty.tools.common.plugins.util.InstallFeatureUtil;

public class BaseInstallFeature {
	
	protected File[] features;
    
    @Before
    public void setUp() throws Exception {
        File dir = new File("liberty/wlp/lib/features");

        features = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".mf");
            }
        });
    }
    
    protected void assertInstallStatus(String feature, boolean expectation) throws Exception {
        String expectationString = (expectation ? "installed" : "not installed");
        assertEquals("Feature " + feature + " was expected to be " + expectationString + " in the lib/features directory", expectation, existsInFeaturesDirectory(feature));
        String featureInfo = getFeatureInfo();
        assertEquals("Feature " + feature + " was expected to be " + expectationString + " according to productInfo featureInfo: " + featureInfo, expectation, featureInfo.contains(feature));
    }

    protected void assertInstalled(String feature) throws Exception {
        assertInstallStatus(feature, true);
    }
    
    protected void assertNotInstalled(String feature) throws Exception {
        assertInstallStatus(feature, false);
    }
    
    protected boolean existsInFeaturesDirectory(String feature) {
        boolean found = false;
        for (File file : features) {
            if ((file.getName().equals("com.ibm.websphere.appserver." + feature + ".mf")) ||
                (file.getName().equals("io.openliberty.versionless." + feature + ".mf")) ||
                (file.getName().equals("io.openliberty.jakarta." + feature + ".mf")) ||
                (file.getName().equals("io.openliberty." + feature + ".mf"))) {
                found = true;
                break;
            }
        }
        return found;
    }
    
    protected String getFeatureInfo() throws Exception {
        File installDirectory = new File("liberty", "wlp");
        return InstallFeatureUtil.productInfo(installDirectory, "featureInfo");
    }


    public boolean buildLogCheckForInstalledFeatures(String testname, String msg, Set<String> installedFeatures) throws Exception {
        
        File buildLog = new File("../build.log");
	if (!buildLog.exists()) {
	    buildLog = new File("../../build.log");
	}
        assertTrue(buildLog.exists());        
        boolean foundTestName = false;

        try (InputStream buildOutput = new FileInputStream(buildLog); InputStreamReader in = new InputStreamReader(buildOutput); Scanner s = new Scanner(in);) {
            while (s.hasNextLine()) {
                String line = s.nextLine();
                if (foundTestName && line.contains(msg)) {
                    // check for all features in message
                    String messageFeatures = line.substring(line.indexOf(":") + 1);
                    String[] messageFeaturesArray = messageFeatures.trim().split(" ");
                    assertTrue("The number of installed features ("+messageFeaturesArray.length+") is different than expected number ("+installedFeatures.size()+"). The log message is: "+line,messageFeaturesArray.length == installedFeatures.size());
                    for (int i=0; i < messageFeaturesArray.length; i++) {
                        assertTrue("Found unexpected feature "+messageFeaturesArray[i]+" in list of installed features in message in build.log.",installedFeatures.contains(messageFeaturesArray[i].trim()));
                    }
                    return true;
                } else if (line.contains(testname)) {
                    // process next feature installation message
                    foundTestName = true;
                }
            }
        } catch (Exception e) {
            System.out.println("Error checking build.log " + e.getMessage());
        }

        return false;
    }
	
}
