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

public class InstallFeaturesVersionlessWithVersionedServerTest extends BaseInstallFeature {

    @Test
    public void testInstalledFeatures() throws Exception {
        

        assertInstalled("beanValidation");
        assertInstalled("jsp");
        assertInstalled("enterpriseBeans-4.0");
        assertInstalled("ejb");
        assertInstalled("jdbc");
        assertInstalled("servlet-5.0");

        assertTrue(buildLogCheck("The following features have been installed: jdbc enterpriseBeansRemote-4.0 jsp connectors-2.0 enterpriseBeansPersistentTimer-4.0 enterpriseBeansLite-4.0 xmlBinding-3.0 mdb-4.0 jdbc-4.2 enterpriseBeans-4.0 expressionLanguage-4.0 pages-3.0 servlet-5.0 beanValidation-3.0 beanValidation jndi-1.0 ejb enterpriseBeansHome-4.0"));

        
        
               
    }

}
