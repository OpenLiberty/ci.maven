/**
 * (C) Copyright IBM Corporation 2023.
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
package application;

import static org.junit.Assert.*;

import java.io.File;
import org.junit.Test;

public class InstallSpringBoot30AppIT {

    @Test
    public void testThinApplicationExistsInAppsDirectory() throws Exception {

        File f = new File("target/liberty/wlp/usr/servers/test/apps/thin-springboot-3-appsdirectory-apps-it-1.0.0.Final-exec.jar");
        assertTrue(f.getCanonicalFile() + " doesn't exist. Plugin failed to place the file at right destination.", f.exists());
        File f2 = new File("target/liberty/wlp/usr/servers/test/configDropins/defaults");
        assertTrue(f2.getCanonicalFile() + " folder doesn't exist. Plugin failed to create the config dropins folder ", f2.exists());
        assertTrue(f2.getCanonicalFile() + " folder doesn't contain any files. Plugin failed to place config xml at right destination.", f2.list().length == 1);
    }

    @Test
    public void testLibIndexCacheExists() throws Exception {
        File f = new File("target/liberty/wlp/usr/shared/resources/lib.index.cache");
        assertTrue(f.getCanonicalFile()+ " doesn't exist. Plugin failed to place the cache directory at right destination.", f.exists());
    }
}