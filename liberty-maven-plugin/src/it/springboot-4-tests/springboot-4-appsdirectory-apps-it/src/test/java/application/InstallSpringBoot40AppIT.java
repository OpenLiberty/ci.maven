package application;

import static org.junit.Assert.*;
import org.junit.Test;
import java.io.File;

public class InstallSpringBoot40AppIT {
    
    @Test
    public void testSpringBoot40JarInstalled() throws Exception {
        File thinJar = new File("target/liberty/wlp/usr/servers/test/apps/thin-springboot-4-appsdirectory-apps-it-1.0.0.Final-exec.jar");
        assertTrue("Spring Boot 4.0 thin JAR should exist at: " + thinJar.getCanonicalPath(), 
                   thinJar.exists());
    }
    
    @Test
    public void testSpringBoot40FeatureInstalled() throws Exception {
        File serverXml = new File("target/liberty/wlp/usr/servers/test/server.xml");
        assertTrue("server.xml should exist", serverXml.exists());
        
        String content = new String(java.nio.file.Files.readAllBytes(serverXml.toPath()));
        assertTrue("server.xml should contain springBoot-4.0 feature", 
                   content.contains("springBoot-4.0"));
    }
}
