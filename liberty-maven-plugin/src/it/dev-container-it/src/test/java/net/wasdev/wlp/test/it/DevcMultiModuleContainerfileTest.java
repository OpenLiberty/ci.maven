package net.wasdev.wlp.test.it;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration test for ci.common Issues 1 (container name collision) and 2 (port collision)
 * when multiple independently Liberty-configured modules start with container dev mode.
 *
 * Starts dev mode on two independent WAR modules (modulea and moduleb) concurrently,
 * each with its own Liberty configuration and Containerfile. Verifies that each module
 * receives a unique container name derived from its artifactId and that both servers
 * start successfully (demonstrating non-colliding ports).
 */
public class DevcMultiModuleContainerfileTest extends BaseDevTest {

    private static final String MODULEA_DIR = "../resources/container-multimodule-independent/modulea";
    private static final String MODULEB_DIR = "../resources/container-multimodule-independent/moduleb";

    private static File moduleBLogFile;
    private static File moduleBLogErrorFile;
    private static Process moduleBProcess;
    private static BufferedWriter moduleBWriter;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Start module A via BaseDevTest machinery (sets basicDevProj, logFile, process, writer).
        setUpBeforeClass(null, MODULEA_DIR, true, false, null, null);
        startProcess("-Dcontainer -Dliberty.dev.podman=true -DcontainerBuildTimeout=599", true);

        // Start module B as a second independent process with its own log files.
        File moduleBDir = new File(MODULEB_DIR);
        moduleBLogFile      = new File(moduleBDir, "logFile.txt");
        moduleBLogErrorFile = new File(moduleBDir, "logErrorFile.txt");

        replaceString("SUB_VERSION", System.getProperty("mavenPluginVersion"),
                new File(moduleBDir, "pom.xml"));
        replaceString("RUNTIME_VERSION", System.getProperty("runtimeVersion"),
                new File(moduleBDir, "pom.xml"));

        ProcessBuilder builder = new ProcessBuilder();
        builder.directory(moduleBDir);
        String os = System.getProperty("os.name");
        String command = "mvn liberty:dev -Dcontainer -Dliberty.dev.podman=true -DcontainerBuildTimeout=599";
        if (os != null && os.toLowerCase().startsWith("windows")) {
            builder.command("CMD", "/C", command);
        } else {
            builder.command("bash", "-c", command);
        }
        builder.redirectOutput(moduleBLogFile);
        builder.redirectError(moduleBLogErrorFile);
        moduleBProcess = builder.start();
        assertTrue("Module B process is not alive", moduleBProcess.isAlive());

        OutputStream stdin = moduleBProcess.getOutputStream();
        moduleBWriter = new BufferedWriter(new OutputStreamWriter(stdin));

        assertTrue("Module B: Liberty features not installed: " + getLogTail(moduleBLogFile),
                verifyLogMessageExists("CWWKF0011I", 120000, moduleBLogFile));
        assertTrue("Module B: Liberty not running in dev mode",
                verifyLogMessageExists("Liberty is running in dev mode.", 60000, moduleBLogFile));
    }

    @AfterClass
    public static void cleanUpAfterClass() throws Exception {
        // Stop module B.
        if (moduleBWriter != null) {
            try {
                moduleBWriter.write("exit\n");
                moduleBWriter.flush();
            } catch (IOException e) {
                // best-effort
            } finally {
                try { moduleBWriter.close(); } catch (IOException e) {}
            }
        }
        if (moduleBProcess != null) {
            moduleBProcess.waitFor(120, java.util.concurrent.TimeUnit.SECONDS);
        }
        if (moduleBLogFile != null && moduleBLogFile.exists()) {
            moduleBLogFile.delete();
        }
        if (moduleBLogErrorFile != null && moduleBLogErrorFile.exists()) {
            moduleBLogErrorFile.delete();
        }

        // Stop module A.
        BaseDevTest.cleanUpAfterClass();
    }

    @Test
    public void moduleAContainerStartsSuccessfully() throws Exception {
        assertTrue("Module A: container build did not complete: " + getLogTail(),
                verifyLogMessageExists("Completed building container image.", 2000));
        assertTrue("Module A: application start message is missing: " + getLogTail(),
                verifyLogMessageExists("CWWKZ0001I:", 2000));
    }

    @Test
    public void moduleBContainerStartsSuccessfully() throws Exception {
        assertTrue("Module B: container build did not complete: " + getLogTail(moduleBLogFile),
                verifyLogMessageExists("Completed building container image.", 2000, moduleBLogFile));
        assertTrue("Module B: application start message is missing: " + getLogTail(moduleBLogFile),
                verifyLogMessageExists("CWWKZ0001I:", 2000, moduleBLogFile));
    }

    @Test
    public void moduleAContainerNameIncludesArtifactId() throws Exception {
        File metaFile = new File(MODULEA_DIR + "/target/defaultServer-liberty-devc-metadata.xml");
        assertTrue("Module A: devc metadata file does not exist: " + metaFile.getAbsolutePath(),
                metaFile.exists());

        String content = FileUtils.readFileToString(metaFile, "UTF-8");
        assertTrue("Module A: container name must be 'liberty-dev-modulea' in: " + content,
                content.contains("<containerName>liberty-dev-modulea</containerName>"));
        assertTrue("Module A: container type should indicate podman",
                content.contains("<containerType>podman</containerType>"));
    }

    @Test
    public void moduleBContainerNameIncludesArtifactId() throws Exception {
        File metaFile = new File(MODULEB_DIR + "/target/defaultServer-liberty-devc-metadata.xml");
        assertTrue("Module B: devc metadata file does not exist: " + metaFile.getAbsolutePath(),
                metaFile.exists());

        String content = FileUtils.readFileToString(metaFile, "UTF-8");
        assertTrue("Module B: container name must be 'liberty-dev-moduleb' in: " + content,
                content.contains("<containerName>liberty-dev-moduleb</containerName>"));
        assertTrue("Module B: container type should indicate podman",
                content.contains("<containerType>podman</containerType>"));
    }
}
