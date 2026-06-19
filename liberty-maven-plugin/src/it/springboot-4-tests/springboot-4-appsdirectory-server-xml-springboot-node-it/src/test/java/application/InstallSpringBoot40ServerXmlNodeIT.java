package application;

import static org.junit.Assert.*;
import org.junit.Test;
import java.io.File;

public class InstallSpringBoot40ServerXmlNodeIT {
    
    @Test
    public void testServerXmlContainsSpringBootApplicationNode() throws Exception {
        File serverXml = new File("target/liberty/wlp/usr/servers/SpringBoot40ServerXmlNodeServer/server.xml");
        assertTrue("server.xml should exist", serverXml.exists());
        
        String content = new String(java.nio.file.Files.readAllBytes(serverXml.toPath()));
        assertTrue("server.xml should contain springBoot-4.0 feature", 
                   content.contains("springBoot-4.0"));
        assertTrue("server.xml should contain springBootApplication node", 
                   content.contains("springBootApplication"));
        assertTrue("server.xml should reference the application JAR", 
                   content.contains("springboot-4-appsdirectory-server-xml-springboot-node-it.jar"));
    }
    
    @Test
    public void testThinApplicationExistsInAppsDirectory() throws Exception {
        File f = new File("target/liberty/wlp/usr/servers/SpringBoot40ServerXmlNodeServer/apps/springboot-4-appsdirectory-server-xml-springboot-node-it.jar");
        assertTrue(f.getCanonicalFile() + " doesn't exist. Plugin failed to place the file at right destination.", f.exists());
        File f2 = new File("target/liberty/wlp/usr/servers/SpringBoot40ServerXmlNodeServer/configDropins");
        assertFalse(f2.getCanonicalFile() + " folder should not exist.", f2.exists());
    }
    
    @Test
    public void testLibIndexCacheExists() throws Exception {
        File f = new File("target/liberty/wlp/usr/shared/resources/lib.index.cache");
        assertTrue(f.getCanonicalFile() + " doesn't exist. Plugin failed to place the cache directory at right destination.", f.exists());
    }
    
    @Test
    public void testLibertyServerInstalled() throws Exception {
        File libertyDir = new File("target/liberty/wlp");
        assertTrue("Liberty installation directory should exist", libertyDir.exists());
        
        File serverDir = new File("target/liberty/wlp/usr/servers/SpringBoot40ServerXmlNodeServer");
        assertTrue("Server directory should exist", serverDir.exists());
    }
}
