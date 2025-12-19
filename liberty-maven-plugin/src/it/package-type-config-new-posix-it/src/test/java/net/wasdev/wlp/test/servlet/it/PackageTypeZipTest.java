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

import java.util.Enumeration;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * Test various values for packageType for package goal. This test also tests serverRoot parameter.
 * 
 */
public class PackageTypeZipTest {
    
    private String projectBuildName = "package-type-config-new-posix-it.zip";
    
    @Test
    public void testPackageFileZipExists() {
        String pathname = System.getProperty("user.dir") + "/" + projectBuildName;
        Path path = Paths.get(pathname);
        Assert.assertTrue(Files.exists(path));
    }

    @Test
    public void testZipFileServerRoot() {
        File packagedZipFile = new File(System.getProperty("user.dir") +  "/" + projectBuildName);

        try {
            ZipFile fileToProcess = new ZipFile(packagedZipFile.getAbsoluteFile());

            Enumeration<? extends ZipEntry> entries = fileToProcess.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();
                
                if (entry.isDirectory()) {
                    Assert.assertTrue("Zip file server root is not correct.",entryName.startsWith("myServerRoot"));
                    break;
                }
            }
        } catch (Exception e) {
            Assert.fail("Unexpected exception when checking the zip server root folder.");

        }
    }

}
