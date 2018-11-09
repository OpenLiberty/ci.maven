package net.wasdev.wlp.maven.test.app;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import org.junit.Test;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;


/**
 * 
 * configDirectory test case
 * 
 */

public class LibertySettingsDirectoryTest {

    @Test
    public void testLibertyConfigDirValidDir() throws Exception {
        File f1 = new File("liberty/etc", "repository.properties");
        assertTrue(f1.getCanonicalFile() + " doesn't exist", f1.exists());
    }

    
    @Test
    public void testLibertyConfigDirInvalidDir() throws Exception {
        File mavenHome = new File("/usr/local/Cellar/maven/3.5.4/libexec");

        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile( new File("../src/test/resources/invalidDirPom.xml"));
        request.setGoals( Collections.singletonList("package"));
        // request.setShellEnvironmentInherited(true);

        InvocationOutputHandler outputHandler = new InvocationOutputHandler(){
            @Override
            public void consumeLine(String line) throws IOException {
                if (line.contains("<libertySettingsFolder> must be a directory")) {
                    throw new IOException("Caught expected MojoExecutionException - " + line);
                }
            }
        };

        Invoker invoker = new DefaultInvoker();
        invoker.setOutputHandler(outputHandler);
        invoker.setMavenHome(mavenHome);
        try {
            InvocationResult result = invoker.execute( request );

            assertTrue("Exited successfully, expected non-zero exit code.", result.getExitCode() != 0);
            assertNotNull("Expected Exception to be thrown.", result.getExecutionException());
        } catch (MavenInvocationException e) {
            e.printStackTrace();
        }
    }
}