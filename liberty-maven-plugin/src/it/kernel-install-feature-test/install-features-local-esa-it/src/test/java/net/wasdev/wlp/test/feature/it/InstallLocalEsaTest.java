package net.wasdev.wlp.test.feature.it;

import static junit.framework.Assert.*;
import org.junit.Test;
import org.junit.AfterClass;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import java.io.FilenameFilter;

public class InstallLocalEsaTest {

    @Test
    public void installLocalEsaTest() throws Exception {
    	try {
			File jsonFile = new File("liberty/wlp/lib/features/com.ibm.websphere.appserver.json-1.0.mf");
			File jspFile = new File("liberty/wlp/lib/features/com.ibm.websphere.appserver.jsp-2.3.mf");

            assert jsonFile.exists() : "json-1.0 not installed";
            assert jspFile.exists() : "jsp-2.3 not installed";
            
        } catch (Exception e) {
            throw new AssertionError ("Fail to install user feature.", e);
        }
    }

}
