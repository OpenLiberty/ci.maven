/**
 * (C) Copyright IBM Corporation 2019.
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

import org.apache.maven.plugin.logging.Log;

import io.openliberty.tools.common.CommonLoggerI;
import org.apache.maven.plugin.logging.SystemStreamLog;

/**
 * Slf4j wrapper
 */
public class CommonLogger implements CommonLoggerI {

    private static CommonLogger logger = null;
    private Log loggerImpl;

    public static CommonLogger getInstance() {
        if (logger == null) {
            logger = new CommonLogger();
        }
        return logger;
    }

    @Override
    public void debug(String msg) {
        getLog().debug(msg);
    }

    public Log getLog() {
        if (this.loggerImpl == null) {
            this.loggerImpl = new SystemStreamLog();
        }

        return loggerImpl;
    }

    @Override
    public void debug(String msg, Throwable e) {
        getLog().debug(msg, e);
    }

    @Override
    public void debug(Throwable e) {
        getLog().debug(e);
    }

    @Override
    public void warn(String msg) {
        getLog().warn(msg);
    }

    @Override
    public void info(String msg) {
        getLog().info(msg);
    }

    @Override
    public void error(String msg) {
        getLog().error(msg);
    }

    @Override
    public boolean isDebugEnabled() {
        return getLog().isDebugEnabled();
    }

}
