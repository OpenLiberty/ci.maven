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
      startProcess("-Dcontainer", true);
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

}
