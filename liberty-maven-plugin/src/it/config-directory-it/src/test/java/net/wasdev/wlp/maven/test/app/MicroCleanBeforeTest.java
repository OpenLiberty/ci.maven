package net.wasdev.wlp.maven.test.app;

import static org.junit.Assert.*;

import java.io.File;
import org.junit.Test;

/**
 * 
 * Before State: Verify generation of jvm.options, bootstrap.properties, and liberty-plugin-variable-config.xml
 * 
 */

public class MicroCleanBeforeTest {

    @Test
    public void verifyBeforeJvmOptions() throws Exception {
        File jvmOptions = new File("liberty/usr/servers/test/jvm.options");

        assertTrue("jvm.options was not generated", jvmOptions.exists());
    }

    @Test
    public void verifyBeforeBootstrapProperties() throws Exception {
        File bootstrapProperties = new File("liberty/usr/servers/test/bootstrap.properties");

        assertTrue("bootstrap.properties was not generated", bootstrapProperties.exists());
    }

    @Test
    public void verifyBeforeLibertyPluginVariableConfig() throws Exception {
        File libertyPluginVariableConfig = new File("liberty/usr/servers/test/configDropins/defaults/liberty-plugin-variable-config.xml");

        assertTrue("liberty variable xml was not generated", libertyPluginVariableConfig.exists());
    }
}