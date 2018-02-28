/**
 * (C) Copyright IBM Corporation 2017, 2018.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Test;

/**
 * Only included in the verify-main-app Maven profile.
 * 
 * @author ctianus.ibm.com
 *
 */
public class VerifyMainAppIT {

    @Test
    public void testVerifyAppInArquillianXml() throws Exception {
        // Make sure that the verifyApps property is properly written to
        // arquillian.xml

        File arquillianXML = new File("target/test-classes/arquillian.xml");
        InputStream is = new FileInputStream(arquillianXML);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = br.readLine()) != null) {
            if (line.contains("<property name=\"verifyApps\">test-configure-arquillian</property>")) {
                return;
            }
        }

        assertTrue(false); // Should have returned in the while loop if the test
        // passed
    }

    @Test
    public void testVerifyMainApp() throws Exception {
        // This test verifies that the main app should start before the
        // Arquillian test application by looking at the build.log console
        // output.

        String mainAppOutput = "[AUDIT   ] CWWKT0016I: Web application available (default_host): http://localhost:9080/myLibertyApp/";
        String testAppOutput = // The test app name is randomly generated, so we
                               // don't know it here. Instead just make sure
                               // that another app is started after the main
                               // app.
                "[AUDIT   ] CWWKT0016I: Web application available (default_host): http://localhost:9080/";

        boolean foundMainApp = false;

        File buildLog = new File("build.log");
        InputStream is = new FileInputStream(buildLog);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        String line;
        while ((line = br.readLine()) != null) {
            if (!foundMainApp) {
                if (line.contains(mainAppOutput)) {
                    foundMainApp = true;
                }
            } else if (line.contains(testAppOutput)) {
                return;
            }
        }

        assertTrue(false); // Should have returned in the while loop if the test
                           // passed
    }

}
