import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Test;

import net.wasdev.wlp.common.arquillian.util.Constants;

/**
 * Only included in the skip-with-xml Maven profile.
 * @author ctianus.ibm.com
 *
 */
public class SkipWithXmlIT {

	@Test
	public void testSkipWithXML() throws Exception {
		// This test has the skip flag true with the XML file already existing.
		// In this case, the server should use the arquillian.xml that is in
		// src/test/resources/arquillian.xml.
		File arquillianXML = new File("target/test-classes/arquillian.xml");
		InputStream is = new FileInputStream(arquillianXML);
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line;
		while ((line = br.readLine()) != null) {
			// Make sure none of the lines indicate that the file was generated
			// by the configure-arquillian goal.
			assertFalse(line.equals(Constants.CONFIGURE_ARQUILLIAN_COMMENT));
		}
	}

}
