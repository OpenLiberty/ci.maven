package net.wasdev.wlp.maven.test.app;

import java.io.File;
import java.io.FileInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import static org.junit.Assert.*;

public class PluginConfigXmlIT {
    
    public final String CONFIG_XML = "target/liberty-plugin-config.xml";
    public final String CONFIG_VAR_OVERRIDES_XML = "target/liberty/usr/servers/test/configDropins/overrides/liberty-plugin-variable-config.xml";
    public final String CONFIG_VAR_DEFAULTS_XML = "target/liberty/usr/servers/test/configDropins/defaults/liberty-plugin-variable-config.xml";
    public final String TARGET_BOOTSTRAP_PROPERTIES = "target/liberty/usr/servers/test/bootstrap.properties";
    public final String TARGET_JVM_OPTIONS = "target/liberty/usr/servers/test/jvm.options";
    public final String SOURCE_SERVER_ENV = "src/main/liberty/config/server.env";
    public final String TARGET_SERVER_ENV = "target/liberty/usr/servers/test/server.env";
    public final String SOURCE_CONFIG_FILE = "src/test/resources/server.xml";
    public final String TARGET_CONFIG_FILE = "target/liberty/usr/servers/test/server.xml";
    
    @Test
    public void testConfigPropFileExist() throws Exception {
        File f = new File(CONFIG_XML);
        assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
    }
    
    @Test
    public void testConfigVarFilesExist() throws Exception {
        File f = new File(CONFIG_VAR_OVERRIDES_XML);
        assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
        f = new File(CONFIG_VAR_DEFAULTS_XML);
        assertTrue(f.getCanonicalFile() + " doesn't exist", f.exists());
    }
    
    @Test
    public void testServerXmlFileElements() throws Exception {
        File in = new File(CONFIG_XML);
        try (FileInputStream input = new FileInputStream(in);) {
        
            // get input XML Document
            DocumentBuilderFactory inputBuilderFactory = DocumentBuilderFactory.newInstance();
            inputBuilderFactory.setIgnoringComments(true);
            inputBuilderFactory.setCoalescing(true);
            inputBuilderFactory.setIgnoringElementContentWhitespace(true);
            inputBuilderFactory.setValidating(false);
            DocumentBuilder inputBuilder = inputBuilderFactory.newDocumentBuilder();
            Document inputDoc = inputBuilder.parse(input);

            // parse input XML Document
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = "/liberty-plugin-config/configFile";
            NodeList nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
            assertEquals("Number of configFile element ==>", 1, nodes.getLength());
                
            expression = "/liberty-plugin-config/configFile/text()";
            String nodeValue = (String) xPath.compile(expression).evaluate(inputDoc, XPathConstants.STRING);
            File f1 = new File(SOURCE_CONFIG_FILE);
            File f2 = new File(nodeValue);
            assertEquals("configFile value", f1.getAbsolutePath(), f2.getAbsolutePath());
            assertEquals("verify target server.xml", FileUtils.fileRead(f2),
                    FileUtils.fileRead(TARGET_CONFIG_FILE));
        }
    }
    
