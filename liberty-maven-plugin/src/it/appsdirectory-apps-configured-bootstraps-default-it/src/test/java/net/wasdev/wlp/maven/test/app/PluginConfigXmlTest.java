package net.wasdev.wlp.maven.test.app;

import java.io.File;
import java.io.FileInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 * Web application test case
 * 
 */
// The test-war.war is pulled in as a dependency and installed in the apps folder.
// Test that the location variable in server.xml with a default value is used for the location of the 
// application since a value is not provided elsewhere (bootstrap props, server.xml, include file, etc).
// Also verifies that variable values referencing other variables works correctly.
// Since the location is resolved correctly, there should be no configDropins file.

public class PluginConfigXmlTest {

    public final String CONFIG_DROPINS_XML="liberty/usr/servers/test/configDropins/defaults/install_apps_configuration_1491924271.xml";

    public final String APP_IN_APPS_FOLDER="liberty/usr/servers/test/apps/test-war.war";

    @Test
    public void testApplicationNotConfiguredInConfigDropins() throws Exception {
        File in = new File(CONFIG_DROPINS_XML);
        assertTrue("configDropins file exists " + CONFIG_DROPINS_XML, !in.exists());

    }

    @Test
    public void testApplicationLocatedInAppsFolder() throws Exception {
        File app = new File(APP_IN_APPS_FOLDER);

        assertTrue("Application not found in apps folder at " + APP_IN_APPS_FOLDER, app.exists());
        
    }

}
