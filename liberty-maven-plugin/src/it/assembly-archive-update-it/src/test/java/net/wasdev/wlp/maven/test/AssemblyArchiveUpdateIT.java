package net.wasdev.wlp.maven.test;

import static org.junit.Assert.*;

import java.io.File;
import org.junit.Test;
import org.apache.commons.io.FileUtils;

public class AssemblyArchiveUpdateIT {

    @Test
    public void testCorrectsAssemblyArchive() throws Exception {
        File installMarker = new File("liberty/wlp", ".installed");
        assertTrue("install marker " + installMarker.getCanonicalFile() + " doesn't exist", installMarker.exists());
        String expectedRuntimeVersion = System.getProperty("libertyRuntimeVersion");
        String assemblyArchivePath = FileUtils.readFileToString(installMarker);
        assertTrue("assembly archive path " + assemblyArchivePath + " does not contain expected runtime version " + expectedRuntimeVersion, 
        	assemblyArchivePath.contains(expectedRuntimeVersion));
    }
}
