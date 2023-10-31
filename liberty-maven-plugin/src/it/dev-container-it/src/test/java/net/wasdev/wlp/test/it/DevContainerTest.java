package net.wasdev.wlp.test.it;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class DevContainerTest extends BaseDevTest {

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      setUpBeforeClass(null, "../resources/container-test-project", true, false, null, null);

      // run dev mode with container flag
      startProcess("-Dcontainer -Dliberty.dev.podman=true -DcontainerBuildTimeout=599 -DdockerBuildTimeout=601", true);
   }

   @AfterClass
   public static void cleanUpAfterClass() throws Exception {
      BaseDevTest.cleanUpAfterClass();
   }

   @Test
   public void runContainerTest() throws Exception {
      // Verify that container was built correctly
      assertTrue("The container build did not complete: "+getLogTail(), verifyLogMessageExists("Completed building container image.", 2000));
      assertTrue("The application start message is missing: "+getLogTail(), verifyLogMessageExists("CWWKZ0001I: Application rest started", 2000));
   }

   @Test 
   public void devcMetaDataTest() throws Exception {
      File devcMetaDataFile = new File("../resources/container-test-project/target/defaultServer-liberty-devc-metadata.xml");

      assertTrue("The devc metadata file does not exist.", devcMetaDataFile.exists());

      String content = FileUtils.readFileToString(devcMetaDataFile, "UTF-8");

      assertTrue("Container name is incorrect in devc metadata XML.", content.contains("<containerName>liberty-dev</containerName"));
      assertTrue("Container alive status is incorrect in devc metadata XML.", content.contains("<containerAlive>true</containerAlive>"));
      assertTrue("Container build timeout is incorrect in devc metadata XML.", content.contains("<containerBuildTimeout>599</containerBuildTimeout>"));
      assertTrue("Container engine is incorrect in devc metadata XML.", content.contains("<containerEngine>podman</containerEngine>"));
      assertTrue("Container run options are incorrect in devc metadata XML.", content.contains("<containerRunOpts></containerRunOpts>"));
      assertTrue("Container image name is incorrect in devc metadata XML.", content.contains("<imageName>dev-containers-it-dev-mode</imageName>"));
   }
}
