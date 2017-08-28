
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

import static junit.framework.Assert.*;

public class InstallRarIT {
    
    public final File CONSOLE_LOG = new File("target/liberty/wlp/usr/servers/jee7sampleServer/logs/console.log");
    
    @Test
    public void testAdapterInstall() throws Exception {
        String message = "J2CA7001I: Resource adapter helloworld-ear.helloworld-ra installed";
        
        assertTrue(CONSOLE_LOG.getCanonicalFile() + " doesn't exist", CONSOLE_LOG.exists());
        assertTrue("Expecting message [" + message + "] in server console log but not found.", 
                isAdapterInstalled(message));      
    }
    
    public boolean isAdapterInstalled(String message) throws FileNotFoundException {
        boolean installed = false;
        
        Scanner scanner = new Scanner(CONSOLE_LOG);
        while (scanner.hasNextLine()) {
            if(scanner.nextLine().contains(message)) { 
                installed = true;
            }
        }
        scanner.close();
        
        return installed;
    }
}
