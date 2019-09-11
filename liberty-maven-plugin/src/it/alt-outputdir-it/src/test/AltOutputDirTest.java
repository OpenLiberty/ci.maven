package test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * 
 * AltOutputDir test case
 * 
 */

public class AltOutputDirTest {

    @Test
    public void testAltOutputDirExists() throws Exception {
        File altOutputDir = new File("liberty-alt-output-dir");
        assertTrue(altOutputDir.getCanonicalFile() + " doesn't exist", altOutputDir.exists());
    }
    

}
