/*******************************************************************************
 * (c) Copyright IBM Corporation 2019, 2022.
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
import org.junit.Ignore;
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
      int generateFeaturesCount = countOccurrences(RUNNING_GENERATE_FEATURES, logFile);
      // configuration file change
      File srcServerXML = new File(tempProj, "/src/main/liberty/config/server.xml");
      File targetServerXML = new File(targetDir, "/liberty/wlp/usr/servers/defaultServer/server.xml");
      assertTrue(srcServerXML.exists());
      assertTrue(targetServerXML.exists());

      replaceString("</feature>", "</feature>\n" + "    <feature>mpHealth-1.0</feature>", srcServerXML);

      // check that features have been generated
      assertTrue(verifyLogMessageExists(RUNNING_GENERATE_FEATURES, 10000, ++generateFeaturesCount)); // mojo ran

      // check for server configuration was successfully updated message
      assertTrue(verifyLogMessageExists("CWWKG0017I", 60000));
      boolean foundUpdate = verifyLogMessageExists("<feature>mpHealth-1.0</feature>", 60000, targetServerXML);
      assertTrue("Could not find the updated feature in the target server.xml file", foundUpdate);
   }

   @Test
   public void configIncludesChangeTest() throws Exception {
      // add a feature to an <includes> server configuration file, ensure that
      // generate-features is called and the server configuration is updated
      int generateFeaturesCount = countOccurrences(RUNNING_GENERATE_FEATURES, logFile);
      File srcServerXMLIncludes = new File(tempProj, "/src/main/liberty/config/extraFeatures.xml");
      File targetServerXMLIncludes = new File(targetDir, "/liberty/wlp/usr/servers/defaultServer/extraFeatures.xml");
      assertTrue(srcServerXMLIncludes.exists());
      assertTrue(targetServerXMLIncludes.exists());

      replaceString("<!-- replace -->", "<feature>servlet-4.0</feature>", srcServerXMLIncludes);

      // check that features have been generated
      assertTrue(verifyLogMessageExists(RUNNING_GENERATE_FEATURES, 10000, ++generateFeaturesCount)); // mojo ran

      // check for server configuration was successfully updated message
      assertTrue(verifyLogMessageExists("CWWKG0017I", 60000));
      boolean foundUpdate = verifyLogMessageExists("<feature>servlet-4.0</feature>", 60000, targetServerXMLIncludes);
      assertTrue("Could not find the updated feature in the target extraFeatures.xml file", foundUpdate);
   }

   @Test
   public void resourceFileChangeTest() throws Exception {
      // CWWKZ0003I: The application xxx updated in y.yyy seconds.
      int appUpdatedCount = countOccurrences("CWWKZ0003I:", logFile);

      // make a resource file change
      File resourceDir = new File(tempProj, "src/main/resources");
      assertTrue(resourceDir.exists());

      File propertiesFile = new File(resourceDir, "microprofile-config.properties");
      assertTrue(propertiesFile.createNewFile());

      // dev mode copies file to target dir
      File targetPropertiesFile = new File(targetDir, "classes/microprofile-config.properties");
      assertTrue(getLogTail(), verifyFileExists(targetPropertiesFile, 30000)); // wait for dev mode
      assertTrue(getLogTail(), verifyLogMessageExists("CWWKZ0003I:", 10000, logFile, ++appUpdatedCount));

      // delete a resource file
      assertTrue(propertiesFile.delete());
      assertTrue(getLogTail(), verifyFileDoesNotExist(targetPropertiesFile, 10000));
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

      File unitTestTargetFile = new File(targetDir, "/test-classes/UnitTest.class");
      // wait for compilation
      assertTrue(getLogTail(), verifyFileExists(unitTestTargetFile, 6000));
      long lastModified = unitTestTargetFile.lastModified();
      waitLongEnough();

      // modify the test file
      String str = "// testing";
      BufferedWriter javaWriter = new BufferedWriter(new FileWriter(unitTestSrcFile, true));
      javaWriter.append(' ');
      javaWriter.append(str);

      javaWriter.close();

      assertTrue(getLogTail(), waitForCompilation(unitTestTargetFile, lastModified, 6000));

      // delete the test file
      assertTrue(getLogTail(), unitTestSrcFile.delete());
      assertTrue(getLogTail(), verifyFileDoesNotExist(unitTestTargetFile, 6000));
   }

   @Test
   public void manualTestsInvocationTest() throws Exception {
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

      // wait for compilation
      assertTrue(getLogTail(), verifyLogMessageExists("Source compilation was successful.", 100000));
      assertTrue(getLogTail(), verifyFileExists(systemHealthTarget, 15000));
   }

   @Test
   public void generateFeatureTest() throws Exception {
      final String SERVER_XML_COMMENT = "Plugin has generated Liberty features"; // the explanation added to server.xml
      final String NEW_FILE_INFO_MESSAGE = "This file was generated by the Liberty Maven Plugin and will be overwritten"; // the explanation added to the generated features file
      // After generate features is toggled off and on we end up with 'No functional changes were detected'
      final String SERVER_NOT_UPDATED = "CWWKG0018I:";
      final String SERVER_UPDATE_COMPLETE = "CWWKF0008I:"; // Feature update completed in 0.649 seconds.

      // Verify generate features runs when dev mode first starts
      assertTrue(verifyLogMessageExists(RUNNING_GENERATE_FEATURES, 10000));
      int generateFeaturesCount = countOccurrences(RUNNING_GENERATE_FEATURES, logFile);
      assertTrue(verifyLogMessageExists("Liberty is running in dev mode.", 10000)); // started
      assertFalse(verifyLogMessageExists("batch-1.0", 10000)); // this will be added

      File newFeatureFile = getGeneratedFeaturesFile();
      File newTargetFeatureFile = getTargetGeneratedFeaturesFile();
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
      verifyFileExists(helloBatchObj, 15000);
      // ... and run the proper mojo.
      assertTrue(verifyLogMessageExists(RUNNING_GENERATE_FEATURES, 10000, ++generateFeaturesCount)); // mojo ran
      assertTrue(verifyFileExists(newFeatureFile, 5000)); // mojo created file
      assertTrue(verifyFileExists(newTargetFeatureFile, 5000)); // dev mode copied file
      assertTrue(verifyLogMessageExists("batch-1.0", 10000, newFeatureFile));
      assertTrue(verifyLogMessageExists(NEW_FILE_INFO_MESSAGE, 10000, newFeatureFile));
      assertTrue(verifyLogMessageExists(SERVER_XML_COMMENT, 10000, serverXmlFile));
      // should appear as part of the message "CWWKF0012I: The server installed the following features:"
      assertTrue(verifyLogMessageExists("batch-1.0", 10000));

      // When there is a compilation error the generate features process should not run
      final String goodCode = "import javax.ws.rs.GET;";
      final String badCode  = "import javax.ws.rs.GET";
      final String errMsg = "Source compilation had errors.";
      int errCount = countOccurrences(errMsg, logFile);
      replaceString(goodCode, badCode, helloBatchSrc);
      assertTrue(getLogTail(), verifyLogMessageExists(errMsg, 15000, errCount+1)); // wait for compilation
      int updatedgenFeaturesCount = countOccurrences(RUNNING_GENERATE_FEATURES, logFile);
      assertEquals(generateFeaturesCount, updatedgenFeaturesCount);

      // Need valid code for testing.
      String goodCompile = "Source compilation was successful.";
      int goodCount = countOccurrences(goodCompile, logFile);
      replaceString(badCode, goodCode, helloBatchSrc);
      assertTrue(verifyLogMessageExists(goodCompile, 10000, goodCount+1));

      final String autoGenOff = "Setting automatic generation of features to: [ Off ]";
      final String autoGenOn  = "Setting automatic generation of features to: [ On ]";
      // toggle off
      writer.write("g\n");
      writer.flush();
      assertTrue(autoGenOff, verifyLogMessageExists(autoGenOff, 1000));
      // toggle on
      writer.write("g\n");
      writer.flush();
      assertTrue(autoGenOn, verifyLogMessageExists(autoGenOn, 10000));
      // Check for server response to regenerated feature list.
      assertTrue(SERVER_NOT_UPDATED, verifyLogMessageExists(SERVER_NOT_UPDATED, 10000));

      // Remove a class and use 'optimize' to rebuild the generated features
      assertTrue(helloBatchSrc.delete());
      assertTrue(verifyFileDoesNotExist(helloBatchSrc, 15000));
      assertTrue(verifyFileDoesNotExist(helloBatchObj, 15000));
      // Just removing the class file does not remove the feature because the feature
      // list is built in an incremental way.
      assertTrue(verifyLogMessageExists("batch-1.0", 100, newFeatureFile, 1));
      writer.write("o\n");
      writer.flush();
      assertTrue(verifyLogMessageExists("batch-1.0", 10000, newFeatureFile, 0)); // exist 0 times
      // Check for server response to newly generated feature list.
      assertTrue(SERVER_UPDATE_COMPLETE, verifyLogMessageExists(SERVER_UPDATE_COMPLETE, 10000));
      // Need to ensure server finished updating before the next test starts.
   }

}
