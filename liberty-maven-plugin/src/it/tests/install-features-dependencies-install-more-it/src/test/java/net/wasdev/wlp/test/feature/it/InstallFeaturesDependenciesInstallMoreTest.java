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

import java.io.File;
import java.io.FilenameFilter;

import static junit.framework.Assert.*;
import org.junit.Test;

public class InstallFeaturesDependenciesInstallMoreTest {

    @Test
    public void testNumberOfFeatures() throws Exception {
        File dir = new File("liberty/wlp/lib/features");

        File[] features = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".mf");
            }
        });

        assertEquals("Number of installed features", 11, features.length);
        assertContains(features, "a-1.0");
        assertContains(features, "b-1.0");
        assertContains(features, "c-1.0");
        assertContains(features, "d-1.0");
    }

    private void assertContains(File[] files, String feature) {
        boolean found = false;
        for (File file : files) {
            if (file.getName().endsWith("." + feature + ".mf")) {
                found = true;
                break;
            }
        }
        assertTrue("Feature " + feature + " was not installed", found);
    }

}
