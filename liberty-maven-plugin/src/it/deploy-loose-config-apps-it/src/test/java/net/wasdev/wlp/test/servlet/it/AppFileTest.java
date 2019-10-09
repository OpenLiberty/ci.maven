/*******************************************************************************
 * (c) Copyright IBM Corporation 2019.
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
package net.wasdev.wlp.test.servlet.it;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * Deployed app archive to apps test case
 * 
 */
public class AppFileTest {
    @Test
    public void testApplicationFileExist() throws Exception {
        File f = new File("liberty/usr/servers/test/apps/deploy-loose-config-apps-it.war");
        Assert.assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
    }
}
