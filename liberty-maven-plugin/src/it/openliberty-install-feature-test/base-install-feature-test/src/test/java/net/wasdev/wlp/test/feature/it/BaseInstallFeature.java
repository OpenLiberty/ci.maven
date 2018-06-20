/*******************************************************************************
 * (c) Copyright IBM Corporation 2018.
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
package net.wasdev.wlp.test.feature.it;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import static junit.framework.Assert.*;
import org.junit.Before;

public class BaseInstallFeature {

    protected File[] features;
    
    @Before
    public void setUp() throws Exception {
        File dir = new File("liberty/wlp/lib/features");

        features = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".mf");
            }
        });
    }

    protected void assertInstalled(String feature) throws Exception {
        assertTrue("Feature " + feature + " was not installed into the lib/features directory", existsInFeaturesDirectory(feature));
        String featureInfo = getFeatureInfo();
        assertTrue("Feature " + feature + " was not installed according to productInfo featureInfo: " + featureInfo, featureInfo.contains(feature));
    }
    
    protected boolean existsInFeaturesDirectory(String feature) {
        boolean found = false;
        for (File file : features) {
            if (file.getName().endsWith("." + feature + ".mf")) {
                found = true;
                break;
            }
        }
        return found;
    }
    
    protected String getFeatureInfo() throws Exception {
        Process pr = null;
        InputStream is = null;
        Scanner s = null;
        Worker worker = null;
        File installDirectory = new File("liberty", "wlp");
        try {
            String command;
            if (isWindows()) {
                command = installDirectory + "\\bin\\productInfo.bat featureInfo";
            } else {
                command = installDirectory + "/bin/productInfo featureInfo";
            }
            pr = Runtime.getRuntime().exec(command);
            worker = new Worker(pr);
            worker.start();
            worker.join(300000);
            if (worker.exit == null) {
                throw new Exception("productInfo featureInfo error: timeout");
            }
            int exitValue = pr.exitValue();

            is = pr.getInputStream();
            s = new Scanner(is);
            // use regex to match the beginning of the input
            s.useDelimiter("\\A");
            if (s.hasNext()) {
                return s.next();
            } else if (exitValue != 0) {
                throw new Exception("productInfo featureInfo exited with return code " + exitValue);
            }
        } catch (InterruptedException ex) {
            worker.interrupt();
            Thread.currentThread().interrupt();
            throw ex;
        } finally {
            if (s != null) {
                s.close();
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
            if (pr != null) {
                pr.destroy();
            }
        }
        return null;
    }
    
    private static boolean isWindows() {
        String osName = System.getProperty("os.name", "unknown").toLowerCase();
        return osName.indexOf("windows") >= 0;
    }
    
    private static class Worker extends Thread {
        private final Process process;
        private Integer exit;

        private Worker(Process process) {
            this.process = process;
        }

        public void run() {
            try {
                exit = process.waitFor();
            } catch (InterruptedException ignore) {
                return;
            }
        }
    }

}
