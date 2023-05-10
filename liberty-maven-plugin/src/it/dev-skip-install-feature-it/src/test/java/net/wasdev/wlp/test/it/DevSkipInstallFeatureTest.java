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

public class DevSkipInstallFeatureTest extends BaseDevTest {

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      setUpBeforeClass(null, "../resources/basic-dev-project", true, false, null, null);

      File f = new File("liberty/wlp");
      assertTrue(f.getCanonicalFile() + " exists", f.exists());

      // add new parameter in first argument to skip install features on restart
      // in this case, we skip install feature because Liberty was already installed
      startProcess("-DskipInstallFeature=true", true);
   }

   @AfterClass
   public static void cleanUpAfterClass() throws Exception {
      BaseDevTest.cleanUpAfterClass();
   }

   @Test
   public void installFeatureTest() throws Exception {
      // Verify that currently install-feature is called
      assertFalse("The install-feature goal ran unexpectedly: "+getLogTail(), verifyLogMessageExists("Running liberty:install-feature", 2000));
      assertTrue("The skip install-feature log message is missing: "+getLogTail(), verifyLogMessageExists("Skipping liberty:install-feature", 2000));

   }

}
