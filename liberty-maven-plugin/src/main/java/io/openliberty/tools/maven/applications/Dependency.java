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

import org.apache.maven.plugins.annotations.Parameter;

/**
 * A Dependency specified with GAV coordinate.
 */
public class Dependency {

    /**
     * The groupId of the Maven dependency to copy. This is required. Maven dependency management will be used to resolve the dependency.
     */
    @Parameter
    private String groupId = null;

    /**
     * The artifactId of the Maven dependency to copy. This is optional and also supports a '*' at the end. Maven dependency management will be used to resolve the dependency.
     */
    @Parameter
    private String artifactId = null;

    /**
     * The version of the Maven dependency to copy. This is optional.
     */
    @Parameter
    private String version = null;

    /**
     * The type of the Maven dependency to copy. This is optional. The default is 'jar'.
     */
    @Parameter
    private String type = "jar";

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
    
    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }
    
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    
}
