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

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.Scanner;

import org.apache.maven.shared.utils.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class DevTest extends BaseDevTest {

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      setUpBeforeClass(null);
   }

   @AfterClass
   public static void cleanUpAfterClass() throws Exception {
      BaseDevTest.cleanUpAfterClass();
   }

   @Test
   /* simple double check. if failure, check parse in ci.common */
   public void verifyJsonHost() throws Exception {
      checkLogMessage(2000, "CWWKT0016I");   // Verify web app code triggered
      checkLogMessage(2000, "http:\\/\\/");  // Verify escape char seq passes
   }

   @Test
   public void basicTest() throws Exception {
      testModifyJavaFile();
   }

   @Test
   public void configChangeTest() throws Exception {
      // configuration file change
      File srcServerXML = new File(tempProj, "/src/main/liberty/config/server.xml");
      File targetServerXML = new File(targetDir, "/liberty/wlp/usr/servers/defaultServer/server.xml");
      assertTrue(srcServerXML.exists());
      assertTrue(targetServerXML.exists());

      replaceString("</feature>", "</feature>\n" + "    <feature>mpHealth-1.0</feature>", srcServerXML);

      // check for server configuration was successfully updated message
      assertFalse(checkLogMessage(60000, "CWWKG0017I"));
      Thread.sleep(2000);
      Scanner scanner = new Scanner(targetServerXML);
      boolean foundUpdate = false;
      try {
         while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.contains("<feature>mpHealth-1.0</feature>")) {
               foundUpdate = true;
               break;
            }
         }
      } finally {
            scanner.close();
      }
      assertTrue("Could not find the updated feature in the target server.xml file", foundUpdate);
   }

   @Test
   public void resourceFileChangeTest() throws Exception {
      // make a resource file change
      File resourceDir = new File(tempProj, "src/main/resources");
      assertTrue(resourceDir.exists());

      File propertiesFile = new File(resourceDir, "microprofile-config.properties");
      assertTrue(propertiesFile.createNewFile());

      Thread.sleep(2000); // wait for compilation
      File targetPropertiesFile = new File(targetDir, "classes/microprofile-config.properties");
      assertTrue(targetPropertiesFile.exists());
      assertFalse(checkLogMessage(100000, "CWWKZ0003I"));

      // delete a resource file
      assertTrue(propertiesFile.delete());
      Thread.sleep(2000);
      assertFalse(targetPropertiesFile.exists());
   }
   
   @Test
   public void testDirectoryTest() throws Exception {
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

      Thread.sleep(6000); // wait for compilation
      File unitTestTargetFile = new File(targetDir, "/test-classes/UnitTest.class");
      assertTrue(unitTestTargetFile.exists());
      long lastModified = unitTestTargetFile.lastModified();

      // modify the test file
      String str = "// testing";
      BufferedWriter javaWriter = new BufferedWriter(new FileWriter(unitTestSrcFile, true));
      javaWriter.append(' ');
      javaWriter.append(str);

      javaWriter.close();

      Thread.sleep(2000); // wait for compilation
      assertTrue(unitTestTargetFile.lastModified() > lastModified);

      // delete the test file
      assertTrue(unitTestSrcFile.delete());
      Thread.sleep(2000);
      assertFalse(unitTestTargetFile.exists());

   }

   @Test
   public void manualTestsInvocationTest() throws Exception {
      assertFalse(checkLogMessage(2000,  "Press the Enter key to run tests on demand."));

      writer.write("\n");
      writer.flush();

      assertFalse(checkLogMessage(10000,  "Unit tests finished."));
      assertFalse(checkLogMessage(2000,  "Integration tests finished."));
   }
   
    @Test
    public void invalidDependencyTest() throws Exception {
        // add invalid dependency to pom.xml
        String invalidDepComment = "<!-- <dependency>\n" + "        <groupId>io.openliberty.features</groupId>\n"
                + "        <artifactId>abcd</artifactId>\n" + "        <version>1.0</version>\n"
                + "    </dependency> -->";
        String invalidDep = "<dependency>\n" + "        <groupId>io.openliberty.features</groupId>\n"
                + "        <artifactId>abcd</artifactId>\n" + "        <version>1.0</version>\n" + "    </dependency>";
        replaceString(invalidDepComment, invalidDep, pom);
        assertFalse(checkLogMessage(10000, "Unable to resolve artifact: io.openliberty.features:abcd:1.0"));
    }
   
   @Test
   public void resolveDependencyTest() throws Exception {      
      assertFalse(checkLogMessage(10000,  "Press the Enter key to run tests on demand."));

      // create the HealthCheck class, expect a compilation error
      File systemHealthRes = new File("../resources/SystemHealth.java");
      assertTrue(systemHealthRes.exists());
      File systemHealthSrc = new File(tempProj, "/src/main/java/com/demo/SystemHealth.java");
      File systemHealthTarget = new File(targetDir, "/classes/com/demo/SystemHealth.class");

      FileUtils.copyFile(systemHealthRes, systemHealthSrc);
      assertTrue(systemHealthSrc.exists());
      
      assertFalse(checkLogMessage(200000, "Source compilation had errors"));
      assertFalse(systemHealthTarget.exists());
      
      // add mpHealth dependency to pom.xml
      String mpHealthComment = "<!-- <dependency>\n" + 
            "        <groupId>io.openliberty.features</groupId>\n" + 
            "        <artifactId>mpHealth-1.0</artifactId>\n" + 
            "        <type>esa</type>\n" + 
            "        <scope>provided</scope>\n" + 
            "    </dependency> -->";
      String mpHealth = "<dependency>\n" + 
            "        <groupId>io.openliberty.features</groupId>\n" + 
            "        <artifactId>mpHealth-1.0</artifactId>\n" + 
            "        <type>esa</type>\n" + 
            "        <scope>provided</scope>\n" + 
            "    </dependency>";
      replaceString(mpHealthComment, mpHealth, pom);
      
      assertFalse(checkLogMessage(100000,"The following features have been installed"));
      
      String str = "// testing";
      BufferedWriter javaWriter = new BufferedWriter(new FileWriter(systemHealthSrc, true));
      javaWriter.append(' ');
      javaWriter.append(str);

      javaWriter.close();

      Thread.sleep(1000); // wait for compilation
      assertFalse(checkLogMessage(100000, "Source compilation was successful."));
      Thread.sleep(15000); // wait for compilation
      assertTrue(systemHealthTarget.exists());
   }

}
