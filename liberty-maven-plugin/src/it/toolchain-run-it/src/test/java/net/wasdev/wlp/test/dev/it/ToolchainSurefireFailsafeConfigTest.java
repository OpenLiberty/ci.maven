package net.wasdev.wlp.test.dev.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Test;

import io.openliberty.tools.maven.utils.ExecuteMojoUtil;

public class ToolchainSurefireFailsafeConfigTest {

    @Test
    public void jdkToolchainIsPreservedForMavenSurefirePluginTestGoal() {
        Plugin plugin = new Plugin();
        plugin.setGroupId("org.apache.maven.plugins");
        plugin.setArtifactId("maven-surefire-plugin");
        plugin.setVersion("3.1.2");

        Xpp3Dom config = new Xpp3Dom("configuration");
        Xpp3Dom jdkToolchain = new Xpp3Dom("jdkToolchain");
        Xpp3Dom version = new Xpp3Dom("version");
        version.setValue("11");
        jdkToolchain.addChild(version);
        config.addChild(jdkToolchain);

        plugin.setConfiguration(config);

        Xpp3Dom goalConfig = ExecuteMojoUtil.getPluginGoalConfig(plugin, "test", new SystemStreamLog());
        Xpp3Dom jdkToolchainChild = goalConfig.getChild("jdkToolchain");
        Xpp3Dom versionChild = jdkToolchainChild.getChild("version");

        assertNotNull("jdkToolchain element should be preserved for maven-surefire-plugin:test", jdkToolchainChild);
        assertNotNull("version child should be present under jdkToolchain", versionChild);
        assertEquals("11", versionChild.getValue());
    }

    @Test
    public void jdkToolchainIsPreservedForMavenFailsafePluginIntegrationTestGoal() {
        Plugin plugin = new Plugin();
        plugin.setGroupId("org.apache.maven.plugins");
        plugin.setArtifactId("maven-failsafe-plugin");
        plugin.setVersion("3.1.2");

        Xpp3Dom config = new Xpp3Dom("configuration");
        Xpp3Dom jdkToolchain = new Xpp3Dom("jdkToolchain");
        Xpp3Dom version = new Xpp3Dom("version");
        version.setValue("11");
        jdkToolchain.addChild(version);
        config.addChild(jdkToolchain);

        plugin.setConfiguration(config);

        Xpp3Dom goalConfig = ExecuteMojoUtil.getPluginGoalConfig(plugin, "integration-test", new SystemStreamLog());
        Xpp3Dom jdkToolchainChild = goalConfig.getChild("jdkToolchain");
        Xpp3Dom versionChild = jdkToolchainChild.getChild("version");

        assertNotNull("jdkToolchain element should be preserved for maven-failsafe-plugin:integration-test", jdkToolchainChild);
        assertNotNull("version child should be present under jdkToolchain", versionChild);
        assertEquals("11", versionChild.getValue());
    }
}
