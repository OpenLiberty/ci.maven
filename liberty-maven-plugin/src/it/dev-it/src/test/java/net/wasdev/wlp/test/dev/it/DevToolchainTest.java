package net.wasdev.wlp.test.dev.it;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.io.FileUtils;
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

            startProcess(null, true, "mvn liberty:");

            assertTrue(verifyLogMessageExists("Maven compiler plugin is not configured with a jdkToolchain. Using Liberty Maven Plugin jdkToolchain configuration for Java compiler options.", 120000));
            assertTrue(verifyLogMessageExists("Setting compiler source to toolchain JDK version 11", 120000));

            // Trigger a recompile by modifying a Java source file
            File javaFile = new File(tempProj, "src/main/java/com/demo/HelloWorld.java");
            String originalContent = "public String helloWorld() {\n\t\treturn \"helloWorld\";\n\t}";
            String modifiedContent = "public String helloWorld() {\n\t\treturn \"helloWorldModified\";\n\t}";
            String fileContent = FileUtils.readFileToString(javaFile, "UTF-8");
            String newContent = fileContent.replace(originalContent, modifiedContent);
            FileUtils.writeStringToFile(javaFile, newContent, "UTF-8");

            // Verify that recompilation used compiler options
            assertTrue(verifyLogMessageExists("Recompiling with compiler options:", 120000));
            assertTrue(verifyLogMessageExists("-source, 11", 120000));
            assertTrue(verifyLogMessageExists("-target, 11", 120000));
        } finally {
            cleanUpAfterClass();
        }
    }

    @Test
    public void noToolchainConfigurationDoesNotEmitToolchainMessages() throws Exception {
        setUpBeforeClass(null, "../resources/basic-dev-project", true, false, null, null);
        try {
            startProcess(null, true, "mvn liberty:");

            assertTrue(verifyLogMessageDoesNotExist(
                    "Maven compiler plugin is not configured with a jdkToolchain. Using Liberty Maven Plugin jdkToolchain configuration for Java compiler options.",
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

            startProcess(null, true, "mvn liberty:");

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

            startProcess(null, true, "mvn liberty:");

            assertTrue(verifyLogMessageExists("Liberty Maven Plugin jdkToolchain configuration (version 11) does not match the Maven Compiler Plugin jdkToolchain configuration (version 8). The Liberty Maven Plugin jdkToolchain configuration will be used for compilation.", 120000));
        } finally {
            cleanUpAfterClass();
        }
    }

    @Test
    public void libertyToolchainWithoutSurefireToolchainLogsInfoAndUsesToolchainVersion() throws Exception {
        setUpBeforeClass(null, "../resources/basic-dev-project", true, false, null, null);
        try {
            String additionalConfigMarker = "<!-- ADDITIONAL_CONFIGURATION -->";
            String additionalConfigReplacement = "<jdkToolchain>\n" +
                    "            <version>11</version>\n" +
                    "          </jdkToolchain>\n" +
                    "          <!-- ADDITIONAL_CONFIGURATION -->";
            replaceString(additionalConfigMarker, additionalConfigReplacement, pom);

            startProcess(null, true, "mvn -X liberty:");

            assertTrue(verifyLogMessageExists("maven-surefire-plugin is not configured with a jdkToolchain. Using Liberty Maven Plugin jdkToolchain configuration for test execution.", 120000));
        } finally {
            cleanUpAfterClass();
        }
    }

    @Test
    public void matchingSurefireToolchainConfigurationsLogInfoMessage() throws Exception {
        setUpBeforeClass(null, "../resources/basic-dev-project", true, false, null, null);
        try {
            String additionalConfigMarker = "<!-- ADDITIONAL_CONFIGURATION -->";
            String additionalConfigReplacement = "<jdkToolchain>\n" +
                    "            <version>11</version>\n" +
                    "          </jdkToolchain>\n" +
                    "          <!-- ADDITIONAL_CONFIGURATION -->";
            replaceString(additionalConfigMarker, additionalConfigReplacement, pom);

            String pluginsEndMarker = "</plugins>";
            String surefirePluginReplacement = "<plugin>\n" +
                    "        <groupId>org.apache.maven.plugins</groupId>\n" +
                    "        <artifactId>maven-surefire-plugin</artifactId>\n" +
                    "        <version>3.0.0</version>\n" +
                    "        <configuration>\n" +
                    "          <jdkToolchain>\n" +
                    "            <version>11</version>\n" +
                    "          </jdkToolchain>\n" +
                    "        </configuration>\n" +
                    "      </plugin>\n" +
                    "      </plugins>";
            replaceString(pluginsEndMarker, surefirePluginReplacement, pom);

            startProcess(null, true, "mvn -X liberty:");

            assertTrue(verifyLogMessageExists("Liberty Maven Plugin jdkToolchain configuration matches the maven-surefire-plugin jdkToolchain configuration: version 11.", 120000));
        } finally {
            cleanUpAfterClass();
        }
    }

    @Test
    public void mismatchedSurefireToolchainConfigurationsLogWarningMessage() throws Exception {
        setUpBeforeClass(null, "../resources/basic-dev-project", true, false, null, null);
        try {
            String additionalConfigMarker = "<!-- ADDITIONAL_CONFIGURATION -->";
            String additionalConfigReplacement = "<jdkToolchain>\n" +
                    "            <version>11</version>\n" +
                    "          </jdkToolchain>\n" +
                    "          <!-- ADDITIONAL_CONFIGURATION -->";
            replaceString(additionalConfigMarker, additionalConfigReplacement, pom);

            String pluginsEndMarker = "</plugins>";
            String surefirePluginReplacement = "<plugin>\n" +
                    "        <groupId>org.apache.maven.plugins</groupId>\n" +
                    "        <artifactId>maven-surefire-plugin</artifactId>\n" +
                    "        <version>3.0.0</version>\n" +
                    "        <configuration>\n" +
                    "          <jdkToolchain>\n" +
                    "            <version>8</version>\n" +
                    "          </jdkToolchain>\n" +
                    "        </configuration>\n" +
                    "      </plugin>\n" +
                    "      </plugins>";
            replaceString(pluginsEndMarker, surefirePluginReplacement, pom);

            startProcess(null, true, "mvn -X liberty:");

            assertTrue(verifyLogMessageExists("Liberty Maven Plugin jdkToolchain configuration (version 11) does not match the maven-surefire-plugin jdkToolchain configuration (version 8). The Liberty Maven Plugin jdkToolchain configuration will be used for test execution.", 120000));
        } finally {
            cleanUpAfterClass();
        }
    }
}
