package application.rest;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

@RunWith(Arquillian.class)
public class TransactionRestEndpointIT {

	@Deployment
	public static WebArchive createDeploymentY() {
		File[] mavenFiles = Maven.resolver().loadPomFromFile("pom.xml").importRuntimeDependencies().resolve()
				.withTransitivity().asFile();

		return ShrinkWrap.create(WebArchive.class).addPackage("application.rest").addPackage("application")
				.addAsLibraries(mavenFiles);
	}

	@Test
	public void testRunningOnServer() throws Exception {
		Properties p = System.getProperties();
		Enumeration<Object> keys = p.keys();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			String value = (String) p.get(key);
			if (key.equals("wlp.process.type") && value.equals("server")) {
				return;
			}
		}
		assertTrue(false);
	}

	@Test
	public void testDataExistsAtEndpoint() throws Exception {
		Thread.sleep(10000);
		URL endpoint = new URL("http://localhost:9080/myLibertyApp/api/transactions");
		String body = readAllAndClose(endpoint.openStream());
		int bodyLength = body.length();
		assertTrue(bodyLength > 0);
	}

	@Test
	public void testNewDataAddedToEndpoint() throws Exception {
		Thread.sleep(10000);
		String firstBody = readAllAndClose(new URL("http://localhost:9080/myLibertyApp/api/transactions").openStream());
		int firstBodyLength = firstBody.length();
		Thread.sleep(12000);
		String secondBody = readAllAndClose(
				new URL("http://localhost:9080/myLibertyApp/api/transactions").openStream());
		int secondBodyLength = secondBody.length();
		assertTrue(secondBodyLength > firstBodyLength);
	}

	String readAllAndClose(InputStream is) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			int read;
			while ((read = is.read()) != -1) {
				out.write(read);
			}
		} finally {
			try {
				is.close();
			} catch (Exception e) {
			}
		}
		return out.toString();
	}

}
