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
package net.wasdev.wlp.test.dev.it;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.Scanner;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class DevTest extends BaseDevTest {

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      BaseDevTest.setUpBeforeClass();
   }

   @AfterClass
   public static void cleanUpAfterClass() throws Exception {
      BaseDevTest.cleanUpAfterClass();
   }

   @Test
   public void basicTest() throws Exception {

      if (!isWindows) { // skip tests on windows until server.env bug is fixed
         testModifyJavaFile();
      }
   }

   @Test
   public void configChangeTest() throws Exception {

      if (!isWindows) { // skip tests on windows until server.env bug is fixed

         // configuration file change
         File srcServerXML = new File(tempProj, "/src/main/liberty/config/server.xml");
         File targetServerXML = new File(targetDir, "/liberty/wlp/usr/servers/defaultServer/server.xml");
         assertTrue(srcServerXML.exists());
         assertTrue(targetServerXML.exists());

         replaceString("</feature>", "</feature>\n" + "    <feature>mpHealth-1.0</feature>", srcServerXML);

         // check for application updated message
         assertFalse(checkLogMessage(60000, "CWWKZ0003I"));
         Thread.sleep(2000);
         Scanner scanner = new Scanner(targetServerXML);

         while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.contains("<feature>mpHealth-1.0</feature>")) {
               assertTrue(true);
            }
         }
      }

   }

   @Test
   public void unhandledChangeTest() throws Exception {

      if (!isWindows) { // skip tests on windows until server.env bug is fixed

         // make an unhandled change to the pom.xml
         replaceString("dev-sample-proj", "dev-sample-project", pom);
         assertFalse(checkLogMessage(100000, "Unhandled change detected in pom.xml"));
      }
   }

   @Test
   public void resourceFileChangeTest() throws Exception {
      if (!isWindows) { // skip tests on windows until server.env bug is fixed

         // make a resource file change
         File resourceDir = new File(tempProj, "/src/main/resources");
         assertTrue(resourceDir.exists());

         File propertiesFile = new File(resourceDir, "/microprofile-config.properties");
         assertTrue(propertiesFile.createNewFile());

         Thread.sleep(2000); // wait for compilation
         File targetPropertiesFile = new File(targetDir, "/classes/microprofile-config.properties");
         assertTrue(targetPropertiesFile.exists());
         assertFalse(checkLogMessage(100000, "CWWKZ0003I"));

         // delete a resource file
         assertTrue(propertiesFile.delete());
         Thread.sleep(2000);
         assertFalse(targetPropertiesFile.exists());
      }
   }
   
   @Test
   public void testDirectoryTest() throws Exception {
      if (!isWindows) { // skip tests on windows until server.env bug is fixed

         // create the test directory
         File testDir = new File(tempProj, "src/test/java");
         assertTrue(testDir.mkdirs());

         // creates a java test file
         File unitTestSrcFile = new File(testDir, "UnitTest.java");
         String unitTest = "import org.junit.Test;\n" + "import static org.junit.Assert.*;\n" + "\n"
               + "public class UnitTest {\n" + "\n" + "    @Test\n" + "    public void testTrue() {\n"
               + "        assertTrue(true);\n" + "\n" + "    }\n" + "}";
         Files.write(unitTestSrcFile.toPath(), unitTest.getBytes());
         assertTrue(unitTestSrcFile.exists());

         Thread.sleep(2000); // wait for compilation
         File unitTestTargetFile = new File(targetDir, "/test-classes/UnitTest.class");
         assertTrue(unitTestTargetFile.exists());
         long lastModified = unitTestTargetFile.lastModified();

         // modify the test file
         String str = "// testing";
         BufferedWriter javaWriter = new BufferedWriter(new FileWriter(unitTestSrcFile, true));
         javaWriter.append(' ');
         javaWriter.append(str);

         javaWriter.close();

         Thread.sleep(5000); // wait for compilation
         assertTrue(unitTestTargetFile.lastModified() > lastModified);

         // delete the test file
         assertTrue(unitTestSrcFile.delete());
         Thread.sleep(2000);
         assertFalse(unitTestTargetFile.exists());
      }
   }

}
