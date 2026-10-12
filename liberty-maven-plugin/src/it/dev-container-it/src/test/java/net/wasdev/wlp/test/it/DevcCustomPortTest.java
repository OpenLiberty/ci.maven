/**
 * (C) Copyright IBM Corporation 2026.
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
package net.wasdev.wlp.test.it;

import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies that liberty:devc resolves variable-based httpEndpoint ports from
 * liberty.var.* properties instead of always using the Liberty defaults
 * (9080/9443). Regression test for GH#541.
 *
 * The project under test configures:
 *   liberty.var.default.http.port=9090
 *   liberty.var.default.https.port=9453
 * and server.xml references those via ${default.http.port} / ${default.https.port}.
 * The container command logged by devc must show -p 9090:9090 and -p 9453:9453.
 */
public class DevcCustomPortTest extends BaseDevTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        setUpBeforeClass(null, "../resources/container-custom-port-project", true, false, null, null);
        startProcess("-Dcontainer -Dliberty.dev.podman=true -DcontainerBuildTimeout=599", true);
    }

    @AfterClass
    public static void cleanUpAfterClass() throws Exception {
        BaseDevTest.cleanUpAfterClass();
    }

    @Test
    public void testCustomHttpPortInContainerCommand() throws Exception {
        // The container-side port must be 9090 (resolved from liberty.var.*), not the default 9080.
        // The host-side port may differ if 9090 was already in use, so check for ":9090" only.
        assertTrue("Container command should map to internal HTTP port 9090, not the default 9080: " + getLogTail(),
                verifyLogMessageExists(":9090", 2000));
    }

    @Test
    public void testCustomHttpsPortInContainerCommand() throws Exception {
        // The container-side port must be 9453 (resolved from liberty.var.*), not the default 9443.
        assertTrue("Container command should map to internal HTTPS port 9453, not the default 9443: " + getLogTail(),
                verifyLogMessageExists(":9453", 2000));
    }

    @Test
    public void testServerStarted() throws Exception {
        assertTrue("The application start message is missing: " + getLogTail(),
                verifyLogMessageExists("CWWKZ0001I: Application rest started", 2000));
    }
}
