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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.maven.shared.utils.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class BaseMultiModuleTest extends BaseDevTest {

   public static void setUpMultiModule(String testcaseFolder, String libertyModule, String pomModule) throws Exception {
      setUpBeforeClass(null, "../resources/multi-module-projects/" + testcaseFolder, true, false, libertyModule, pomModule);

      optionalReplaceVersion(tempProj);
      optionalReplaceVersion(new File(tempProj, "pom"));
      optionalReplaceVersion(new File(tempProj, "ear"));
      optionalReplaceVersion(new File(tempProj, "war"));
      optionalReplaceVersion(new File(tempProj, "war2"));
   }

   protected static void run() throws Exception {
      run(true);
   }

   protected static void run(boolean devMode) throws Exception {
      startProcess(null, devMode, "mvn io.openliberty.tools:liberty-maven-plugin:"+System.getProperty("mavenPluginVersion")+":");
   }

   private static void replaceVersion(File dir) throws IOException {
      File pom = new File(dir, "pom.xml");
      String pluginVersion = System.getProperty("mavenPluginVersion");
      replaceString("SUB_VERSION", pluginVersion, pom);
      String runtimeVersion = System.getProperty("runtimeVersion");
      replaceString("RUNTIME_VERSION", runtimeVersion, pom);
   }

   private static void optionalReplaceVersion(File pom) {
      try {
         replaceVersion(pom);
      } catch (IOException e) {
         // ignore failures
      }
   }

   public void manualTestsInvocationTest(String... moduleArtifactIds) throws Exception {
      assertTrue(getLogTail(), verifyLogMessageExists("To run tests on demand, press Enter.", 30000));

      writer.write("\n");
      writer.flush();

      for (String moduleArtifactId : moduleArtifactIds) {
         if (!moduleArtifactId.endsWith("ear")) {
            assertTrue(getLogTail(), verifyLogMessageExists("Unit tests for " + moduleArtifactId + " finished.", 10000));
         }
         assertTrue(getLogTail(), verifyLogMessageExists("Integration tests for " + moduleArtifactId + " finished.", 10000));
  
      }

      assertFalse("Found CWWKM2179W message indicating incorrect app deployment. " + getLogTail(), verifyLogMessageExists("CWWKM2179W", 2000));
   }

   public void assertEndpointContent(String url, String assertResponseContains) throws IOException, HttpException {
      HttpClient client = new HttpClient();

      GetMethod method = new GetMethod(url);
      try {
         int statusCode = client.executeMethod(method);

         assertEquals("HTTP GET failed. " + getLogTail(), HttpStatus.SC_OK, statusCode);

         String response = method.getResponseBodyAsString();

         assertTrue("Unexpected response body: " + response + ". " + getLogTail(), response.contains(assertResponseContains));
      } finally {
         method.releaseConnection();
      }
   }

   protected static void modifyJarClass() throws IOException, InterruptedException {
      // modify a java file
      File srcClass = new File(tempProj, "jar/src/main/java/io/openliberty/guides/multimodules/lib/Converter.java");
      File targetClass = new File(tempProj, "jar/target/classes/io/openliberty/guides/multimodules/lib/Converter.class");
      assertTrue(srcClass.exists());
      assertTrue(targetClass.exists());

      long lastModified = targetClass.lastModified();
      replaceString("return feet;", "return feet*2;", srcClass);

      Thread.sleep(5000); // wait for compilation
      boolean wasModified = targetClass.lastModified() > lastModified;
      assertTrue(wasModified);
   }

   protected void testEndpointsAndUpstreamRecompile() throws Exception {
      // check main endpoint
      assertEndpointContent("http://localhost:9080/converter", "Height Converter");

      // invoke upstream java code
      assertEndpointContent("http://localhost:9080/converter/heights.jsp?heightCm=3048", "100");

      // test modify a Java file in an upstream module
      modifyJarClass();
      assertEndpointContent("http://localhost:9080/converter/heights.jsp?heightCm=3048", "200");
   }

   @AfterClass
   public static void cleanUpAfterClass() throws Exception {
      BaseDevTest.cleanUpAfterClass();
   }
}

