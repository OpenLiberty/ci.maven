package net.wasdev.wlp.maven.test.app;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;

public class ToolchainInstallFeatureTest {

    private static final String TOOLCHAIN_CONFIGURED_MESSAGE = "CWWKM4101I: The install-feature goal is using the configured toolchain JDK";
    
    @Test
    public void testToolchainUsedForInstallFeature() throws Exception {
        File buildLog = new File("../build.log");
        assertTrue("Build log should exist: " + buildLog.getAbsolutePath(), buildLog.exists());
        
        String logContent = readFile(buildLog);
        assertNotNull("Build log content should not be null", logContent);

        assertTrue("Build log should contain toolchain configured message for install-feature goal",
                logContent.contains(TOOLCHAIN_CONFIGURED_MESSAGE));
    }
    
    private String readFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
}
