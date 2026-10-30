/*******************************************************************************
 * (c) Copyright IBM Corporation 2026.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package net.wasdev.wlp.test.dev.it;

import java.io.File;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Verifies that ~/.m2/toolchains.xml exists and defines the JDK toolchains a
 * toolchain integration test depends on, so a missing prerequisite is reported
 * with an actionable message instead of an opaque test failure.
 */
public final class ToolchainsXmlValidator {

    private static final String README_PATH = "liberty-maven-plugin/src/it/toolchain-run-it/README.md";

    private ToolchainsXmlValidator() {
    }

    public static File getToolchainsFile() {
        return new File(new File(System.getProperty("user.home"), ".m2"), "toolchains.xml");
    }

    /**
     * Fails the test with a descriptive message if toolchains.xml is missing,
     * unreadable, or does not provide a usable JDK toolchain for each version.
     *
     * @param versions JDK versions the calling test requires, for example "11"
     */
    public static void requireJdkToolchains(String... versions) {
        File file = getToolchainsFile();
        if (!file.isFile()) {
            Assert.fail("The toolchain integration tests require " + file.getAbsolutePath()
                    + " but it does not exist. Create it with a jdk toolchain for JDK version(s) "
                    + String.join(", ", versions) + ". See " + README_PATH + " for an example.");
        }

        NodeList toolchains = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document doc = factory.newDocumentBuilder().parse(file);
            toolchains = doc.getElementsByTagName("toolchain");
        } catch (Exception e) {
            Assert.fail("Could not parse " + file.getAbsolutePath() + ": " + e.getMessage()
                    + ". See " + README_PATH + " for the expected format.");
        }

        for (String version : versions) {
            requireJdkToolchain(file, toolchains, version);
        }
    }

    private static void requireJdkToolchain(File file, NodeList toolchains, String version) {
        String invalidJdkHome = null;
        for (int i = 0; i < toolchains.getLength(); i++) {
            Element toolchain = (Element) toolchains.item(i);
            if (!"jdk".equals(text(toolchain, "type")) || !version.equals(text(toolchain, "version"))) {
                continue;
            }
            String jdkHome = text(toolchain, "jdkHome");
            if (jdkHome != null && new File(jdkHome).isDirectory()) {
                return;
            }
            invalidJdkHome = jdkHome;
        }

        if (invalidJdkHome != null) {
            Assert.fail("The jdk toolchain for version " + version + " in " + file.getAbsolutePath()
                    + " has a <jdkHome> that is not a directory: " + invalidJdkHome);
        }
        Assert.fail("No jdk toolchain providing version " + version + " was found in " + file.getAbsolutePath()
                + ". See " + README_PATH + " for an example.");
    }

    private static String text(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0 || nodes.item(0).getTextContent() == null) {
            return null;
        }
        return nodes.item(0).getTextContent().trim();
    }
}