    @Test
    public void testBootstrapPropertiesFileElements() throws Exception {
        File in = new File(CONFIG_XML);
        try (FileInputStream input = new FileInputStream(in);) {
        
            // get input XML Document
            DocumentBuilderFactory inputBuilderFactory = DocumentBuilderFactory.newInstance();
            inputBuilderFactory.setIgnoringComments(true);
            inputBuilderFactory.setCoalescing(true);
            inputBuilderFactory.setIgnoringElementContentWhitespace(true);
            inputBuilderFactory.setValidating(false);
            DocumentBuilder inputBuilder = inputBuilderFactory.newDocumentBuilder();
            Document inputDoc = inputBuilder.parse(input);
            
            // parse input XML Document
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = "/liberty-plugin-config/bootstrapPropertiesFile";
            NodeList nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
            assertEquals("Number of bootstrapPropertiesFile element ==>", 0, nodes.getLength());
            
            expression = "/liberty-plugin-config/bootstrapProperties";
            nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
            assertEquals("Number of bootstrapProperties element ==>", 1, nodes.getLength());
            
            String fileContents = FileUtils.fileRead(TARGET_BOOTSTRAP_PROPERTIES).replaceAll("\r","");

            String[] fileContentsArray = fileContents.split("\\n");
            assertTrue("fileContents", fileContentsArray.length == 4);

            boolean someBootstrapVarFound = false;
            boolean locationFound = false;
            boolean someUndefinedBootstrapVarFound = false;

            for (int i=0; i < fileContentsArray.length; i++) {
                String nextLine = fileContentsArray[i];
                if (i == 0) {
                    assertTrue("comment not found on first line", nextLine.equals("# Generated by liberty-maven-plugin"));
                } else {
                    if (nextLine.equals("someBootstrapVar=someBootstrapValue")) {
                        someBootstrapVarFound = true;
                    } else if (nextLine.equals("location=pom.xml")) {
                        locationFound = true;
                    } else if (nextLine.equals("someUndefinedBootstrapVar=@{undefinedValue}")) {
                        someUndefinedBootstrapVarFound = true;
                    }
                }
            }

            assertTrue("someBootstrapVar=someBootstrapValue not found", someBootstrapVarFound);
            assertTrue("location=pom.xml not found", locationFound);
            assertTrue("someUndefinedBootstrapVar=@{undefinedValue} not found", someUndefinedBootstrapVarFound);
        }
    }
    
