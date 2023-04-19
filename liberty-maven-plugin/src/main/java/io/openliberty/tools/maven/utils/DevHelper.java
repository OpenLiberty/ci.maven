/**
 * (C) Copyright IBM Corporation 2023.
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
package io.openliberty.tools.maven.utils;

import java.util.Properties;

import org.codehaus.plexus.util.xml.Xpp3Dom;

public class DevHelper {

    /**
     * Use the following priority ordering for skip test flags: <br>
     * 1. within Liberty Maven plugin’s configuration in a module <br>
     * 2. within Liberty Maven plugin’s configuration in a parent pom’s pluginManagement <br>
     * 3. by command line with -D <br>
     * 4. within a module’s properties <br>
     * 5. within a parent pom’s properties <br>
     * 
     * @param config the config xml that might contain the param as an attribute
     * @param userProps the Maven user properties
     * @param props the Maven project's properties
     * @param param the Boolean parameter to look for
     * @return a boolean in priority order, or null if the param was not found anywhere
     */
    public static boolean getBooleanFlag(Xpp3Dom config, Properties userProps, Properties props, String param) {
        // this handles 1 and 2
        Boolean pluginConfig = parseBooleanIfDefined(getConfigValue(config, param));
        
        // this handles 3
        Boolean userProp = parseBooleanIfDefined(userProps.getProperty(param));
        
        // this handles 4 and 5
        Boolean prop = parseBooleanIfDefined(props.getProperty(param));
        
        return getFirstNonNullValue(pluginConfig, userProp, prop);
    }

    /**
     * Gets the value of the given attribute, or null if the attribute is not found.
     * @param config
     * @param attribute
     * @return
     */
    private static String getConfigValue(Xpp3Dom config, String attribute) {
        return (config.getChild(attribute) == null ? null : config.getChild(attribute).getValue());
    }

    /**
     * Parses a Boolean from a String if the String is not null.  Otherwise returns null.
     * @param value the String to parse
     * @return a Boolean, or null if value is null
     */
    private static Boolean parseBooleanIfDefined(String value) {
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return null;
    }

    /**
     * Gets the value of the first Boolean object that is not null, in order from lowest to highest index.
     * @param booleans an array of Boolean objects, some of which may be null
     * @return the value of the first non-null Boolean, or false if everything is null
     */
    public static boolean getFirstNonNullValue(Boolean... booleans) {
        for (Boolean b : booleans) {
            if (b != null) {
                return b.booleanValue();
            }
        }
        return false;
    }
}
