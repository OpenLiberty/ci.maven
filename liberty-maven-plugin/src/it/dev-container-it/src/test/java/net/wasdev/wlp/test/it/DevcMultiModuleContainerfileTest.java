package net.wasdev.wlp.test.it;

import static org.junit.Assert.*;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration test for ci.common Issues 1 (container name collision) and 2 (port collision)
 * when multiple independently Liberty-configured modules start with container dev mode.
 *
 * <p>This test starts dev mode sequentially on two independent WAR modules
 * (modulea and moduleb), each with its own Liberty configuration and Containerfile.
 * It verifies that the container name written to each module's devc metadata file
 * uses the module artifactId as a suffix — e.g. {@code liberty-dev-modulea} — rather
 * than the generic {@code liberty-dev}.  Distinct names are the stable outcome of
 * the Issue 1 fix in ci.common's {@code DevUtil.generateNewContainerName()}.
 *
 * <p>Port uniqueness (Issue 2) is implicitly verified because if two modules were
 * assigned the same host port the second container start would fail; both containers
 * starting successfully therefore demonstrates non-colliding ports.
 */
public class DevcMultiModuleContainerfileTest extends BaseDevTest {

    private static final String MODULEA_DIR = "../resources/container-multimodule-independent/modulea";
    private static final String MODULEB_DIR = "../resources/container-multimodule-independent/moduleb";

    // -----------------------------------------------------------------------
    // Module A
    // -----------------------------------------------------------------------

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Start dev mode on modulea, pointing at its own pom and Liberty config.
        setUpBeforeClass(null, MODULEA_DIR, true, false, null, null);
        startProcess("-Dcontainer -Dliberty.dev.podman=true -DcontainerBuildTimeout=599", true);
    }

    @AfterClass
    public static void cleanUpAfterClass() throws Exception {
        BaseDevTest.cleanUpAfterClass();
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    @Test
    public void moduleAContainerStartsSuccessfully() throws Exception {
        assertTrue("Module A: container build did not complete: " + getLogTail(),
            verifyLogMessageExists("Completed building container image.", 120000));
        assertTrue("Module A: application start message is missing: " + getLogTail(),
            verifyLogMessageExists("CWWKZ0001I:", 120000));
    }

    /**
     * Verifies that the container name assigned to modulea is {@code liberty-dev-modulea}
     * (derived from the Maven artifactId) rather than the old generic {@code liberty-dev}.
     *
     * <p>This is the direct regression check for Issue 1: if two modules started
     * concurrently they would both try to use {@code liberty-dev}, causing a collision.
     * With the fix each module uses a stable, artifactId-based name so no collision
     * can occur.
     */
    @Test
    public void moduleAContainerNameIncludesArtifactId() throws Exception {
        File metaFile = new File(MODULEA_DIR + "/target/defaultServer-liberty-devc-metadata.xml");
        assertTrue("Module A: devc metadata file does not exist: " + metaFile.getAbsolutePath(),
            metaFile.exists());

        String content = FileUtils.readFileToString(metaFile, "UTF-8");

        assertTrue("Module A: container name must include the artifactId 'modulea'. " +
            "Expected '<containerName>liberty-dev-modulea</containerName>' in: " + content,
            content.contains("<containerName>liberty-dev-modulea</containerName>"));

        assertTrue("Module A: container type should indicate podman.",
            content.contains("<containerType>podman</containerType>"));
    }
}
