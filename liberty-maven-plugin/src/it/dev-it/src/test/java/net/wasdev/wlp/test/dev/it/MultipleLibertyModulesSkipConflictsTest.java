/*******************************************************************************
 * (c) Copyright IBM Corporation 2024.
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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class MultipleLibertyModulesSkipConflictsTest extends BaseMultiModuleTest {

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      setUpMultiModule("multipleLibertyModules-skip-conflicts", null /* this param is not used in this scenario */, null);
   }

   /**
    * The multipleLibertyModules-skip-conflicts project has the following module chains:
    * 
    * jar->war->ear
    * jar->war->ear2
    * jar2
    * 
    * The jar2 module should be skipped implicitly when calling dev mode since dev mode cannot run in a jar project. 
    * The ear2 module should also be skipped bacause skip is explicitly set in the plugin configuration.
    * @throws Exception
    */
   @Test
   public void MultipleLibertyModulesSkipConflictsTest() throws Exception {
	   String mavenPluginCommand = "mvn io.openliberty.tools:liberty-maven-plugin:"+System.getProperty("mavenPluginVersion")+":dev";

       StringBuilder command = new StringBuilder(mavenPluginCommand);
       ProcessBuilder builder = buildProcess(command.toString());

       builder.redirectOutput(logFile);
       builder.redirectError(logFile);
       process = builder.start();
       assertTrue(process.isAlive());

       OutputStream stdin = process.getOutputStream();

       writer = new BufferedWriter(new OutputStreamWriter(stdin));
	      
	   // Make sure we are skipping the jar2 project
	   assertTrue(getLogTail(), verifyLogMessageExists("Skipping module guide-maven-multimodules-jar2 which is not included in this invocation of dev mode", 30000));
	   
       // Make sure neither the ear2 or jar2 projects cause conflicts
       assertTrue(getLogTail(), verifyLogMessageDoesNotExist("Found multiple independent modules in the Reactor build order", 30000));
   }
}

