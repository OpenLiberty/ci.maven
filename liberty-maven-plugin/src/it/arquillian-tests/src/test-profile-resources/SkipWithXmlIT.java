/**
 * (C) Copyright IBM Corporation 2017, 2021.
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

import io.openliberty.tools.common.arquillian.util.Constants;

/**
 * Only included in the skip-with-xml Maven profile.
 * 
 * @author ctianus.ibm.com
 *
 */
public class SkipWithXmlIT {
    public static final String LOG_LOCATION = "target/test-classes/arquillian.xml";

    @Test
    public void testSkipWithXML() throws Exception {
        // This test has the skip flag true with the XML file already existing.
        // In this case, the server should use the arquillian.xml that is in
        // src/test/resources/arquillian.xml.
 
        assertFalse("Message unexpectedly found in arquillian.xml", logContainsMessage(Constants.CONFIGURE_ARQUILLIAN_COMMENT));
    }

    public boolean logContainsMessage(String message) throws FileNotFoundException {
        File logFile = new File(LOG_LOCATION);
        assertTrue("Log file not found at location: "+LOG_LOCATION, logFile.exists());
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
