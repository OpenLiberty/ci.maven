package net.wasdev.wlp.maven.test.app;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

import net.wasdev.wlp.maven.test.support.HttpUtils;

/**
 * 
 * EAB Tes Case
 * 
 */

public class PluginEBATestIT {

    private String baseURL = "http://localhost:9080/";

    @Test
    public void testWAB() throws Exception {
        URL url = null;
        try {
            url = new URL(baseURL + "test-wab/index.jsp");
            String textToFind = "Liberty";
            Assert.assertTrue(textToFind, HttpUtils.findStringInUrl(url, textToFind));
        } catch (MalformedURLException e) {
            Assert.fail("Fail to access " + url + " caused by " + e.getMessage());
        }
    }
}
