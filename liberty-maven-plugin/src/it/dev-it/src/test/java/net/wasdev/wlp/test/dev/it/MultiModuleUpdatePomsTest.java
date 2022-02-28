/*******************************************************************************
 * (c) Copyright IBM Corporation 2022.
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

import org.apache.maven.shared.utils.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class MultiModuleUpdatePomsTest extends BaseMultiModuleTest {

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      setUpMultiModule("typeA", "ear", null);
      run();
   }

   /**
    * Test that saving a pom twice in a row only causes things to recompile once.
    * Test updating upstream poms (jar, war) and main pom (ear).
    */
   @Test
   public void updatePomsTest() throws Exception {
      // Wait until the last module (ear) finishes compiling during dev mode startup
      // TODO: this can be removed if dev mode does not recompile after first starting up
      verifyLogMessageExists("guide-maven-multimodules-ear tests compilation was successful", 10000);

      int jarSourceCount = countOccurrences("guide-maven-multimodules-jar source compilation was successful.", logFile);
      int jarTestsCount = countOccurrences("guide-maven-multimodules-jar tests compilation was successful.", logFile);
      int warSourceCount = countOccurrences("guide-maven-multimodules-war source compilation was successful.", logFile);
      int warTestsCount = countOccurrences("guide-maven-multimodules-war tests compilation was successful.", logFile);
      int earTestsCount = countOccurrences("guide-maven-multimodules-ear tests compilation was successful.", logFile);

      // verify that generated-features.xml file exists
      File newFeatureFile = getGeneratedFeaturesFile("ear");
      assertTrue(getLogTail(), verifyFileExists(newFeatureFile, 1000));
      long newFeatureFileLastModified = newFeatureFile.lastModified();
      waitLongEnough();

      touchFileTwice("jar/pom.xml");

      // Give time for recompile to occur
      Thread.sleep(3000);

      // count exact number of messages
      assertEquals(getLogTail(), ++jarSourceCount,
            countOccurrences("guide-maven-multimodules-jar source compilation was successful.", logFile));
      assertEquals(getLogTail(), ++jarTestsCount,
            countOccurrences("guide-maven-multimodules-jar tests compilation was successful.", logFile));
      assertEquals(getLogTail(), ++warSourceCount,
            countOccurrences("guide-maven-multimodules-war source compilation was successful.", logFile));
      assertEquals(getLogTail(), ++warTestsCount,
            countOccurrences("guide-maven-multimodules-war tests compilation was successful.", logFile));
      assertEquals(getLogTail(), ++earTestsCount,
            countOccurrences("guide-maven-multimodules-ear tests compilation was successful.", logFile));

      // verify that feature generation ran
      assertTrue(getLogTail(), waitForCompilation(newFeatureFile, newFeatureFileLastModified, 1000));
      newFeatureFileLastModified = newFeatureFile.lastModified();
      waitLongEnough();

      touchFileTwice("war/pom.xml");

      // Give time for recompile to occur
      Thread.sleep(5000);

      // count exact number of messages
      // only war and ear should have had recompiles
      assertEquals(getLogTail(), jarSourceCount,
            countOccurrences("guide-maven-multimodules-jar source compilation was successful.", logFile));
      assertEquals(getLogTail(), jarTestsCount,
            countOccurrences("guide-maven-multimodules-jar tests compilation was successful.", logFile));
      assertEquals(getLogTail(), ++warSourceCount,
            countOccurrences("guide-maven-multimodules-war source compilation was successful.", logFile));
      assertEquals(getLogTail(), ++warTestsCount,
            countOccurrences("guide-maven-multimodules-war tests compilation was successful.", logFile));
      assertEquals(getLogTail(), ++earTestsCount,
            countOccurrences("guide-maven-multimodules-ear tests compilation was successful.", logFile));

      // verify that feature generation ran
      assertTrue(getLogTail(), waitForCompilation(newFeatureFile, newFeatureFileLastModified, 1000));
      newFeatureFileLastModified = newFeatureFile.lastModified();
      waitLongEnough();

      touchFileTwice("ear/pom.xml");

      // Give time for recompile to occur
      Thread.sleep(3000);

      // count exact number of messages
      // only ear should have had recompiles
      assertEquals(getLogTail(), jarSourceCount,
            countOccurrences("guide-maven-multimodules-jar source compilation was successful.", logFile));
      assertEquals(getLogTail(), jarTestsCount,
            countOccurrences("guide-maven-multimodules-jar tests compilation was successful.", logFile));
      assertEquals(getLogTail(), warSourceCount,
            countOccurrences("guide-maven-multimodules-war source compilation was successful.", logFile));
      assertEquals(getLogTail(), warTestsCount,
            countOccurrences("guide-maven-multimodules-war tests compilation was successful.", logFile));
      assertEquals(getLogTail(), ++earTestsCount,
            countOccurrences("guide-maven-multimodules-ear tests compilation was successful.", logFile));

      // verify that feature generation did not run since there are no source class
      // files for the ear module
      assertEquals("generated-features.xml was modified\n" + getLogTail(), newFeatureFileLastModified, newFeatureFile.lastModified());
   }

private void touchFileTwice(String path) throws InterruptedException {
      File file = new File(tempProj, path);
      long time = System.currentTimeMillis();
      assertTrue(file.setLastModified(time));
      Thread.sleep(40);
      assertTrue(file.setLastModified(time+40));
}


}

