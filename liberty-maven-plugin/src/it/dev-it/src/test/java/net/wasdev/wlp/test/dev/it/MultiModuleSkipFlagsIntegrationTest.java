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
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import io.openliberty.tools.maven.server.DevMojo;

public class MultiModuleSkipFlagsIntegrationTest extends BaseMultiModuleTest {

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      // nothing
   }

   @AfterClass
   public static void cleanUpAfterClass() throws Exception {
      // nothing
   }

   @Before
   public void setUp() throws Exception {
      setUpMultiModule("skipTests", "ear", null);
   }

   @After
   public void cleanUp() throws Exception {
      BaseDevTest.cleanUpAfterClass(true);
   }

   @Test
   public void overridePomConfigTest() throws Exception {
      // set default config in parent to skip
      modifyFileForModule("pom.xml", "<!-- PARENT_CONFIG_SKIP_TESTS -->", "<configuration><skipTests>true</skipTests></configuration>");

      // override war config to not skip
      modifyFileForModule("war/pom.xml", "<!-- WAR_CONFIG_SKIP_TESTS -->", "<configuration><skipTests>false</skipTests></configuration>");
      
      // override jar property to not skip -- should skip anyways because parent config takes predecence
      modifyFileForModule("jar/pom.xml", "<!-- JAR_PROPS_SKIP_TESTS -->", "<skipTests>false</skipTests>");

      // override ear config to not skip
      modifyFileForModule("ear/pom.xml", "<!-- EAR_CONFIG_SKIP_TESTS -->", "<configuration><skipTests>false</skipTests></configuration>");
      // the ear config above should override this -- so should not skip
      modifyFileForModule("ear/pom.xml", "<!-- EAR_PROPS_SKIP_TESTS -->", "<skipTests>true</skipTests>");

      run("-DhotTests=true");

      verifyTestsRan("guide-maven-multimodules-war", "guide-maven-multimodules-ear");
      verifyTestsDidNotRun("guide-maven-multimodules-jar");
   }

   @Test
   public void overridePropertyTest() throws Exception {
      // set default prop in parent to skip
      modifyFileForModule("pom.xml", "<!-- PARENT_PROPS_SKIP_TESTS -->", "<skipTests>true</skipTests>");

      // override war config to not skip -- takes precedence over parent property, so should not skip.
      // Although failsafe/surefire could pick up the property and skip itself
      modifyFileForModule("war/pom.xml", "<!-- WAR_CONFIG_SKIP_TESTS -->", "<configuration><skipTests>false</skipTests></configuration>");
       
      // override jar property to not skip -- takes precedence over parent property, so should not skip
      modifyFileForModule("jar/pom.xml", "<!-- JAR_PROPS_SKIP_TESTS -->", "<skipTests>false</skipTests>");
 
      // override ear config to skip
      modifyFileForModule("ear/pom.xml", "<!-- EAR_CONFIG_SKIP_TESTS -->", "<configuration><skipTests>true</skipTests></configuration>");
      // the ear config above should override this -- so should skip
      modifyFileForModule("ear/pom.xml", "<!-- EAR_PROPS_SKIP_TESTS -->", "<skipTests>false</skipTests>");
  
      run("-DhotTests=true");
 
      verifyTestsRan("guide-maven-multimodules-war", "guide-maven-multimodules-jar");
      verifyTestsDidNotRun("guide-maven-multimodules-ear");
   }

   @Test
   public void overrideUserPropertyTest() throws Exception {
      // set default prop in parent, but user property takes precedence, so default should still be skip
      modifyFileForModule("pom.xml", "<!-- PARENT_PROPS_SKIP_TESTS -->", "<skipTests>false</skipTests>");

      // override war config to not skip -- takes precedence over user property, so should not skip.
      // Although failsafe/surefire could pick up the property and skip itself
      modifyFileForModule("war/pom.xml", "<!-- WAR_CONFIG_SKIP_TESTS -->", "<configuration><skipTests>false</skipTests></configuration>");
       
      // override jar property to not skip -- but user property takes precedence, so should skip
      modifyFileForModule("jar/pom.xml", "<!-- JAR_PROPS_SKIP_TESTS -->", "<skipTests>false</skipTests>");
 
      // override ear property to not skip -- but user property takes precedence, so should skip
      modifyFileForModule("ear/pom.xml", "<!-- EAR_PROPS_SKIP_TESTS -->", "<skipTests>false</skipTests>");
  
      // set user property
      run("-DhotTests=true -DskipTests");
 
      verifyTestsRan("guide-maven-multimodules-war");
      verifyTestsDidNotRun("guide-maven-multimodules-jar", "guide-maven-multimodules-ear");
   }

}

