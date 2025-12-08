/*******************************************************************************
 * (c) Copyright IBM Corporation 2025.
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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ToolchainRunServerWithServerEnvJdkTest extends BaseToolchainTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        setUpBeforeClass(null, "../resources/basic-toolchain-project", null, null, "run",
                () -> {
                    String javaHome = System.getenv("JAVA_HOME");
                    try {
                        File libertyConfigDir = Paths.get(tempProj.getPath(), "src", "main", "liberty", "config").toAbsolutePath().toFile();
                        String content="JAVA_HOME=" + javaHome;
                        Files.write(new File(libertyConfigDir, "server.env").toPath(), content.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

   @AfterClass
   public static void cleanUpAfterClass() throws Exception {
      BaseToolchainTest.cleanUpAfterClass(false, false);
   }

   @Test
   public void runServerTest() throws Exception {
      tagLog("##runServerTest start");
      assertTrue(getLogTail(), verifyLogMessageExists(TOOLCHAIN_INITIALIZED, 10000));
      assertTrue(getLogTail(), verifyLogMessageExists(String.format(TOOLCHAIN_CONFIGURED_FOR_GOAL, "create"), 10000));
      assertTrue(getLogTail(), verifyLogMessageExists(String.format(JAVA_HOME_CONFIGURED, "run"), 10000));

      tagLog("##runServerTest end");
   }
}
