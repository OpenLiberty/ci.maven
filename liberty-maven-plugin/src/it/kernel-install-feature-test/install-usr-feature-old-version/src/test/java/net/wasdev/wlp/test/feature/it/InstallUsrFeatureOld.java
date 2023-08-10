package net.wasdev.wlp.test.feature.it;

import static junit.framework.Assert.*;
import org.junit.Test;
import org.junit.AfterClass;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import java.io.FilenameFilter;

public class InstallUsrFeatureOld {
	
	static File mavenLocalRepo = new File(System.getProperty("user.home")+ "/.m2/repository");
	static File userTestRepo = new File(mavenLocalRepo, "test/user/test/features");
	
	public static boolean deleteFolder(final File directory) {
		if (directory.isDirectory()) {
			File[] files = directory.listFiles();
			if (null != files) {
				for (File file : files) {
					if (file.isDirectory()) {
						deleteFolder(file);
					} else {
						if (!file.delete()) {
								file.deleteOnExit();
						}
					}
				}
			}
		}
		if(!directory.delete()){
			directory.deleteOnExit();
			return false;
		}
		return true;
	}
	
    /*
     * Manually copy user feature esa contents to Liberty image if OL version is less than 21.0.0.11.
     * For newer version of Liberty they can use prepare-feature goal to generate a json file which contains metadata of the user feature. 
     * This will resolve full dependencies for their user feature. 
     */
    @Test
    public void testUsrFeatureInstallOld() throws Exception {
    	try {
			File featureFile = new File("liberty/wlp/usr/cik/extensions/testExt/lib/features/testesa1.mf");

            assert featureFile.exists() : "testesa1.mf cannot be generated";
            
        } catch (Exception e) {
            throw new AssertionError ("Fail to install user feature.", e);
        }
    }
    
    
    @AfterClass
	public static void cleanUp() {
		deleteFolder(userTestRepo);
	}

}
