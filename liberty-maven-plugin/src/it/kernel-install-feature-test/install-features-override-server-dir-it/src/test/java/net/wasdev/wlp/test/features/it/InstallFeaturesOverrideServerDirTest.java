/*******************************************************************************
 * (c) Copyright IBM Corporation 2022.
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

import org.junit.Test;

public class InstallFeaturesOverrideServerDirTest extends BaseInstallFeature {
    
    @Test
    public void testInstalledFeatures() throws Exception {
        assertInstalled("jsp-2.3"); // specified in extra-features.xml
        // ensure that the serverDir param is working as expected and 
        // servlet-3.1 is not installed as servlet-4.0 is specified
        assertNotInstalled("servlet-3.1");
        assertInstalled("appSecurityClient-1.0"); // specified in the server.xml
        assertInstalled("servlet-4.0"); // specified in server.xml
    }
}
