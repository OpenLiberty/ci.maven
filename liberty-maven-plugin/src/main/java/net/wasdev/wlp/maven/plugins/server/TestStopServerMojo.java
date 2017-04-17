/**
 * (C) Copyright IBM Corporation 2014, 2017.
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
package net.wasdev.wlp.maven.plugins.server;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Stop a liberty server if tests are not skipped
 */
@Mojo(name = "test-stop-server") 
public class TestStopServerMojo extends StopServerMojo {
    
    @Parameter(property = "testSkipServer", defaultValue = "false")
    private Boolean testSkipServer = false;

    @Override
    protected void doExecute() throws Exception {
        String mavenSkipTest = System.getProperty( "maven.test.skip" );
        String skipTests = System.getProperty( "skipTests" );
        String skipITs = System.getProperty( "skipITs" );
        if((skipTests != null && skipTests.equalsIgnoreCase("true")) 
                || (skipITs != null && skipITs.equalsIgnoreCase("true")) 
                || (mavenSkipTest != null && mavenSkipTest.equalsIgnoreCase("true"))
                || testSkipServer){
            return;
        }
        super.doExecute();
    }
}
