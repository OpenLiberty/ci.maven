/**
 * (C) Copyright IBM Corporation 2017.
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
package io.openliberty.tools.maven.applications;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Display an application URL in the default browser.
 */
@Mojo(name = "display-url")
public class DisplayUrlMojo extends AbstractMojo {
    
    /**
     * Display a URI in the default browser
     */
    @Parameter
    protected URI applicationURL;
 
    public void execute() throws MojoExecutionException {
        if (applicationURL != null && Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(applicationURL);
            } catch (IOException e) {
                throw new MojoExecutionException(e.getLocalizedMessage(), e);
            }
        }
    }
}
