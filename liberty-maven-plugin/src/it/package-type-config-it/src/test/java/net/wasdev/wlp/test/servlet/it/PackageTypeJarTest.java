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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.io.IOException;

import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.jar.JarFile;

import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * Test various values for packageType for package goal
 * 
 */
public class PackageTypeJarTest {
    
    private String projectBuildName = "package-type-config-it.jar";
    
    @Test
    public void testPackageFileJarExists() {
        String pathname = System.getProperty("user.dir") + "/" + projectBuildName;
        Path path = Paths.get(pathname);
        Assert.assertTrue(Files.exists(path));
    }

    @Test
    public void testJarFileIsSelfExtracting() {
        File packagedJarFile = new File(System.getProperty("user.dir") +  "/" + projectBuildName);

        try {
            JarFile fileToProcess = new JarFile(packagedJarFile.getAbsoluteFile(), false);
            // If a manifest file has an invalid format, an IOException is thrown.
            // Catch the exception so that the rest of the archive can be processed.
            Manifest mf = fileToProcess.getManifest();
            Assert.assertNotNull(mf);

            Attributes manifestMap = mf.getMainAttributes();
            String value = (manifestMap != null) ? manifestMap.getValue("Main-Class") : null;
            Assert.assertEquals("Expected Main-Class manifest value for self extracting jar not found.","wlp.lib.extract.SelfExtract", value);
        } catch (Exception e) {
            Assert.fail("Unexpected exception when checking the Jar Manifest for Main-Class attribute.");

        }
    }

}
