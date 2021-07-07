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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.maven.shared.utils.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class MultiModuleEjbTest extends BaseMultiModuleTest {

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      setUpMultiModule("sample.ejb", "ejb-ear", null);
      run();
   }

   @Test
   public void runTest() throws Exception {
      assertEndpointContent("http://localhost:9080/ejb-war/ejbservlet", "Hello EJB World.");

      modifyEjb();

      assertEndpointContent("http://localhost:9080/ejb-war/ejbservlet", "Hello EJB World 1.");
   }

   private void modifyEjb() throws Exception {
      // modify a java file
      File srcClass = new File(tempProj, "ejb-ejb/src/main/java/wasdev/ejb/ejb/SampleStatelessBean.java");
      File targetClass = new File(tempProj, "ejb-ejb/target/classes/wasdev/ejb/ejb/SampleStatelessBean.class");
      assertTrue(srcClass.exists());
      assertTrue(targetClass.exists());

      long lastModified = targetClass.lastModified();
      replaceString("Hello EJB World.", "Hello EJB World 1.", srcClass);

      Thread.sleep(5000); // wait for compilation
      boolean wasModified = targetClass.lastModified() > lastModified;
      assertTrue(wasModified);
   }

}