    @Test
    public void testJvmOptionsFileElements() throws Exception {
        File in = new File(CONFIG_XML);
        try (FileInputStream input = new FileInputStream(in);) {
        
            // get input XML Document
            DocumentBuilderFactory inputBuilderFactory = DocumentBuilderFactory.newInstance();
            inputBuilderFactory.setIgnoringComments(true);
            inputBuilderFactory.setCoalescing(true);
            inputBuilderFactory.setIgnoringElementContentWhitespace(true);
            inputBuilderFactory.setValidating(false);
            DocumentBuilder inputBuilder = inputBuilderFactory.newDocumentBuilder();
            Document inputDoc = inputBuilder.parse(input);
            
            // parse input XML Document
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = "/liberty-plugin-config/jvmOptionsFile";
            NodeList nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
            assertEquals("Number of jvmOptionsFile element ==>", 0, nodes.getLength());
            
            expression = "/liberty-plugin-config/jvmOptions";
            nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
            assertEquals("Number of jvmOptions element ==>", 1, nodes.getLength());
            
            String fileContents = FileUtils.fileRead(TARGET_JVM_OPTIONS).replaceAll("\r","");

            String[] fileContentsArray = fileContents.split("\\n");
            assertTrue("fileContentsArray.length="+fileContentsArray.length+" fileContents: "+fileContents, fileContentsArray.length == 9);

            boolean myArgLineValueFound = false;
            boolean myUndefinedVarFound = false;
            boolean myXms512mFound = false;
            boolean myXms1024mFound = false;    // should NOT be present
            boolean myXmx1024mFound = false;
            boolean myXmx2056mFound = false;    // should appear before -Xmx1024m
            boolean myJvmPropFoundAbc = false;  // should only be found once (had dup values specified)
            boolean myJvmProp2 = false;         // should only be found once (had dup values specified)
            boolean myDebugPropFound = false;   // should only be found once, but was specified 3 different ways

            // Verify file contents appear in this overall order (but order within a section is not guaranteed). 
            // There should be NO duplicates.

            // Maven project properties:
            // -javaagent:/path/to/some/jar.jar
            // @{undefined}
            // -Xmx2056m

            // System properties:
            // -Xms512m (overrode project property with same name)
            // -Dmy.jvm.prop=abc (overrode project property that had dup value)

            // jvmOptions
            // -Xmx1024m (from jvmOptions parameter)
            // -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=7777. (from jvmOptions parameter that had dup value of both a System prop and Maven project prop)
            // -Dmy.jvm.prop2=This is the value (from jvmOptions parameter specified in both parent and project pom.xml - should only appear once)

            for (int i=0; i < fileContentsArray.length; i++) {
                String nextLine = fileContentsArray[i];
                // verify that -Xmx1024m comes after -Xmx2056m and -Xms512m in the jvm.options file, and that -Xms1024m is not included.
                if (i == 0) {
                    assertTrue("comment not found on first line", nextLine.equals("# Generated by liberty-maven-plugin"));
                } else if (i >= 6) {  // verify jvmOptions present
                    assertTrue("Expected jvmOptions not found: "+nextLine,
                        nextLine.equals("-Xmx1024m") || 
                        nextLine.equals("-agentlib:jdwp=transport=dt_socket;server=y;suspend=n;address=7777") ||
                        nextLine.equals("-Dmy.jvm.prop2=This is the value"));

                    if (nextLine.equals("-Xmx1024m")) {
                        assertFalse("jvm option found more than once: "+nextLine,myXmx1024mFound);
                        myXmx1024mFound = true;
                    } else if (nextLine.equals("-agentlib:jdwp=transport=dt_socket;server=y;suspend=n;address=7777")) {
                        assertFalse("jvm option found more than once: "+nextLine,myDebugPropFound);
                        myDebugPropFound = true;
                    } else {
                        assertFalse("jvm option found more than once: "+nextLine,myJvmProp2);
                        myJvmProp2 = true;
                    }
                } else if (i >= 4) {  // verify System properties present
                        assertTrue("Expected System properties not found: "+nextLine,
                        nextLine.equals("-Xms512m") || 
                        nextLine.equals("-Dmy.jvm.prop=abc"));

                    if (nextLine.equals("-Xms512m")) {
                        assertFalse("jvm option found more than once: "+nextLine,myXms512mFound);
                        myXms512mFound = true;
                    } else {
                        assertFalse("jvm option found more than once: "+nextLine,myJvmPropFoundAbc);
                        myJvmPropFoundAbc = true;
                    }
                } else {  // verify Maven project properties present
                    assertTrue("Expected jvmOptions not found: "+nextLine,
                        nextLine.equals("-javaagent:/path/to/some/jar.jar") || 
                        nextLine.equals("@{undefined}") ||
                        nextLine.equals("-Xmx2056m"));

                    if (nextLine.equals("-javaagent:/path/to/some/jar.jar")) {
                        assertFalse("jvm option found more than once: "+nextLine,myArgLineValueFound);
                        myArgLineValueFound = true;
                    } else if (nextLine.equals("@{undefined}")) {
                        assertFalse("jvm option found more than once: "+nextLine,myUndefinedVarFound);
                        myUndefinedVarFound = true;
                    } else {
                        assertFalse("jvm option found more than once: "+nextLine,myXmx2056mFound);
                        myXmx2056mFound = true;
                    }
                }
            }
            assertTrue("-Xmx1024m not found", myXmx1024mFound);
            assertTrue("-Dmy.jvm.prop2=This is the value - not found", myJvmProp2);
            assertTrue("-agentlib:jdwp=transport=dt_socket;server=y;suspend=n;address=7777 not found", myDebugPropFound);

            assertTrue("-Xms512m not found", myXms512mFound);
            assertTrue("-Dmy.jvm.prop=abc", myJvmPropFoundAbc);

            assertTrue("-javaagent:/path/to/some/jar.jar not found", myArgLineValueFound);
            assertTrue("@{undefined} not found", myUndefinedVarFound);
            assertTrue("-Xmx2056m not found", myXmx2056mFound);
        }
    }

