/*******************************************************************************
 * (c) Copyright IBM Corporation 2024.
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

import static org.junit.Assert.*;
import org.junit.Test;
import java.io.File;
import java.util.Set;
import java.util.HashSet;

public class InstallFeaturesVersionlessWithVersionedServerTest extends BaseInstallFeature {

    @Test
    public void testInstalledFeatures() throws Exception {
        

        assertInstalled("beanValidation");
        assertInstalled("jsp");
        assertInstalled("enterpriseBeans-4.0");
        assertInstalled("ejb");
        assertInstalled("jdbc");
        assertInstalled("servlet-5.0");
        
        Set<String> expectedFeatures = new HashSet<String>();
        expectedFeatures.add("jdbc");
        expectedFeatures.add("enterpriseBeansRemote-4.0");
        expectedFeatures.add("jsp");
        expectedFeatures.add("connectors-2.0");
        expectedFeatures.add("enterpriseBeansPersistentTimer-4.0");
        expectedFeatures.add("enterpriseBeansLite-4.0");
        expectedFeatures.add("xmlBinding-3.0");
        expectedFeatures.add("mdb-4.0");
        expectedFeatures.add("jdbc-4.2");
        expectedFeatures.add("enterpriseBeans-4.0");
        expectedFeatures.add("expressionLanguage-4.0");
        expectedFeatures.add("pages-3.0");
        expectedFeatures.add("beanValidation-3.0");
        expectedFeatures.add("beanValidation");
        expectedFeatures.add("servlet-5.0");
        expectedFeatures.add("jndi-1.0");
        expectedFeatures.add("ejb");
        expectedFeatures.add("enterpriseBeansHome-4.0");

        assertTrue(buildLogCheckForInstalledFeatures("io.openliberty.tools.it:install-features-versionless-with-versioned-server-it", "The following features have been installed:", expectedFeatures));

        
        
        
               
    }

}
