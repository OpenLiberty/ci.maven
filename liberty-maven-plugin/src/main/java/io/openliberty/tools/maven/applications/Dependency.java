/**
 * (C) Copyright IBM Corporation 2020.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.openliberty.tools.maven.applications;

import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * A Dependency specified with a filter and an optional location.
 */
public class Dependency {

    /**
     * Filter specifying the dependency to copy. The format is groupId:artifactId:version, with artifactId and 
     * version optional. Maven dependency management will be used to resolve the dependency.
     */
    @Parameter
    private String filter = null;

    /**
     * Optional location to copy the Dependency to. This can be a full path, or a path relative to
     * ${server.config.dir}. The location in the containing CopyDependencies configuration will be 
     * used if nothing is specified here.
     */
    @Parameter
    private String location = null;

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
    
    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }
}
