/*******************************************************************************
 * (c) Copyright IBM Corporation 2018.
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

import static junit.framework.Assert.*;
import org.junit.Test;

public class InstallFeaturesDependenciesAlreadyInstalledTest extends BaseInstallFeature {

    @Test
    public void testInstalledFeatures() throws Exception {
        assertInstalled("appSecurityClient-1.0");
        assertNotInstalled("beanValidation-2.0");
        assertNotInstalled("couchdb-1.0");
        assertNotInstalled("distributedMap-1.0");
    }

}
