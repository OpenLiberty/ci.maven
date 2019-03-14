package net.wasdev.wlp.maven.test.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utilities for testing HTTP connections.
 */
public class HttpUtils {
    private final static Log log = LogFactory.getLog(HttpUtils.class);

    /**
     * This method is used to get a connection stream from an HTTP connection. It
     * gives the output from the webpage that it gets from the connection
     * 
     * @param con The connection to the HTTP address
     * @return The Output from the webpage
     */
    public static BufferedReader getConnectionStream(HttpURLConnection con) throws IOException {
        InputStream is = con.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        return br;
    }

    /**
     * This method creates a connection to a webpage and then reutrns the connection
     * 
     * @param url The Http Address to connect to
     * @param expectedResponseCode The expected response code to wait for
     * @param connectionTimeout The timeout in seconds
     * @return The connection to the http address
     */
    public static HttpURLConnection getHttpConnection(URL url, int expectedResponseCode, int connectionTimeout) throws IOException, ProtocolException {
        int count = 0;
        HttpURLConnection con = null;
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                //swallow the InterruptedException if there is one
            }
            con = getHttpConnection(url);
            con.connect();
            count++;
        } while (con.getResponseCode() != expectedResponseCode && count < connectionTimeout);
        return con;
    }

    /**
     * Method to find some text from the output of a URL. If the text isn't found an assertion error is thrown.
     * 
     * @param hostname The liberty server for that is hosting the URL
     * @param path The path to the URL with the output to test (excluding port and server information). For instance "/someContextRoot/servlet1"
     * @param textToFind The text to search for
     * @throws Exception
     * @throws {@link AssertionError} If the text isn't found
     */
    public static boolean findStringInUrl(URL url, String textToFind) throws Exception {

        log.info("Calling application with URL=" + url.toString());
        //check application is installed
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, 5);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        boolean foundText = false;
        String line;
        StringBuilder outputBuilder = new StringBuilder();
        while ((line = br.readLine()) != null) {
            outputBuilder.append(line);
            outputBuilder.append("\n");
            if (line.contains(textToFind)) {
                foundText = true;
                break;
            }
        }
        con.disconnect();
        return foundText;
    }

    /**
     * This gets an HttpURLConnection to the requested address
     * 
     * @param url The URL to get a connection to
     * @return
     * @throws IOException
     * @throws ProtocolException
     */
    private static HttpURLConnection getHttpConnection(URL url) throws IOException, ProtocolException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestMethod("GET");
        return con;
    }

}
