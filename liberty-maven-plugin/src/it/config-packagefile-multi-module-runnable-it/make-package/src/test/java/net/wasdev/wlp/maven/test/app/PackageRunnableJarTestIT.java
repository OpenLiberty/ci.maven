package net.wasdev.wlp.maven.test.app;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;


/**
 *
 * Default Config Test Case
 *
 */

public class PackageRunnableJarTestIT {

    private String projectBuildName = "package-runnable-jar-it";

    @Test
    public void testPackageFileJarExists() throws Exception {
	File path = new File(projectBuildName + ".jar");
        assertTrue("Package file does not exist at path: "+path.getCanonicalPath(), path.exists());
    }
    
    @Test
    public void testPackageFileZipNotExists() throws Exception {
	File path = new File(projectBuildName + ".zip");
        assertFalse("Package file exists at path: "+ path.getCanonicalPath(), path.exists());
    }
}
