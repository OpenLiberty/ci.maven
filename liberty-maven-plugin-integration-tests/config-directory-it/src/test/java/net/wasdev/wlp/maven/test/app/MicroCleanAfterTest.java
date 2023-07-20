package net.wasdev.wlp.maven.test.app;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;

import org.junit.Test;

/**
 * 
 * After State: Verify non-existence of jvm.options, bootstrap.properties, and liberty-plugin-variable-config.xml
 * 
 */

public class MicroCleanAfterTest {

    @Test
    public void verifyBeforeJvmOptions() throws Exception {
        File jvmOptions = new File("liberty/usr/servers/test/jvm.options");

        assertTrue("jvm.options should not exist", !jvmOptions.exists());
    }

    @Test
    public void verifyBeforeBootstrapProperties() throws Exception {
        File bootstrapProperties = new File("liberty/usr/servers/test/bootstrap.properties");

        assertTrue("bootstrap.properties should not exist", !bootstrapProperties.exists());
    }

    @Test
    public void verifyBeforeLibertyPluginVariableConfig() throws Exception {
        File libertyPluginVariableConfig = new File("liberty/usr/servers/test/configDropins/defaults/liberty-plugin-variable-config.xml");

        assertTrue("liberty variable xml wasn't created in before state", new File("liberty/usr/servers/test/configDropins/defaults").exists());
        assertTrue("liberty variable xml should not exist", !libertyPluginVariableConfig.exists());
    }
}