    @Test
    public void testServerEnvElements() throws Exception {
        File in = new File(CONFIG_XML);
        try (FileInputStream input = new FileInputStream(in);) {
        
            // get input XML Document
            DocumentBuilderFactory inputBuilderFactory = DocumentBuilderFactory.newInstance();
            inputBuilderFactory.setIgnoringComments(true);
            inputBuilderFactory.setCoalescing(true);
            inputBuilderFactory.setIgnoringElementContentWhitespace(true);
            inputBuilderFactory.setValidating(false);
            DocumentBuilder inputBuilder = inputBuilderFactory.newDocumentBuilder();
            Document inputDoc = inputBuilder.parse(input);
            
            // parse input XML Document
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = "/liberty-plugin-config/serverEnv";
            NodeList nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
            assertEquals("Number of serverEnv element ==>", 0, nodes.getLength());
                    
            assertEquals("verify target server server.env", "# Generated by liberty-maven-plugin\nJAVA_HOME=/opt/ibm/java\n",
                    FileUtils.fileRead(TARGET_SERVER_ENV).replaceAll("\r",""));
        }
    }

    @Test
    public void testServerConfigDropinsOverridesVarElements() throws Exception {
        File in = new File(CONFIG_VAR_OVERRIDES_XML);
        FileInputStream input = new FileInputStream(in);
        
        // get input XML Document
        DocumentBuilderFactory inputBuilderFactory = DocumentBuilderFactory.newInstance();
        inputBuilderFactory.setIgnoringComments(true);
        inputBuilderFactory.setCoalescing(true);
        inputBuilderFactory.setIgnoringElementContentWhitespace(true);
        inputBuilderFactory.setValidating(false);
        DocumentBuilder inputBuilder = inputBuilderFactory.newDocumentBuilder();
        Document inputDoc = inputBuilder.parse(input);
        
        // parse input XML Document
        XPath xPath = XPathFactory.newInstance().newXPath();
        String expression = "/server/variable";
        NodeList nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
        assertEquals("Number of variable elements ==>", 3, nodes.getLength());

        // iterate through nodes and verify their values
        for (int i=0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            String varName = el.getAttribute("name");
            String varValue = el.getAttribute("value");

            switch (varName) {
                case "someVariable1":
                            assertEquals("Unexpected variable value for "+varName,"someValue1",varValue);
                            break;
                case "someVariable2":
                            assertEquals("Unexpected variable value for "+varName,"myCmdLineValue",varValue);
                            break;
                case "new.CmdLine.Var":
                            assertEquals("Unexpected variable value for "+varName,"newCmdLineValue",varValue);
                            break;
                default: fail("Found unexpected variable in generated configDropins/overrides/liberty-plugin-variable-config.xml file.");
            }
 
        }
                
    }

    @Test
    public void testServerConfigDropinsDefaultsVarElements() throws Exception {
        File in = new File(CONFIG_VAR_DEFAULTS_XML);
        FileInputStream input = new FileInputStream(in);
        
        // get input XML Document
        DocumentBuilderFactory inputBuilderFactory = DocumentBuilderFactory.newInstance();
        inputBuilderFactory.setIgnoringComments(true);
        inputBuilderFactory.setCoalescing(true);
        inputBuilderFactory.setIgnoringElementContentWhitespace(true);
        inputBuilderFactory.setValidating(false);
        DocumentBuilder inputBuilder = inputBuilderFactory.newDocumentBuilder();
        Document inputDoc = inputBuilder.parse(input);
        
        // parse input XML Document
        XPath xPath = XPathFactory.newInstance().newXPath();
        String expression = "/server/variable";
        NodeList nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
        assertEquals("Number of variable elements ==>", 3, nodes.getLength());

        // iterate through nodes and verify their values
        for (int i=0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            String varName = el.getAttribute("name");
            String varDefaultValue = el.getAttribute("defaultValue");

            switch (varName) {
                case "someDefaultVar1":
                            assertEquals("Unexpected variable value for "+varName,"someDefaultValue1", varDefaultValue);
                            break;
                case "someDefaultVar2":
                            assertEquals("Unexpected variable value for "+varName,"someDefaultValue2", varDefaultValue);
                            break;
                case "someVariable1":
                            assertEquals("Unexpected variable value for "+varName,"ignoredDefaultValue", varDefaultValue);
                            break;
                default: fail("Found unexpected variable in generated configDropins/defaults/liberty-plugin-variable-config.xml file.");
            }
 
        }
                
    }

}
