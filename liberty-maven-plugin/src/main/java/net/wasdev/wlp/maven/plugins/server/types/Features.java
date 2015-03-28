/**
 * (C) Copyright IBM Corporation 2015.
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
package net.wasdev.wlp.maven.plugins.server.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A class to store a set of features and the install configuration.
 */
public class Features {

    /**
     * Acceptance of terms and conditions of all the features. By default the
     * license value is false.
     */
    private boolean acceptLicense = false;

    /**
     * Specify where to install the features. The features can be installed to
     * any configured product extension location, or as a user feature. If this
     * option is not specified the feature will be installed as a user feature.
     */
    private String to = "usr";

    /**
     * If a file that is part of the ESA already exists on the system, you must
     * specify what actions to take. Valid options are: fail - abort the
     * installation; ignore - continue the installation and ignore the file that
     * exists; replace - overwrite the existing file. Do not use the replace
     * option to reinstall features.
     */
    private String whenFileExists = null;

    /**
     * A list with the names of the features.
     */
    private List<String> featureList = new ArrayList<String>();

    public boolean isAcceptLicense() {
        return acceptLicense;
    }

    public void setAcceptLicense(boolean acceptLicense) {
        this.acceptLicense = acceptLicense;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getWhenFileExists() {
        return whenFileExists;
    }

    public void setWhenFileExists(String whenFileExists) {
        this.whenFileExists = whenFileExists;
    }

    /**
     * Get all the current features.
     *
     * @return A list with the name of the features.
     */
    public List<String> getFeatures() {
        return featureList;
    }

    /**
     * Add a feature into a list.
     *
     * @param feature A feature name.
     */
    public void addFeature(String feature) {
        if (feature == null) {
            throw new NullPointerException();
        }
        feature = feature.trim();
        if (!feature.isEmpty()) { 
            featureList.add(feature);
        }
    }
    
    public String getFeaturesAsString() {
        StringBuilder buffer = new StringBuilder();
        int size = featureList.size();
        for (int i = 0; i < size;  i++) {
            buffer.append(featureList.get(i));
            if (i + 1 < size) {
                buffer.append(",");
            }
        }
        return buffer.toString();
    }

}
