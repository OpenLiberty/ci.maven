package io.openliberty.tools.maven.utils;

import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.Objects;

public class LibertyAntTaskFactory {

    private static final Logger LOG = LoggerFactory.getLogger(LibertyAntTaskFactory.class);

    private final MavenProject mavenProject;
    private final Project ant;

    private LibertyAntTaskFactory(MavenProject mavenProject) {
        this.mavenProject = mavenProject;

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

    public static LibertyAntTaskFactory forMavenProject(MavenProject mavenProject) {
        return new LibertyAntTaskFactory(mavenProject);
    }

    @SuppressWarnings("unchecked")
    public <T extends Task> T createTask(String taskName) {
        return (T) this.ant.createTask(taskName);
    }

    static class Slf4jLoggingBuildListener extends DefaultLogger implements BuildListener {

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
