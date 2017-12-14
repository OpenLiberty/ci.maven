import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Test;

/**
 * Only included in the verify-main-app Maven profile.
 * 
 * @author ctianus.ibm.com
 *
 */
public class VerifyMainAppIT {

	@Test
	public void testVerifyMainApp() throws Exception {
		// This test verifies that the main app should start before the
		// Arquillian test application by looking at the build.log console
		// output.

		String mainAppOutput = "[AUDIT   ] CWWKT0016I: Web application available (default_host): http://localhost:9080/myLibertyApp/";
		String testAppOutput = // The test app name is randomly generated, so we
								// don't know it here. Instead just make sure
								// that another app is started after the main
								// app.
				"[AUDIT   ] CWWKT0016I: Web application available (default_host): http://localhost:9080/";

		boolean foundMainApp = false;

		File buildLog = new File("build.log");
		InputStream is = new FileInputStream(buildLog);
		BufferedReader br = new BufferedReader(new InputStreamReader(is));

		String line;
		while ((line = br.readLine()) != null) {
			if (!foundMainApp) {
				if (line.contains(mainAppOutput)) {
					foundMainApp = true;
				}
			} else if (line.contains(testAppOutput)) {
				return;
			}
		}

		assertTrue(false); // Should have returned in the while loop if the test passed
	}

}
