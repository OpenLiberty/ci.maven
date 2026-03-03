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
    private static final String PRODUCT_VALIDATION_TOOLCHAIN_MESSAGE = "Product validation is using toolchain JAVA_HOME:";
    private static final String PRODUCT_VALIDATION_SUCCESS_MESSAGE = "Product validation completed successfully.";
    
    @Test
    public void testToolchainUsedForInstallFeature() throws Exception {
        File buildLog = new File("../build.log");
        if (!buildLog.exists()) {
            buildLog = new File("../../build.log");
        }

        assertTrue("Build log should exist: " + buildLog.getAbsolutePath(), buildLog.exists());
        
        String logContent = readFile(buildLog);
        assertNotNull("Build log content should not be null", logContent);

        assertTrue("Build log should contain toolchain configured message for install-feature goal",
                logContent.contains(TOOLCHAIN_CONFIGURED_MESSAGE));
        
        assertTrue("Product validation should use toolchain JAVA_HOME",
                logContent.contains(PRODUCT_VALIDATION_TOOLCHAIN_MESSAGE));
        
        assertTrue("Product validation should complete successfully",
                logContent.contains(PRODUCT_VALIDATION_SUCCESS_MESSAGE));
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
