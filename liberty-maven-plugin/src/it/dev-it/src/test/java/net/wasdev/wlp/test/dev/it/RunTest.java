/*******************************************************************************
 * (c) Copyright IBM Corporation 2019, 2020.
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import org.apache.maven.shared.utils.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class RunTest extends BaseDevTest {

   private static String URL;

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      URL = "http://localhost:9080/dev-sample-proj-1.0-SNAPSHOT/servlet";
      setUpBeforeClass(null, false);
   }

   @Test
   public void endpointTest() throws Exception {
         HttpClient client = new HttpClient();

        GetMethod method = new GetMethod(URL);
         try {
            int statusCode = client.executeMethod(method);

            assertEquals("HTTP GET failed", HttpStatus.SC_OK, statusCode);

            String response = method.getResponseBodyAsString();

            assertTrue("Unexpected response body", response.contains("hello world"));
            // Note that checkLogMessage returns true if it does NOT find the message.
            // So I am verifying that SLF4J loaded properly.
            assertTrue(checkLogMessage(2000, "SLF4J: Failed to load class"));
            // And then verify the logging message was successfully logged.
            assertFalse(checkLogMessage(2000, "SLF4J Logger is ready for messages."));
         } finally {
            method.releaseConnection();
         }
   }

   @AfterClass
   public static void cleanUpAfterClass() throws Exception {
      BaseDevTest.cleanUpAfterClass(false);
   }
}

