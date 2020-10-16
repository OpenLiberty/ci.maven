package net.wasdev.wlp.maven.test.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import java.nio.file.Path;
import java.nio.file.Paths;
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

    public final String MESSAGES_LOG = "liberty/wlp/usr/servers/test/logs/messages.log";
    public final String INCLUDE_REGEX_MESSAGE = ".* CWWKG0028A: Processing included configuration resource: .*/|\\\\target/|\\\\liberty/|\\\\usr/|\\\\servers/|\\\\test/|\\\\test_resources_[123].xml";
    public final String FEATURE_INSTALL_MESSAGE = ".* CWWKF0012I: The server installed the following features:.*\\.";
    public final String INCLUDE_FAIL_MESSAGE = ".* CWWKG0090E: The.+bogusVar.+configuration resource does not exist or cannot be read\\.";

    @Test
    public void testMessagesLogFileExist() throws Exception {
        File f = new File(MESSAGES_LOG);
        Assert.assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
    }
    
    @Test
    public void checkMessagesLogForIncludes() throws Exception {
    	File messagesLog = new File(MESSAGES_LOG);
                
        InputStream serverOutput = null;
        InputStreamReader in = null;
        Scanner s = null;
        List<String> includeMatches = new ArrayList<String>();
        List<String> featureMatches = new ArrayList<String>();
        List<String> includeFailMatches = new ArrayList<String>();

        try {
            // Read file and search
            serverOutput = new FileInputStream(messagesLog);
            in = new InputStreamReader(serverOutput);
            s = new Scanner(in);

            String foundString = null;
            Pattern pattern1 = Pattern.compile(INCLUDE_REGEX_MESSAGE);
            Pattern pattern2 = Pattern.compile(FEATURE_INSTALL_MESSAGE);
            Pattern pattern3 = Pattern.compile(INCLUDE_FAIL_MESSAGE);

            while (s.hasNextLine()) {
                String line = s.nextLine();
                if (pattern1.matcher(line).find()) {
                    includeMatches.add(line);
                } else if (pattern2.matcher(line).find()) {
                    featureMatches.add(line);
                } else if (pattern3.matcher(line).find()) {
                    includeFailMatches.add(line);
                }
            }
        } catch (Exception e) {

        }
        s.close(); 
        serverOutput.close();
        in.close();

        Assert.assertTrue("Did not find all test_resources in messages.log", includeMatches.size() == 3);
        Assert.assertTrue("Did not find installed features message in messages.log", featureMatches.size() == 1);
        Assert.assertTrue("Did not find failed include message in messages.log", includeFailMatches.size() == 1);

        String datasources_1 = includeMatches.get(0);
        String datasources_2 = includeMatches.get(1);
        String datasources_3 = includeMatches.get(2);

        Assert.assertTrue("test_resources_1 wasn't resolved correctly", datasources_1.endsWith("test_resources_1.xml"));
        Assert.assertTrue("test_resources_2 wasn't resolved correctly", datasources_2.endsWith("test_resources_2.xml"));
        Assert.assertTrue("test_resources_3 wasn't resolved correctly", datasources_3.endsWith("test_resources_3.xml"));
        
        String featureMatch = featureMatches.get(0);

        // CWWKF0012I: The server installed the following features: [mpConfig-1.3, mongodb-2.0].
        Assert.assertTrue("The mpConfig-1.3 feature was not installed", featureMatch.contains("mpConfig-1.3"));
        Assert.assertTrue("The mongodb-2.0 feature was not installed", featureMatch.contains("mongodb-2.0"));

    }
    
}