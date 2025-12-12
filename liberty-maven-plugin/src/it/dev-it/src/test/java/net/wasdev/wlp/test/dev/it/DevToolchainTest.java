package net.wasdev.wlp.test.dev.it;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DevToolchainTest extends BaseDevTest {

    @Test
    public void libertyToolchainWithoutCompilerToolchainLogsInfoAndUsesToolchainVersion() throws Exception {
        setUpBeforeClass(null, "../resources/basic-dev-project", true, false, null, null);
        try {
            String additionalConfigMarker = "<!-- ADDITIONAL_CONFIGURATION -->";
            String additionalConfigReplacement = "<jdkToolchain>\n" +
                    "            <version>11</version>\n" +
                    "          </jdkToolchain>\n" +
                    "          <!-- ADDITIONAL_CONFIGURATION -->";
            replaceString(additionalConfigMarker, additionalConfigReplacement, pom);

            startProcess(null, true, "mvn -X liberty:");

            assertTrue(verifyLogMessageExists("Maven compiler plugin is not configured with a jdkToolchain. Using liberty-maven-plugin jdkToolchain configuration for Java compiler options.", 120000));
            assertTrue(verifyLogMessageExists("Setting compiler source to toolchain JDK version 11", 120000));
        } finally {
            cleanUpAfterClass();
        }
    }

    @Test
    public void noToolchainConfigurationDoesNotEmitToolchainMessages() throws Exception {
        setUpBeforeClass(null, "../resources/basic-dev-project", true, false, null, null);
        try {
            startProcess(null, true, "mvn -X liberty:");

            assertTrue(verifyLogMessageDoesNotExist(
                    "Maven compiler plugin is not configured with a jdkToolchain. Using liberty-maven-plugin jdkToolchain configuration for Java compiler options.",
                    120000));
            assertTrue(verifyLogMessageDoesNotExist(
                    "Liberty Maven Plugin jdkToolchain configuration matches the Maven Compiler Plugin jdkToolchain configuration",
                    120000));
            assertTrue(verifyLogMessageDoesNotExist(
                    "Liberty Maven Plugin jdkToolchain configuration (version",
                    120000));
        } finally {
            cleanUpAfterClass();
        }
    }

    @Test
    public void matchingToolchainConfigurationsLogInfoMessage() throws Exception {
        setUpBeforeClass(null, "../resources/basic-dev-project", true, false, null, null);
        try {
            String additionalConfigMarker = "<!-- ADDITIONAL_CONFIGURATION -->";
            String additionalConfigReplacement = "<jdkToolchain>\n" +
                    "            <version>11</version>\n" +
                    "          </jdkToolchain>\n" +
                    "          <!-- ADDITIONAL_CONFIGURATION -->";
            replaceString(additionalConfigMarker, additionalConfigReplacement, pom);

            String pluginsEndMarker = "</plugins>";
            String compilerPluginReplacement = "<plugin>\n" +
                    "        <groupId>org.apache.maven.plugins</groupId>\n" +
                    "        <artifactId>maven-compiler-plugin</artifactId>\n" +
                    "        <version>3.11.0</version>\n" +
                    "        <configuration>\n" +
                    "          <jdkToolchain>\n" +
                    "            <version>11</version>\n" +
                    "          </jdkToolchain>\n" +
                    "          <release>11</release>\n" +
                    "          <source>11</source>\n" +
                    "          <target>11</target>\n" +
                    "        </configuration>\n" +
                    "      </plugin>\n" +
                    "      </plugins>";
            replaceString(pluginsEndMarker, compilerPluginReplacement, pom);

            startProcess(null, true, "mvn -X liberty:");

            assertTrue(verifyLogMessageExists("Liberty Maven Plugin jdkToolchain configuration matches the Maven Compiler Plugin jdkToolchain configuration: version 11.", 120000));
        } finally {
            cleanUpAfterClass();
        }
    }

    @Test
    public void mismatchedToolchainConfigurationsLogWarningMessage() throws Exception {
        setUpBeforeClass(null, "../resources/basic-dev-project", true, false, null, null);
        try {
            String additionalConfigMarker = "<!-- ADDITIONAL_CONFIGURATION -->";
            String additionalConfigReplacement = "<jdkToolchain>\n" +
                    "            <version>11</version>\n" +
                    "          </jdkToolchain>\n" +
                    "          <!-- ADDITIONAL_CONFIGURATION -->";
            replaceString(additionalConfigMarker, additionalConfigReplacement, pom);

            String pluginsEndMarker = "</plugins>";
            String compilerPluginReplacement = "<plugin>\n" +
                    "        <groupId>org.apache.maven.plugins</groupId>\n" +
                    "        <artifactId>maven-compiler-plugin</artifactId>\n" +
                    "        <version>3.11.0</version>\n" +
                    "        <configuration>\n" +
                    "          <jdkToolchain>\n" +
                    "            <version>8</version>\n" +
                    "          </jdkToolchain>\n" +
                    "          <release>8</release>\n" +
                    "          <source>8</source>\n" +
                    "          <target>8</target>\n" +
                    "        </configuration>\n" +
                    "      </plugin>\n" +
                    "      </plugins>";
            replaceString(pluginsEndMarker, compilerPluginReplacement, pom);

            startProcess(null, true, "mvn -X liberty:");

            assertTrue(verifyLogMessageExists("Liberty Maven Plugin jdkToolchain configuration (version 11) does not match the Maven Compiler Plugin jdkToolchain configuration (version 8). The project-level Maven Compiler Plugin toolchain configuration will be used for compilation.", 120000));
        } finally {
            cleanUpAfterClass();
        }
    }
}
