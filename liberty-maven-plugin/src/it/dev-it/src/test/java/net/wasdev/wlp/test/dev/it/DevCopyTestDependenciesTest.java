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

public class DevCopyTestDependenciesTest extends BaseDevTest {

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      setUpBeforeClass(null, "../resources/basic-dev-project", true, false, null, null);

      String additionalDependencies = "<dependency> <groupId>org.postgresql</groupId> <artifactId>postgresql</artifactId> <version>42.1.1</version> <scope>test</scope> </dependency>";
      replaceString("<!-- ADDITIONAL_DEPENDENCIES -->", additionalDependencies, pom);

      String additionalConfiguration = "<copyDependencies> <dependency> <groupId>org.postgresql</groupId> <artifactId>postgresql</artifactId> </dependency> </copyDependencies>";
      replaceString("<!-- ADDITIONAL_CONFIGURATION -->", additionalConfiguration, pom);

      // add new parameter in first argument to skip install features on restart
      // in this case, it should not skip install feature because Liberty was not previously installed
      startProcess("-DskipInstallFeature=true", true);
   }

   @AfterClass
   public static void cleanUpAfterClass() throws Exception {
      BaseDevTest.cleanUpAfterClass();
   }

   @Test
   public void copyDependenciesTest() throws Exception {
      // Test scoped dependency should be omitted
      assertTrue("The test scoped dependency was copied unexpectedly: "+getLogTail(), verifyLogMessageExists("copyDependencies failed for dependency with groupId org.postgresql, artifactId postgresql and type jar.", 2000));

      File f = new File(targetDir, "liberty/wlp/usr/servers/defaultServer/lib/global/postgresql-42.1.1.jar");
      assertFalse(f.exists());

      assertTrue("The install-feature goal did not run: "+getLogTail(), verifyLogMessageExists("Running liberty:install-feature", 2000));
      assertFalse("The skip install-feature log message was found unexpectedly: "+getLogTail(), verifyLogMessageExists("Skipping liberty:install-feature", 2000));

   }

}
