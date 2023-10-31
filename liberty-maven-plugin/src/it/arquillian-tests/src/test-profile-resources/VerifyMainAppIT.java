/**
 * (C) Copyright IBM Corporation 2017, 2023.
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

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.junit.Test;

/**
 * Only included in the verify-main-app Maven profile.
 * 
 * @author ctianus.ibm.com
 *
 */
public class VerifyMainAppIT {
    public static final String ARQUILLIAN_XML_LOCATION = "target/test-classes/arquillian.xml";

    @Test
    public void testVerifyAppInArquillianXml() throws Exception {
        // Make sure that the verifyApps property is properly written to
        // arquillian.xml
        assertTrue("Did not find expected property in arquillian.xml", fileContainsMessage("<property name=\"verifyApps\">test-configure-arquillian</property>", ARQUILLIAN_XML_LOCATION)); 
    }

    @Test
    public void testVerifyMainApp() throws Exception {
        // This test verifies that the main app should start before the
        // Arquillian test application by looking at the build.log console
        // output.

        String mainAppOutput = "[AUDIT   ] CWWKT0016I: Web application available (default_host): http://localhost:9080/test-configure-arquillian/";
        String testAppOutput = "[AUDIT   ] CWWKT0016I: Web application available (default_host): http://localhost:9080/test/";

        boolean foundMainApp = false;

        File buildLog = new File("build.log");
        assertTrue("build.log does not exist", buildLog.exists());

        try (Scanner scanner = new Scanner(buildLog);) {
           while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
                if (!foundMainApp) {
                    if (line.contains(mainAppOutput)) {
                        foundMainApp = true;
                    }
                } else if (line.contains(testAppOutput)) {
                    return;
                }
            }
        }

        assertTrue("Did not find expected test app message in build.log", false); // Should have returned in the while loop if the test passed
    }

    public boolean fileContainsMessage(String message, String fileToCheck) throws FileNotFoundException {
        File logFile = new File(fileToCheck);
        assertTrue("File not found at location: "+fileToCheck, logFile.exists());
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
