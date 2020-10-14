package net.wasdev.wlp.maven.test.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * 
 * 
 */

public class IncludeBootStrapPathTest {

    public final String MESSAGES_LOG = "liberty/usr/servers/test/logs/messages.log";
    public final String INCLUDE_REGEX_MESSAGE = ".* CWWKG0028A: Processing included configuration resource: .*/target/liberty/usr/servers/test/test_resources_[123]\\.xml";

    @Test
    public void testConfigPropFileExist() throws Exception {
        File f = new File(MESSAGES_LOG);
        Assert.assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
    }
    
    @Test
    public void testXmlElements() throws Exception {
    	File messagesLog = new File(MESSAGES_LOG);
                
        InputStream serverOutput = null;
        InputStreamReader in = null;
        Scanner s = null;
        List<String> matches = null;

        try {
            // Read file and search
            serverOutput = new FileInputStream(messagesLog);
            in = new InputStreamReader(serverOutput);
            s = new Scanner(in);

            String foundString = null;
            Pattern pattern = Pattern.compile(INCLUDE_REGEX_MESSAGE);

            matches = new ArrayList<String>();
            while (s.hasNextLine()) {
                String line = s.nextLine();
                if (pattern.matcher(line).find()) {
                    foundString = line;
                    matches.add(line);
                }
            }
        } catch (Exception e) {

        }
        s.close(); 
        serverOutput.close();
        in.close();

        Assert.assertTrue(matches.size() == 3);

        String datasources_1 = matches.get(0);
        String datasources_2 = matches.get(1);
        String datasources_3 = matches.get(2);

        Assert.assertTrue("test_resources wasn't resolved correctly", datasources_1.endsWith("test_resources_1.xml"));
        Assert.assertTrue("test_resources_2 wasn't resolved correctly", datasources_2.endsWith("test_resources_2.xml"));
        Assert.assertTrue("test_resources_3 wasn't resolved correctly", datasources_3.endsWith("test_resources_3.xml"));
        

    }
    
}