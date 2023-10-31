
/*******************************************************************************
 * (c) Copyright IBM Corporation 2017.
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
 package net.wasdev.wlp.maven.test.it;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.junit.Test;

import static org.junit.Assert.*;

public class InstallRarIT {
    
    public static final String LOG_LOCATION = "target/liberty/wlp/usr/servers/jee7sampleServer/logs/messages.log";
    public final File MESSSAGES_LOG = new File("target/liberty/wlp/usr/servers/jee7sampleServer/logs/messages.log");
    
    @Test
    public void testAdapterInstall() throws Exception {
        String message = "J2CA7001I: Resource adapter helloworld-ear.helloworld-ra installed";
        assertTrue("Expecting message [" + message + "] in server message log but not found.", 
                logContainsMessage(message));      
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
