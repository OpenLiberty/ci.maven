/**
 * (C) Copyright IBM Corporation 2022,2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package application;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import org.junit.Test;

public class InstallSpringBoot20AppIT {
    public static final String TOOLCHAIN_CONFIGURED_FOR_GOAL = "CWWKM4101I: The %s goal is using the configured toolchain JDK located at";
    public static final String LOG_LOCATION = "liberty/usr/servers/test/logs/messages.log";

    @Test
    public void testThinApplicationExistsInAppsDirectory() throws Exception {

        File f = new File("target/liberty/wlp/usr/servers/test/apps/thin-springboot-appsdirectory-apps-it-1.0.0.Final-exec.jar");
        assertTrue(f.getCanonicalFile() + " doesn't exist. Plugin failed to place the file at right destination.", f.exists());
    }

    @Test
    public void testLibIndexCacheExists() throws Exception {
        File f = new File("target/liberty/wlp/usr/shared/resources/lib.index.cache");
        assertTrue(f.getCanonicalFile()+ " doesn't exist. Plugin failed to place the cache directory at right destination.", f.exists());
    }

    @Test
    public void testToolchainLogs() throws Exception {
        File buildLog = new File("../build.log");
        assertTrue(buildLog.exists());

        assertTrue("Did not find toolchain honored message for create goal in build.log", logContainsMessage(buildLog, String.format(TOOLCHAIN_CONFIGURED_FOR_GOAL, "create")));
        assertTrue("Did not find toolchain honored message for package goal in build.log", logContainsMessage(buildLog, String.format(TOOLCHAIN_CONFIGURED_FOR_GOAL, "package")));
        assertTrue("Did not find toolchain honored message for test-start goal in build.log", logContainsMessage(buildLog, String.format(TOOLCHAIN_CONFIGURED_FOR_GOAL, "test-start")));
        assertTrue("Did not find toolchain honored message for test-stop goal in build.log", logContainsMessage(buildLog, String.format(TOOLCHAIN_CONFIGURED_FOR_GOAL, "test-stop")));
        File logFile = new File(LOG_LOCATION);
        assertTrue("Log file not found at location: "+LOG_LOCATION, logFile.exists());
        // should contain java.version = 11 since <jdkToolChain> is defined as Java 11
        assertTrue("Did not find toolchain version in messages.log", logContainsMessage(logFile,  "java.version = 11"));

    }

    public boolean logContainsMessage(File logFile, String message) throws FileNotFoundException {

        boolean found = false;

        try (Scanner scanner = new Scanner(logFile);) {
            while (scanner.hasNextLine()) {
                if(scanner.nextLine().contains(message)) {
                    found = true;
                }
            }
        }

        return found;
    }
}
