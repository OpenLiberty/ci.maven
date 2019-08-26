/**
 * (C) Copyright IBM Corporation 2017.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openliberty.tools.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.plugin.logging.Log;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ServerConfigDocument {

    private static ServerConfigDocument instance;

    private static Log log;

    private static DocumentBuilder docBuilder;

    private static File configDirectory;
    private static File serverXMLFile;

    private static Set<String> locations;
    private static Properties props;

    private static final XPathExpression XPATH_SERVER_APPLICATION;
    private static final XPathExpression XPATH_SERVER_WEB_APPLICATION;
    private static final XPathExpression XPATH_SERVER_ENTERPRISE_APPLICATION;
    private static final XPathExpression XPATH_SERVER_INCLUDE;
    private static final XPathExpression XPATH_SERVER_VARIABLE;

    static {
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            XPATH_SERVER_APPLICATION = xPath.compile("/server/application");
            XPATH_SERVER_WEB_APPLICATION = xPath.compile("/server/webApplication");
            XPATH_SERVER_ENTERPRISE_APPLICATION = xPath.compile("/server/enterpriseApplication");
            XPATH_SERVER_INCLUDE = xPath.compile("/server/include");
            XPATH_SERVER_VARIABLE = xPath.compile("/server/variable");
        } catch (XPathExpressionException ex) {
            // These XPath expressions should all compile statically.
            // Compilation failures mean the expressions are not syntactically
            // correct
            throw new RuntimeException(ex);
        }
    }

    public Set<String> getLocations() {
        return locations;
    }

    public static Properties getProperties() {
        return props;
    }

    private static File getServerXML() {
        return serverXMLFile;
    }

    public ServerConfigDocument(Log log, File serverXML, File configDir, File bootstrapFile,
            Map<String, String> bootstrapProp, File serverEnvFile) {
        initializeAppsLocation(log, serverXML, configDir, bootstrapFile, bootstrapProp, serverEnvFile);
    }

    private static DocumentBuilder getDocumentBuilder() {
        if (docBuilder == null) {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilderFactory.setIgnoringComments(true);
            docBuilderFactory.setCoalescing(true);
            docBuilderFactory.setIgnoringElementContentWhitespace(true);
            docBuilderFactory.setValidating(false);
            try {
                docBuilder = docBuilderFactory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                // fail catastrophically if we can't create a document builder
                throw new RuntimeException(e);
            }
        }
        return docBuilder;
    }

    /**
     * Nulls out cached instance so a new one will be created next time a getInstance() is done.
     * Not thread-safe.
     */
    public static void markInstanceStale() {
        instance = null;
    }
    
    public static ServerConfigDocument getInstance(Log log, File serverXML, File configDir, File bootstrapFile,
            Map<String, String> bootstrapProp, File serverEnvFile) throws IOException {
        // Initialize if instance is not created yet, or source server xml file
        // location has been changed.
        if (instance == null || !serverXML.getCanonicalPath().equals(getServerXML().getCanonicalPath())) {
            instance = new ServerConfigDocument(log, serverXML, configDir, bootstrapFile, bootstrapProp, serverEnvFile);
        }
        return instance;
    }

    private static void initializeAppsLocation(Log log, File serverXML, File configDir, File bootstrapFile,
            Map<String, String> bootstrapProp, File serverEnvFile) {
        try {
            ServerConfigDocument.log = log;
            serverXMLFile = serverXML;
            configDirectory = configDir;

            locations = new HashSet<String>();
            props = new Properties();

            Document doc = parseDocument(new FileInputStream(serverXMLFile));

            // Server variable precedence in ascending order if defined in
            // multiple locations.
            //
            // 1. variables from 'server.env'
            // 2. variables from 'bootstrap.properties'
            // 3. variables defined in <include/> files
            // 4. variables from configDropins/defaults/<file_name>
            // 5. variables defined in server.xml
            // e.g. <variable name="myVarName" value="myVarValue" />
            // 6. variables from configDropins/overrides/<file_name>

            Properties fProps;
            // get variables from server.env
            File cfgDirFile = getFileFromConfigDirectory("server.env");

            if (serverEnvFile != null && serverEnvFile.exists()) {
                fProps = parseProperties(new FileInputStream(serverEnvFile));
                props.putAll(fProps);
            } else if (cfgDirFile != null) {
                fProps = parseProperties(new FileInputStream(cfgDirFile));
                props.putAll(fProps);
            }

            cfgDirFile = getFileFromConfigDirectory("bootstrap.properties");

            if (bootstrapProp != null && !bootstrapProp.isEmpty()) {
                 while (bootstrapProp.values().remove(null))
                     ;
                props.putAll(bootstrapProp);
            } else if (bootstrapFile != null && bootstrapFile.exists()) {
                fProps = parseProperties(new FileInputStream(bootstrapFile));
                props.putAll(fProps);
            } else if (cfgDirFile != null) {
                fProps = parseProperties(new FileInputStream(cfgDirFile));
                props.putAll(fProps);
            }

            parseIncludeVariables(doc);
            parseConfigDropinsDirVariables("defaults");
            parseVariables(doc);
            parseConfigDropinsDirVariables("overrides");

            parseApplication(doc, XPATH_SERVER_APPLICATION);
            parseApplication(doc, XPATH_SERVER_WEB_APPLICATION);
            parseApplication(doc, XPATH_SERVER_ENTERPRISE_APPLICATION);
            parseInclude(doc);
            parseConfigDropinsDir();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void parseApplication(Document doc, XPathExpression expression) throws XPathExpressionException {

        NodeList nodeList = (NodeList) expression.evaluate(doc, XPathConstants.NODESET);

        for (int i = 0; i < nodeList.getLength(); i++) {
            String nodeValue = nodeList.item(i).getAttributes().getNamedItem("location").getNodeValue();

            // add unique values only
            if (!nodeValue.isEmpty()) {
                String resolved = getResolvedVariable(nodeValue);
                if (!locations.contains(resolved)) {
                    locations.add(resolved);
                }
            }
        }
    }

    private static void parseInclude(Document doc) throws XPathExpressionException, IOException, SAXException {
        // parse include document in source server xml
        NodeList nodeList = (NodeList) XPATH_SERVER_INCLUDE.evaluate(doc, XPathConstants.NODESET);

        for (int i = 0; i < nodeList.getLength(); i++) {
            String nodeValue = nodeList.item(i).getAttributes().getNamedItem("location").getNodeValue();

            if (!nodeValue.isEmpty()) {
                Document docIncl = getIncludeDoc(nodeValue);

                if (docIncl != null) {
                    parseApplication(docIncl, XPATH_SERVER_APPLICATION);
                    parseApplication(docIncl, XPATH_SERVER_WEB_APPLICATION);
                    parseApplication(docIncl, XPATH_SERVER_ENTERPRISE_APPLICATION);
                    // handle nested include elements
                    parseInclude(docIncl);
                }
            }
        }
    }

    private static void parseConfigDropinsDir() throws XPathExpressionException, IOException, SAXException {
        File configDropins = getConfigDropinsDir();

        if (configDropins != null && configDropins.exists()) {
            File overrides = new File(configDropins, "overrides");
            if (overrides.exists()) {
                parseDropinsFiles(overrides.listFiles());
            }

            File defaults = new File(configDropins, "defaults");
            if (defaults.exists()) {
                parseDropinsFiles(defaults.listFiles());
            }
        }
    }

    private static void parseDropinsFiles(File[] files) throws XPathExpressionException, IOException, SAXException {
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                parseDropinsFile(files[i]);
            }
        }
    }

    private static Document parseDropinsXMLFile(File file) throws FileNotFoundException, IOException {
        try (FileInputStream is = new FileInputStream(file)) {
            return parseDocument(is);
        } catch (SAXException ex) {
            // If the file was not valid XML, assume it was some other non XML
            // file in dropins.
            log.info("Skipping parsing " + file.getAbsolutePath() + " because it was not recognized as XML.");
            return null;
        }
    }

    private static void parseDropinsFile(File file) throws IOException, XPathExpressionException, SAXException {
        // get input XML Document
        Document doc = parseDropinsXMLFile(file);
        if (doc != null) {
            parseApplication(doc, XPATH_SERVER_APPLICATION);
            parseApplication(doc, XPATH_SERVER_WEB_APPLICATION);
            parseApplication(doc, XPATH_SERVER_ENTERPRISE_APPLICATION);
            parseInclude(doc);
        }
    }

    private static Document getIncludeDoc(String loc) throws IOException, SAXException {

        Document doc = null;
        File locFile = null;

        if (loc.startsWith("http:") || loc.startsWith("https:")) {
            if (isValidURL(loc)) {
                URL url = new URL(loc);
                URLConnection connection = url.openConnection();
                doc = parseDocument(connection.getInputStream());
            }
        } else if (loc.startsWith("file:")) {
            if (isValidURL(loc)) {
                locFile = new File(loc);
                if (locFile.exists()) {
                    InputStream inputStream = new FileInputStream(locFile.getCanonicalPath());
                    doc = parseDocument(inputStream);
                }
            }
        } else if (loc.startsWith("ftp:")) {
            // TODO handle ftp protocol
        } else {
            locFile = new File(loc);

            // check if absolute file
            if (locFile.isAbsolute()) {
                InputStream inputStream = new FileInputStream(locFile.getCanonicalPath());
                doc = parseDocument(inputStream);
            } else {
                // check configDirectory first if exists
                if (configDirectory != null && configDirectory.exists()) {
                    locFile = new File(configDirectory, loc);
                }

                if (locFile == null || !locFile.exists()) {
                    locFile = new File(getServerXML().getParentFile(), loc);
                }

                if (locFile != null && locFile.exists()) {
                    InputStream inputStream = new FileInputStream(locFile.getCanonicalPath());
                    doc = parseDocument(inputStream);
                }
            }
        }
        return doc;
    }

    private static Document parseDocument(InputStream in) throws SAXException, IOException {
        try (InputStream ins = in) { // ins will be auto-closed
            return getDocumentBuilder().parse(ins);
        }
    }

    private static Properties parseProperties(InputStream ins) throws Exception {
        Properties props = null;
        try {
            props = new Properties();
            props.load(ins);
        } catch (Exception e) {
            throw e;
        } finally {
            if (ins != null) {
                ins.close();
            }
        }
        return props;
    }

    private static boolean isValidURL(String url) {
        try {
            URL testURL = new URL(url);
            testURL.toURI();
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    private static String getResolvedVariable(String nodeValue) {
        final String VARIABLE_NAME_PATTERN = "\\$\\{(.*?)\\}";

        String resolved = nodeValue;
        Pattern varNamePattern = Pattern.compile(VARIABLE_NAME_PATTERN);
        Matcher varNameMatcher = varNamePattern.matcher(nodeValue);

        while (varNameMatcher.find()) {
            String variable = getProperties().getProperty(varNameMatcher.group(1));

            if (variable != null && !variable.isEmpty()) {
                resolved = resolved.replaceAll("\\$\\{" + varNameMatcher.group(1) + "\\}", variable);
            }
        }
        return resolved;
    }

    private static void parseVariables(Document doc) throws XPathExpressionException {
        // parse input document
        NodeList nodeList = (NodeList) XPATH_SERVER_VARIABLE.evaluate(doc, XPathConstants.NODESET);

        for (int i = 0; i < nodeList.getLength(); i++) {
            String varName = nodeList.item(i).getAttributes().getNamedItem("name").getNodeValue();
            String varValue = nodeList.item(i).getAttributes().getNamedItem("value").getNodeValue();

            // add unique values only
            if (!varName.isEmpty() && !varValue.isEmpty()) {
                props.put(varName, varValue);
            }
        }
    }

    private static void parseIncludeVariables(Document doc) throws XPathExpressionException, IOException, SAXException {
        // parse include document in source server xml
        NodeList nodeList = (NodeList) XPATH_SERVER_INCLUDE.evaluate(doc, XPathConstants.NODESET);

        for (int i = 0; i < nodeList.getLength(); i++) {
            String nodeValue = nodeList.item(i).getAttributes().getNamedItem("location").getNodeValue();

            if (!nodeValue.isEmpty()) {
                Document docIncl = getIncludeDoc(nodeValue);

                if (docIncl != null) {
                    parseVariables(docIncl);
                    // handle nested include elements
                    parseIncludeVariables(docIncl);
                }
            }
        }
    }

    private static File getConfigDropinsDir() {
        File configDropins = null;
        if (configDropins == null || !configDropins.exists()) {
            configDropins = new File(getServerXML().getParent(), "configDropins");
        }
        return configDropins;
    }

    private static void parseConfigDropinsDirVariables(String inDir)
            throws XPathExpressionException, SAXException, IOException {
        File configDropins = getConfigDropinsDir();

        if (configDropins != null && configDropins.exists()) {
            File dir = new File(configDropins, inDir);

            if (dir.exists()) {
                File[] cfgFiles = dir.listFiles();

                for (int i = 0; i < cfgFiles.length; i++) {
                    if (cfgFiles[i].isFile()) {
                        parseDropinsFilesVariables(cfgFiles[i]);
                    }
                }
            }
        }
    }

    private static void parseDropinsFilesVariables(File file)
            throws SAXException, IOException, XPathExpressionException {
        // get input XML Document
        Document doc = parseDropinsXMLFile(file);
        if (doc != null) {
            parseVariables(doc);
            parseIncludeVariables(doc);
        }
    }

    /*
     * Get the file from configDrectory if it exists; otherwise return def only
     * if it exists, or null if not
     */
    private static File getFileFromConfigDirectory(String file, File def) {
        File f = new File(configDirectory, file);
        if (configDirectory != null && f.exists()) {
            return f;
        }
        if (def != null && def.exists()) {
            return def;
        }
        return null;
    }

    private static File getFileFromConfigDirectory(String file) {
        return getFileFromConfigDirectory(file, null);
    }
}
