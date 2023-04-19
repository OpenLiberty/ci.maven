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

import java.io.PrintStream;
import java.util.Objects;

import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AntTaskFactory {
    private static final Logger LOG = LoggerFactory.getLogger(AntTaskFactory.class);

    private final Project ant;

    private AntTaskFactory(MavenProject mavenProject) {

        this.ant = new Project();
        ant.setBaseDir(mavenProject.getBasedir());

        initAntLogger(ant);

        ant.init();
    }

    protected void initAntLogger(final Project ant) {
        Slf4jLoggingBuildListener antLogger = new Slf4jLoggingBuildListener(LOG);
        antLogger.setEmacsMode(true);
        antLogger.setOutputPrintStream(System.out);
        antLogger.setErrorPrintStream(System.err);

        if (LOG.isDebugEnabled()) {
            antLogger.setMessageOutputLevel(Project.MSG_VERBOSE);
        } else {
            antLogger.setMessageOutputLevel(Project.MSG_INFO);
        }

        ant.addBuildListener(antLogger);
    }

    public static AntTaskFactory forMavenProject(MavenProject mavenProject) {
        return new AntTaskFactory(mavenProject);
    }

    @SuppressWarnings("unchecked")
    public <T extends Task> T createTask(String taskName) {
        return (T) this.ant.createTask(taskName);
    }

    static class Slf4jLoggingBuildListener extends DefaultLogger {

        private final Logger logger;

        public Slf4jLoggingBuildListener(Logger logger) {
            super();
            this.logger = Objects.requireNonNull(logger);
        }

        @Override
        protected void printMessage(String message, PrintStream stream, int priority) {
            switch (priority) {
                case Project.MSG_ERR:
                    logger.error(message);
                    break;
                case Project.MSG_WARN:
                    logger.warn(message);
                    break;
                case Project.MSG_INFO:
                    logger.info(message);
                    break;
                case Project.MSG_DEBUG:
                    logger.debug(message);
                    break;
                case Project.MSG_VERBOSE:
                    logger.trace(message);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown logging level: " + priority);
            }
        }
    }
}
