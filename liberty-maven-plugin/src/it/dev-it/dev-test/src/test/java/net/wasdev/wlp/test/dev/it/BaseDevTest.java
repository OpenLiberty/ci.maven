/*******************************************************************************
 * (c) Copyright IBM Corporation 2019.
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

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import org.apache.maven.shared.utils.io.FileUtils;
import org.junit.Test;

public class BaseDevTest {

    @Test
    public void basicTest() throws Exception {
        // set up project
        File basicProj = Files.createTempDirectory("test").toFile();
        assertTrue(basicProj.exists());

        File basicDevProj = new File("resources/basic-dev-project");
        assertTrue(basicDevProj.exists());
        
        FileUtils.copyDirectoryStructure(basicDevProj, basicProj);
        assertTrue(basicProj.listFiles().length > 0);

		// run dev mode on project
		ProcessBuilder builder = new ProcessBuilder();
		builder.directory(basicProj);
		String processCommand = "mvn liberty:dev";
		Properties props = System.getProperties();
		Set<Object> keys = props.keySet();
		for (Object key : keys) {
			processCommand += " -D" + key + "=\"" + props.get(key) + "\"";
		}

		String os = System.getProperty("os.name");
		if (os != null && os.toLowerCase().startsWith("windows")) {
			builder.command("CMD", "/C", processCommand);
		} else {
			builder.command("bash", "-c", processCommand);
		}

        try {
            File logFile = new File(basicDevProj, "/logFile.txt");
            Files.write(logFile.toPath(), "".getBytes());
            
            builder.redirectOutput(logFile);
            builder.redirectError(logFile);
            Process process = builder.start();
            assertTrue(process.isAlive());
            
            OutputStream stdin = process.getOutputStream();
            
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
            Thread.sleep(50000); // wait for dev mode to start up
                 
            // shut down dev mode
            writer.write("exit"); // trigger dev mode to shut down
            writer.flush();
            writer.close();
            Thread.sleep(1000); // wait for dev mode to shut down
            
            // test that dev mode has stopped running
            assertTrue(readFile("Server defaultServer stopped.", logFile));
            assertFalse(readFile("Error", logFile));

        } catch (IOException e) {
            assertFalse(true);
        }
    }


    public boolean readFile(String str, File file) throws FileNotFoundException {
        Scanner scanner = new Scanner(file);
        
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.contains(str)){
                return true;
            }
        }
        return false;
    }
}
