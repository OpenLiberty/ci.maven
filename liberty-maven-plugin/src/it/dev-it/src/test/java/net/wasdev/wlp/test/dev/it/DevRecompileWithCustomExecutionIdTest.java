/*******************************************************************************
 * (c) Copyright IBM Corporation 2025.
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

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static io.openliberty.tools.common.plugins.util.BinaryScannerUtil.*;

public class DevRecompileWithCustomExecutionIdTest extends BaseDevTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        setUpBeforeClass(null, "../resources/basic-dev-project-with-execution-id", false, false, null, null);
        startProcess(null, false, "mvn compile liberty:", false);
    }

    @AfterClass
    public static void cleanUpAfterClass() throws Exception {
        BaseDevTest.cleanUpAfterClass(false, false);
    }

    @Test
    public void validateRunExecutionNotSkipped() throws Exception {
        //java-compile is the custom execution id
        assertTrue(verifyLogMessageExists("Running maven-compiler-plugin:compile#java-compile", 120000));
        assertTrue(verifyLogMessageExists("Nothing to compile - all classes are up to date.", 120000));
    }
}