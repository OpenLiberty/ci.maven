package net.wasdev.wlp.maven.test.app;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.Assert;
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

public class PluginConfigXmlTest {

    public final String CONFIG_DROPINS_XML="liberty/usr/servers/test/configDropins/defaults/install_apps_configuration_1491924271.xml";

    @Test
    public void testConfigDropinsXMLFileNotExist() throws Exception {
        File f = new File(CONFIG_DROPINS_XML);
        // if the variables are resolved, install_apps_configuration_*.xml won't be generated.
        Assert.assertTrue(f.getCanonicalFile() + " does exist", !f.exists());
    }
}
