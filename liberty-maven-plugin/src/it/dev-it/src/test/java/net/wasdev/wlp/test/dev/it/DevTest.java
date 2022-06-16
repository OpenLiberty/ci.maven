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
      assertTrue(verifyLogMessageExists(WEB_APP_AVAILABLE, 2000));   // Verify web app code triggered
      //TODO: fix below with correct assertion
      verifyLogMessageExists("http:\\/\\/", 2000);  // Verify escape char seq passes
   }

   @Test
   public void basicTest() throws Exception {
      testModifyJavaFile();
   }

   @Test
   public void configChangeTest() throws Exception {
      tagLog("##configChangeTest start");
      int generateFeaturesCount = countOccurrences(RUNNING_GENERATE_FEATURES, logFile);

      // configuration file change
      File srcServerXML = new File(tempProj, "/src/main/liberty/config/server.xml");
      File targetServerXML = new File(targetDir, "/liberty/wlp/usr/servers/defaultServer/server.xml");
      assertTrue(srcServerXML.exists());
      assertTrue(targetServerXML.exists());

      replaceString("</feature>", "</feature>\n" + "    <feature>mpFaultTolerance-2.0</feature>", srcServerXML);

      // check that features have been generated
      assertTrue(verifyLogMessageExists(RUNNING_GENERATE_FEATURES, 10000, ++generateFeaturesCount)); // mojo ran

      // check for server configuration was successfully updated message
      assertTrue(verifyLogMessageExists(SERVER_UPDATED, 60000));
      verifyFileExists(targetServerXML, 11000); // ensure file copy is complete.
      boolean foundUpdate = verifyLogMessageExists("<feature>mpFaultTolerance-2.0</feature>", 60000, targetServerXML);
      assertTrue("Could not find the updated feature in the target server.xml file", foundUpdate);
      tagLog("##configChangeTest end");
   }

   @Test
   public void configIncludesChangeTest() throws Exception {
      tagLog("##configIncludesChangeTest start");
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
      assertTrue(verifyLogMessageExists(SERVER_UPDATED, 60000));
      verifyFileExists(targetServerXMLIncludes, 11000); // ensure file copy is complete.
      boolean foundUpdate = verifyLogMessageExists("<feature>servlet-4.0</feature>", 60000, targetServerXMLIncludes);
      assertTrue("Could not find the updated feature in the target extraFeatures.xml file", foundUpdate);
      // restore config files
      replaceString("<feature>servlet-4.0</feature>", "<!-- replace -->", srcServerXMLIncludes);

      // check that features have been generated
      assertTrue(verifyLogMessageExists(RUNNING_GENERATE_FEATURES, 10000, ++generateFeaturesCount)); // mojo ran

      // check for server configuration was successfully updated message
      assertTrue(verifyLogMessageExists(SERVER_UPDATED, 60000));
      tagLog("##configIncludesChangeTest end");
   }

   @Test
   public void resourceFileChangeTest() throws Exception {
      tagLog("##resourceFileChangeTest start");
      int appUpdatedCount = countOccurrences(SERVER_CONFIG_SUCCESS, logFile);

      // make a resource file change
      File resourceDir = new File(tempProj, "src/main/resources");
      assertTrue(resourceDir.exists());

      File propertiesFile = new File(resourceDir, "microprofile-config.properties");
      assertTrue(propertiesFile.createNewFile());

      // dev mode copies file to target dir
      File targetPropertiesFile = new File(targetDir, "classes/microprofile-config.properties");
      assertTrue(getLogTail(), verifyFileExists(targetPropertiesFile, 30000)); // wait for dev mode
      assertTrue(getLogTail(), verifyLogMessageExists(SERVER_CONFIG_SUCCESS, 10000, logFile, ++appUpdatedCount));

      // delete a resource file
      assertTrue(propertiesFile.delete());
      assertTrue(getLogTail(), verifyFileDoesNotExist(targetPropertiesFile, 10000));
      tagLog("##resourceFileChangeTest end");
   }

   @Test
   public void testDirectoryTest() throws Exception {
      tagLog("##testDirectoryTest start");
      Thread.sleep(500); // this test often fails, wait for dev mode
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
      tagLog("##testDirectoryTest end");
   }

   @Test
   public void manualTestsInvocationTest() throws Exception {
      tagLog("##manualTestsInvocationTest start");
      writer.write("\n");
      writer.flush();

      assertTrue(verifyLogMessageExists("Unit tests finished.", 10000));
      assertTrue(verifyLogMessageExists("Integration tests finished.", 2000));
      tagLog("##manualTestsInvocationTest end");
   }

   @Test
   public void restartServerTest() throws Exception {
      tagLog("##restartServerTest start");
      int runningGenerateCount = countOccurrences(RUNNING_GENERATE_FEATURES, logFile);
      String RESTARTED = "The server has been restarted.";
      int restartedCount = countOccurrences(RESTARTED, logFile);
      writer.write("r\n"); // command to restart liberty
      writer.flush();

      assertTrue(verifyLogMessageExists(RESTARTED, 20000, ++restartedCount));

      // not supposed to rerun generate features just because of a server restart
      assertTrue(verifyLogMessageExists(RUNNING_GENERATE_FEATURES, 2000, runningGenerateCount));
      tagLog("##restartServerTest end");
   }

    @Test
    public void invalidDependencyTest() throws Exception {
        tagLog("##invalidDependencyTest start");
        // add invalid dependency to pom.xml
        String invalidDepComment = "<!-- <dependency>\n" + "        <groupId>io.openliberty.features</groupId>\n"
                + "        <artifactId>abcd</artifactId>\n" + "        <version>1.0</version>\n"
                + "    </dependency> -->";
        String invalidDep = "<dependency>\n" + "        <groupId>io.openliberty.features</groupId>\n"
                + "        <artifactId>abcd</artifactId>\n" + "        <version>1.0</version>\n" + "    </dependency>";
        replaceString(invalidDepComment, invalidDep, pom);
        assertTrue(verifyLogMessageExists("The POM for io.openliberty.features:abcd:jar:1.0 is missing, no dependency information available", 10000));
        // restore valid pom
        replaceString(invalidDep, invalidDepComment, pom);
        assertTrue(verifyLogMessageExists(SERVER_NOT_UPDATED, 10000));
        tagLog("##invalidDependencyTest end");
    }

   @Ignore // TODO remove when liberty issue 20749 and 1481
   @Test
   public void resolveDependencyTest() throws Exception {
      tagLog("##resolveDependencyTest start");
      // create the HealthCheck class, expect a compilation error
      File systemHealthRes = new File("../resources/SystemHealth.java");
      assertTrue(systemHealthRes.exists());
      File systemHealthSrc = new File(tempProj, "/src/main/java/com/demo/SystemHealth.java");
      File systemHealthTarget = new File(targetDir, "/classes/com/demo/SystemHealth.class");

      FileUtils.copyFile(systemHealthRes, systemHealthSrc);
      assertTrue(systemHealthSrc.exists());

      assertTrue(verifyLogMessageExists(COMPILATION_ERRORS, 200000));
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
      assertTrue(getLogTail(), verifyLogMessageExists(COMPILATION_SUCCESSFUL, 100000));
      assertTrue(getLogTail(), verifyFileExists(systemHealthTarget, 15000));
      tagLog("##resolveDependencyTest end");
   }

   @Test
   public void generateFeatureTest() throws Exception {
      tagLog("##generateFeatureTest start");
      // Verify generate features runs when dev mode first starts
      assertTrue(verifyLogMessageExists(RUNNING_GENERATE_FEATURES, 10000));
      assertFalse(verifyLogMessageExists("batch-1.0", 10000)); // shouldn't be here yet
      int runGenerateFeaturesCount = countOccurrences(RUNNING_GENERATE_FEATURES, logFile);
      int installedFeaturesCount = countOccurrences(SERVER_INSTALLED_FEATURES, logFile);

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
      assertTrue(verifyLogMessageExists(RUNNING_GENERATE_FEATURES, 10000, ++runGenerateFeaturesCount)); // mojo ran
      assertTrue(verifyFileExists(newFeatureFile, 5000)); // mojo created file
      assertTrue(verifyFileExists(newTargetFeatureFile, 5000)); // dev mode copied file
      assertTrue(verifyLogMessageExists("batch-1.0", 10000, newFeatureFile));
      assertTrue(verifyLogMessageExists(NEW_FILE_INFO_MESSAGE, 10000, newFeatureFile));
      assertTrue(verifyLogMessageExists(SERVER_XML_COMMENT, 10000, serverXmlFile));
      // "CWWKF0012I: The server installed the following features:" assume batch-1.0 is in there
      // batch-1.0 pulls in other features that can take a long time to download.
      assertTrue(verifyLogMessageExists(SERVER_INSTALLED_FEATURES, 123000, ++installedFeaturesCount));

      // When there is a compilation error the generate features process should not run
      final String goodCode = "import javax.ws.rs.GET;";
      final String badCode  = "import javax.ws.rs.GET";
      int errCount = countOccurrences(COMPILATION_ERRORS, logFile);
      replaceString(goodCode, badCode, helloBatchSrc);
      assertTrue(getLogTail(), verifyLogMessageExists(COMPILATION_ERRORS, 15000, errCount+1)); // wait for compilation
      int updatedgenFeaturesCount = countOccurrences(RUNNING_GENERATE_FEATURES, logFile);
      // after failed compilation generate features is not run.
      assertEquals(runGenerateFeaturesCount, updatedgenFeaturesCount);

      int goodCount = countOccurrences(COMPILATION_SUCCESSFUL, logFile);
      int regenerateCount = countOccurrences(REGENERATE_FEATURES, logFile);
      replaceString(badCode, goodCode, helloBatchSrc);
      assertTrue(verifyLogMessageExists(COMPILATION_SUCCESSFUL, 10000, goodCount+1));
      // after successful compilation run generate features. "Regenerate" message should appear after.
      assertTrue(verifyLogMessageExists(RUNNING_GENERATE_FEATURES, 10000, logFile, ++runGenerateFeaturesCount));
      assertTrue(verifyLogMessageExists(REGENERATE_FEATURES, 10000, logFile, ++regenerateCount));

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
      // After generate features is toggled off and on we end up with the same features as before
      assertTrue(verifyLogMessageExists(REGENERATE_FEATURES, 10000, logFile, ++regenerateCount));

      // Remove a class and use 'optimize' to rebuild the generated features
      int generateFeaturesCount = countOccurrences(GENERATE_FEATURES, logFile);
      assertTrue(helloBatchSrc.delete());
      assertTrue(verifyFileDoesNotExist(helloBatchSrc, 15000));
      assertTrue(verifyFileDoesNotExist(helloBatchObj, 15000));
      // Just removing the class file does not remove the feature because the feature
      // list is built in an incremental way.

      int serverUpdateCount = countOccurrences(SERVER_UPDATE_COMPLETE, logFile);
      writer.write("o\n"); // on optimize regenerate
      writer.flush();
      assertTrue(verifyLogMessageExists(GENERATE_FEATURES, 10000, logFile, ++generateFeaturesCount));
      assertTrue(verifyLogMessageExists("batch-1.0", 10000, newFeatureFile, 0)); // exist 0 times
      // Check for server response to newly generated feature list.
      assertTrue(verifyLogMessageExists(SERVER_UPDATE_COMPLETE, 10000, serverUpdateCount+1));
      // Need to ensure server finished updating before the next test starts.
      tagLog("##generateFeatureTest end");
   }

   @Test
   public void scannerInvalidEETest() throws Exception {
      tagLog("##scannerInvalidEETest start");
      String placeholder1 = "<!-- Umbrella dependency replace 1 -->";
      String badDep = "<dependency>\n" +
         "        <groupId>jakarta.platform</groupId>\n" +
         "        <artifactId>jakarta.jakartaee-api</artifactId>\n" +
         "        <version>99.0.0</version>\n" +
         "        <scope>provided</scope>\n" +
         "    </dependency>\n";

      try {
         int msgCount = countOccurrences(INVALID_EE_VERSION_MSG, logFile);
         replaceString(placeholder1, badDep, pom);
         assertTrue(verifyLogMessageExists(INVALID_EE_VERSION_MSG, 9000, logFile, ++msgCount));
      } finally {
         // restore pom
         replaceString(badDep, placeholder1, pom);
      }
      int systemUpdateCount = countOccurrences(SERVER_CONFIG_SUCCESS, logFile);
      replaceString(badDep, placeholder1, pom);
      assertTrue(verifyLogMessageExists(SERVER_CONFIG_SUCCESS, 9000, logFile, ++systemUpdateCount));

      tagLog("##scannerInvalidEETest end");
   }

   @Test
   public void scannerInvalidMPTest() throws Exception {
      tagLog("##scannerInvalidMPTest start");
      String placeholder2 = "<!-- Umbrella dependency replace 2 -->";
      String badDep = "<dependency>\n" +
         "        <groupId>org.eclipse.microprofile</groupId>\n" +
         "        <artifactId>microprofile</artifactId>\n" +
         "        <version>99.0</version>\n" +
         "        <scope>provided</scope>\n" +
         "        <type>pom</type>\n" +
         "    </dependency>\n";

      try {
         int msgCount = countOccurrences(INVALID_MP_VERSION_MSG, logFile);
         replaceString(placeholder2, badDep, pom);
         assertTrue(verifyLogMessageExists(INVALID_MP_VERSION_MSG, 9000, logFile, ++msgCount));
      } finally {
         // restore pom
         replaceString(badDep, placeholder2, pom);
      }
      int systemUpdateCount = countOccurrences(SERVER_CONFIG_SUCCESS, logFile);
      replaceString(badDep, placeholder2, pom);
      assertTrue(verifyLogMessageExists(SERVER_CONFIG_SUCCESS, 9000, logFile, ++systemUpdateCount));

      tagLog("##scannerInvalidMPTest end");
   }

}
