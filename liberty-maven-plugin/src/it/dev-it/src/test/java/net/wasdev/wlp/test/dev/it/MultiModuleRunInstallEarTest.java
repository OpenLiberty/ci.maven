/*******************************************************************************
 * (c) Copyright IBM Corporation 2021.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import org.apache.maven.shared.utils.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class MultiModuleRunInstallEarTest extends BaseMultiModuleTest {

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      setUpMultiModule("typeH", "pom", "pom");
      run(false);
   }

   /**
    * setUpBeforeClass will install an empty EAR file to m2, or a real EAR file may already exist there from a previous run.
    * Then in this test case, install a real EAR file, and verify that running liberty:run again will not overwrite the real EAR file that is in m2.
    */
   @Test
   public void notOverwriteExistingM2Test() throws Exception {
      // Check that the file was already created (from the run() during setup)
      File ear = new File(System.getProperty("user.home"), ".m2/repository/io/openliberty/guides/guide-maven-multimodules-ear/1.0-SNAPSHOT/guide-maven-multimodules-ear-1.0-SNAPSHOT.ear");
      assertTrue(ear.exists());
      long lastModified = ear.lastModified();
      waitLongEnough();

      // Install the real EAR file through Maven command
      runMvnInstallEar();

      // Check file is updated
      long newModified = ear.lastModified();
      assertNotEquals(lastModified, newModified);
      waitLongEnough();

      // Cleanup (stop Liberty)
      cleanUpAfterClass(false);

      // Setup and run Liberty again
      setUpBeforeClass();

      // Check file is not overwritten by liberty:run
      long newModified2 = ear.lastModified();
      assertEquals(newModified2, newModified);
   }

   private static void runMvnInstallEar() throws Exception {
      StringBuilder command = new StringBuilder("mvn install -pl ../ear -am");
      ProcessBuilder builder = buildProcess(command.toString());

      builder.redirectOutput(logFile);
      builder.redirectError(logFile);
      builder.directory(new File(tempProj, "pom"));

      Process mvnInstallProcess = builder.start();
      assertTrue(mvnInstallProcess.isAlive());
      mvnInstallProcess.waitFor(120, TimeUnit.SECONDS);
      assertEquals(0, mvnInstallProcess.exitValue());
   }

   @AfterClass
   public static void cleanUpAfterClass() throws Exception {
      BaseDevTest.cleanUpAfterClass(false);
   }

}

