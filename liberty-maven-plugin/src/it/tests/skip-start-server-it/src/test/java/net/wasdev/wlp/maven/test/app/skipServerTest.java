package net.wasdev.wlp.maven.test.app;

import static org.junit.Assert.*;

import java.net.Socket;
import org.junit.Test;

/**
 * 
 * Web application test case
 * 
 */

public class skipServerTest {

    private String host = "localhost";
    private Integer port = 9080;

    @Test
    public void testWAR() throws Exception {
        //boolean listening = serverListening(host, port);
        assertFalse(serverListening(host, port));
    }
    
    private static boolean serverListening(String host, int port)
    {
        Socket s = null;
        try
        {
            s = new Socket(host, port);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
        finally
        {
            if(s != null)
                try {s.close();}
                catch(Exception e){}
        }
    }
}
