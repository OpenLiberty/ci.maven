/**
 * (C) Copyright IBM Corporation 2021.
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
package io.openliberty.tools.maven;

import io.openliberty.tools.common.plugins.util.ServerFeatureUtil;

public class ServerFeatureSupport extends BasicSupport {

    private ServerFeatureUtil servUtil;

    protected class ServerFeatureMojoUtil extends ServerFeatureUtil {

        @Override
        public void debug(String msg) {
            log.debug(msg);
        }

        @Override
        public void debug(String msg, Throwable e) {
            log.debug(msg, e);
        }

        @Override
        public void debug(Throwable e) {
            log.debug(e);
        }

        @Override
        public void warn(String msg) {
            log.warn(msg);
        }

        @Override
        public void info(String msg) {
            log.info(msg);
        }

        @Override
        public void error(String msg, Throwable e) {
            log.error(msg, e);
        }
    }

    private void createNewServerFeatureUtil() {
        servUtil = new ServerFeatureMojoUtil();
    }

    /**
     * Get a new instance of ServerFeatureUtil
     * 
     * @return instance of ServerFeatureUtil
     */
    protected ServerFeatureUtil getServerFeatureUtil() {
        if (servUtil == null) {
            createNewServerFeatureUtil();
        }
        return servUtil;
    }

}
