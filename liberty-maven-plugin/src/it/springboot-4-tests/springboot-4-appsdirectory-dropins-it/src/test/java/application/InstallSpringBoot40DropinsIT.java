package application;

import static org.junit.Assert.*;
import org.junit.Test;
import java.io.File;

public class InstallSpringBoot40DropinsIT {
    
    @Test
    public void testServerXmlContainsSpringBoot40Feature() throws Exception {
        File serverXml = new File("target/liberty/wlp/usr/servers/SpringBoot40DropinsServer/server.xml");
        assertTrue("server.xml should exist", serverXml.exists());
        
        String content = new String(java.nio.file.Files.readAllBytes(serverXml.toPath()));
        assertTrue("server.xml should contain springBoot-4.0 feature", 
                   content.contains("springBoot-4.0"));
    }
    
    @Test
    public void testSpringBootApplicationDeployedToDropins() throws Exception {
        // Spring Boot apps are deployed to dropins/spring/ subdirectory
        File f = new File("target/liberty/wlp/usr/servers/SpringBoot40DropinsServer/dropins/spring/thin-springboot-4-appsdirectory-dropins-it-1.0.0.Final.jar");
        assertTrue(f.getCanonicalFile() + " doesn't exist. Plugin failed to place the file at right destination.", f.exists());
    }
    
    @Test
    public void testLibertyServerInstalled() throws Exception {
        File libertyDir = new File("target/liberty/wlp");
        assertTrue("Liberty installation directory should exist", libertyDir.exists());
        
        File serverDir = new File("target/liberty/wlp/usr/servers/SpringBoot40DropinsServer");
        assertTrue("Server directory should exist", serverDir.exists());
    }
}
