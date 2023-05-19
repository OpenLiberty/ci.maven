/**
 * (C) Copyright IBM Corporation 2020, 2023.
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
package io.openliberty.tools.maven.server;

import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * A class to store a set of CopyDependencies.
 */
public class CopyDependencies {

    /**
     * The location to copy dependencies to. This can be a full path, or a path relative to
     * ${server.config.dir}. The default is ${server.config.dir}/lib/global.
     */
    @Parameter
    private String location = "lib/global";

    /**
     * Boolean to indicate whether to strip versions from the file names when copying.
     * The default is false.
     */
    @Parameter(defaultValue="false")
    private Boolean stripVersion;

    /**
     * A list of Dependency to copy.
     */
    private List<Dependency> copyDependencyList = new ArrayList<Dependency>();
    
    /**
     * A list of DependencyGroup to copy.
     */
    private List<DependencyGroup> copyDependencyGroupList = new ArrayList<DependencyGroup>();
    
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
    
    public boolean isStripVersion() {
        if (this.stripVersion == null) {
            return false;
        }
        return this.stripVersion.booleanValue();
    }

    public Boolean getStripVersion() {
        return this.stripVersion;
    }

    public void setStripVersion(boolean strip) {
        this.stripVersion = Boolean.valueOf(strip);
    }
    
    /**
     * Get all the current Dependency to copy.
     *
     * @return A list with the Dependency to copy.
     */
    public List<Dependency> getDependencies() {
        return copyDependencyList;
    }

    /**
     * Add a Dependency into a list.
     *
     * @param dependency
     */
    public void addDependency(Dependency dependency) {
        copyDependencyList.add(dependency);
    }

    /**
     * Get all the current DependencyGroups to copy.
     *
     * @return A list with the DependencyGroup to copy.
     */
    public List<DependencyGroup> getDependencyGroups() {
        return copyDependencyGroupList;
    }

    /**
     * Add a DependencyGroup into a list.
     *
     * @param dependencyGroup
     */
    public void addDependencyGroup(DependencyGroup dependencyGroup) {
        copyDependencyGroupList.add(dependencyGroup);
    }

}
