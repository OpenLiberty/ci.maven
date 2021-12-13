/*******************************************************************************
 * (c) Copyright IBM Corporation 2019, 2021.
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

import io.openliberty.tools.maven.server.GenerateFeaturesMojo;

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
      assertTrue(verifyLogMessageExists("CWWKT0016I", 2000));   // Verify web app code triggered
      //TODO: fix below with correct assertion
      verifyLogMessageExists("http:\\/\\/", 2000);  // Verify escape char seq passes
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
      assertTrue(verifyLogMessageExists("CWWKG0017I", 60000));
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
      assertTrue(verifyLogMessageExists("CWWKZ0003I", 100000));

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
      // assertTrue(verifyLogMessageExists("To run tests on demand, press Enter.", 2000));

      writer.write("\n");
      writer.flush();

      assertTrue(verifyLogMessageExists("Unit tests finished.", 10000));
      assertTrue(verifyLogMessageExists("Integration tests finished.", 2000));
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
        assertTrue(verifyLogMessageExists("The POM for io.openliberty.features:abcd:jar:1.0 is missing, no dependency information available", 10000));
    }
   
   @Test
   public void resolveDependencyTest() throws Exception {      
      assertTrue(verifyLogMessageExists("Liberty is running in dev mode.", 10000));

      // create the HealthCheck class, expect a compilation error
      File systemHealthRes = new File("../resources/SystemHealth.java");
      assertTrue(systemHealthRes.exists());
      File systemHealthSrc = new File(tempProj, "/src/main/java/com/demo/SystemHealth.java");
      File systemHealthTarget = new File(targetDir, "/classes/com/demo/SystemHealth.class");

      FileUtils.copyFile(systemHealthRes, systemHealthSrc);
      assertTrue(systemHealthSrc.exists());
      
      assertTrue(verifyLogMessageExists("Source compilation had errors", 200000));
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
      
      assertTrue(verifyLogMessageExists("The following features have been installed", 100000));
      
      String str = "// testing";
      BufferedWriter javaWriter = new BufferedWriter(new FileWriter(systemHealthSrc, true));
      javaWriter.append(' ');
      javaWriter.append(str);

      javaWriter.close();

      Thread.sleep(1000); // wait for compilation
      assertTrue(verifyLogMessageExists("Source compilation was successful.", 100000));
      Thread.sleep(15000); // wait for compilation
      assertTrue(systemHealthTarget.exists());
   }

   // TODO @Test
   public void generateFeatureTest() throws Exception {

      final String GENERATED_FEATURES_FILE_NAME = "generated-features.xml";
      final String SERVER_XML_COMMENT = "Plugin has generated Liberty features"; // the explanation added to server.xml
      // TODO final String NEW_FILE_INFO_MESSAGE = "Some message"; // the explanation added to the generated features file

      assertTrue(verifyLogMessageExists("Liberty is running in dev mode.", 10000));
      assertFalse(verifyLogMessageExists("batch-1.0", 10000));

      File newFeatureFile = new File(tempProj, "/src/main/liberty/config/configDropins/overrides/"+GENERATED_FEATURES_FILE_NAME);
      File newTargetFeatureFile = new File(targetDir, "/liberty/wlp/usr/servers/defaultServer/configDropins/overrides/"+GENERATED_FEATURES_FILE_NAME);
      File serverXmlFile = new File(tempProj, "/src/main/liberty/config/server.xml");
      assertTrue(serverXmlFile.exists());

      // Copy a Java file into place to create the HelloBatch class
      File helloBatchRes = new File("../resources/HelloBatch.java");
      assertTrue(helloBatchRes.exists());
      File helloBatchSrc = new File(tempProj, "/src/main/java/com/demo/HelloBatch.java");
      FileUtils.copyFile(helloBatchRes, helloBatchSrc);
      assertTrue(helloBatchSrc.exists());

      // Dev mode should now compile the new Java file...
      File helloBatchObj = new File(tempProj, "target/classes/com/demo/HelloBatch.class");
      verifyFileExists(newTargetFeatureFile, 15000);
      // ... and run the proper mojo.
      assertTrue(verifyLogMessageExists("Running liberty:generate-features", 10000)); // mojo ran
      assertTrue(verifyFileExists(newFeatureFile, 5000)); // mojo created file
      assertTrue(verifyFileExists(newTargetFeatureFile, 5000)); // mojo copied file
      assertTrue(verifyLogMessageExists("batch-1.0", 10000, newFeatureFile));
      // TODO assertTrue(verifyLogMessageExists(NEW_FILE_INFO_MESSAGE, 10000, newFeatureFile));
      assertTrue(verifyLogMessageExists(SERVER_XML_COMMENT, 10000, serverXmlFile));
      assertTrue(verifyLogMessageExists("batch-1.0", 10000)); // should appear in the message "CWWKF0012I: The server installed the following features:"
   }

   @Test
   public void mpVersionTest() throws Exception {
      assertEquals(GenerateFeaturesMojo.getMPVersion("mpHealth-2.0"), 3);
      assertEquals(GenerateFeaturesMojo.getMPVersion("mpconfig-1.0"), 1);
      assertEquals(GenerateFeaturesMojo.getMPVersion("mpconfig-1.3"), 2);
      assertEquals(GenerateFeaturesMojo.getMPVersion("mpconfig-1.4"), 3);
      assertEquals(GenerateFeaturesMojo.getMPVersion("mprestclient-1.0"), 1);
      assertEquals(GenerateFeaturesMojo.getMPVersion("mprestclient-1.1"), 1);
      assertEquals(GenerateFeaturesMojo.getMPVersion("mprestclient-1.2"), 2);
      assertEquals(GenerateFeaturesMojo.getMPVersion("mprestclient-1.3"), 3);
      assertEquals(GenerateFeaturesMojo.getMPVersion("mprestclient-1.4"), 3);
      assertEquals(GenerateFeaturesMojo.getMPVersion("mpjwt-1.0"), 1);
      assertEquals(GenerateFeaturesMojo.getMPVersion("mpjwt-1.1"), 3);
      assertEquals(GenerateFeaturesMojo.getMPVersion("mpjwt-1.2"), 4);
      // Error testing
      assertEquals(GenerateFeaturesMojo.getMPVersion("mprestclientX-1.5"), 0);
      assertEquals(GenerateFeaturesMojo.getMPVersion("mprestclient1.5"), 0);
      assertEquals(GenerateFeaturesMojo.getMPVersion("mprestclient-1.5a"), 0);
      assertEquals(GenerateFeaturesMojo.getMPVersion("mprestclient-10"), 0);
      assertEquals(GenerateFeaturesMojo.getMPVersion("Xmprestclient-1.0"), 0);
   }
}
