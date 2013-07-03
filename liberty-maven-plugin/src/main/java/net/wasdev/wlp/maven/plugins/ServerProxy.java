package net.wasdev.wlp.maven.plugins;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Liberty server proxy used to operate bundles via JMX
 * 
 * 
 * 
 */
public class ServerProxy {
    private static final Log log = LogFactory.getLog(ServerProxy.class);

    private static final String CONNECTOR_ADDRESS_FILE_NAME = "com.ibm.ws.jmx.local.address";
    private static final String OSGI_FRAMEWORK_MBEAN_NAME = "osgi.core:type=framework,version=1.5";

    protected MBeanServerConnection mbsc = null;

    protected JMXConnector connector = null;

    private JMXServiceURL url = null;

    private File workAreaPath = null;

    private String connectorAddr = null;

    public ServerProxy(File workAreaPath) throws IOException {
        this.workAreaPath = workAreaPath;

    }

    /**
     * Get jmx connector address from the file under workarea folder.
     * 
     * @param serverWorkAreaPath
     * @throws IOException
     */
    private void getJMXConnectorAddress(File serverWorkAreaPath)
                    throws IOException {
        File addressFilePath = new File(serverWorkAreaPath,
                        CONNECTOR_ADDRESS_FILE_NAME);

        if (addressFilePath.exists()) {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(addressFilePath));
                connectorAddr = br.readLine();
            } catch (IOException e) {
                throw e;
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        } else {
            log.error(addressFilePath.getCanonicalPath()
                      + " doesn't exist. Please check jmx feature is eanbled. If not, add <feature>localConnector-1.0</feature> to your server.xml");
        }
    }

    /**
     * Get mbean server connection.
     * 
     * @return
     * @throws IOException
     */
    private MBeanServerConnection getConnection() throws IOException {
        if (mbsc == null) {
            getJMXConnectorAddress(workAreaPath);
            if (connectorAddr != null) {

                url = new JMXServiceURL(connectorAddr);

                connector = JMXConnectorFactory.connect(url);
                mbsc = connector.getMBeanServerConnection();

                log.info("Connected at JMX " + url);
            }
        }

        return mbsc;
    }

    /**
     * 
     * @param objName
     * @param operation
     * @param args
     * @param signature
     * @return
     * @throws Exception
     */
    private Object invoke(ObjectName objName, final String operation,
                          final Object[] args, final String[] signature) throws Exception {
        assert objName != null;
        assert operation != null;

        return getConnection().invoke(objName, operation, args, signature);
    }

    /**
     * 
     * @param operation
     * @param args
     * @param signature
     * @return
     * @throws Exception
     */
    public Object invoke(final String operation, final Object[] args,
                         final String[] signature) throws Exception {
        assert operation != null;

        ObjectName objName = new ObjectName(OSGI_FRAMEWORK_MBEAN_NAME);
        return invoke(objName, operation, args, signature);
    }

    public void disconnect() throws Exception {
        mbsc = null;
        if (connector == null) {
            throw new Exception("JMX Connector is null");
        }
        try {
            connector.close();
        } catch (Exception e) {
            // ignore
        }
        connector = null;
    }

    public void restartOSGiFramework() throws Exception {

        try {
            invoke("restartFramework", null, null);
            log.info("Restart OSGi success");
        } catch (Throwable e) {
            log.error("Failed to restart the server OSGi framework", e);
            throw new Exception("Failed to restart the server OSGi framework");
        } finally {
            disconnect();
        }
    }
}
