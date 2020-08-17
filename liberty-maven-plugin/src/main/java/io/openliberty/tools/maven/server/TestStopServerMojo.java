/**
 * (C) Copyright IBM Corporation 2017, 2019.
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
package io.openliberty.tools.maven.server;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Quickly bypass stopping server if server isn't started
 */
@Mojo(name = "test-stop")

public class TestStopServerMojo extends StopServerMojo {
    
    @Parameter(property = "skipTestServer", defaultValue = "false")
    private boolean skipTestServer;

    @Override
    protected void doExecute() throws Exception {
        
        String mavenSkipTest = System.getProperty( "maven.test.skip" );
        String skipTests = System.getProperty( "skipTests" );
        String skipITs = System.getProperty( "skipITs" );
        if((skipTests != null && skipTests.equalsIgnoreCase("true")) 
                || (skipITs != null && skipITs.equalsIgnoreCase("true")) 
                || (mavenSkipTest != null && mavenSkipTest.equalsIgnoreCase("true"))
                || skipTestServer){
            getLog().info("\nSkipping test-stop goal.\n");
            return;
        }
        super.doExecute();
    }
}
