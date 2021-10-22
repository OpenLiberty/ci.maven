/*******************************************************************************
 * (c) Copyright IBM Corporation 2021.
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
import java.nio.file.Files;
import java.util.Scanner;

import org.apache.maven.shared.utils.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ExplodedLooseWarAppTest extends BaseDevTest {
       private final String projectArtifact = "exploded-war-proj-1.0-SNAPSHOT";
       private final String appsDir = "target/liberty/wlp/usr/servers/defaultServer/dropins/";

	   @BeforeClass
	   public static void setUpBeforeClass() throws Exception {
	      setUpBeforeClass(null, "../resources/exploded-war-project");
	   }
	
	   @AfterClass
	   public static void cleanUpAfterClass() throws Exception {
	      BaseDevTest.cleanUpAfterClass();
	   }
	
	   @Test
	   public void configureWebXmlFiltering() throws Exception {
	      // Add deployment descriptor filtering config to pom war plugin
		  replaceString("<filteringDeploymentDescriptors>false</filteringDeploymentDescriptors>", 
				  "<filteringDeploymentDescriptors>true</filteringDeploymentDescriptors>", pom);
		  
		  // Verify exploded goal running and redeploy
		  verifyLogMessageExists("Running liberty:deploy", 2000);
		  verifyLogMessageExists("Running maven-war-plugin:exploded", 2000);
		  
		  // Verify loose app xml is correct
		  verifyExplodedLooseApp();
		   
		  // Remove filtering config
		  replaceString("<filteringDeploymentDescriptors>true</filteringDeploymentDescriptors>", 
				  "<filteringDeploymentDescriptors>false</filteringDeploymentDescriptors>", pom);	  
		  
		  // Verify redeploy
		  verifyLogMessageExists("Running liberty:deploy", 2000);
		  
		  // Verify loose app xml is back to how it was
		  verifyNonExplodedLooseApp();
	   }
	   
	   @Test
	   public void configureFilteredResource() throws Exception {
		   // Add filtering config to pom war plugin (directory)
		   replaceString("<!-- Filtered directory start", 
					  "<!-- Filtered directory start -->", pom);
		   
		   replaceString("Filtered directory end -->", 
					  "<!-- Filtered directory end -->", pom);
		   
		   // Verify exploded goal running and redeploy
		   verifyLogMessageExists("Running liberty:deploy", 2000);
		   verifyLogMessageExists("Running maven-war-plugin:exploded", 2000);
		   
		   // Verify loose app xml is correct
		   verifyExplodedLooseApp();   
		   
		   // Remove filtering config
		   replaceString("<!-- Filtered directory start -->", 
					  "<!-- Filtered directory start ", pom);
		   
		   replaceString("<!-- Filtered directory end -->", 
					  "Filtered directory end -->", pom);
		   
		   // Verify redeploy
		   verifyLogMessageExists("Running liberty:deploy", 2000);
		   
		   // Verify loose app xml is back to how it was
		   verifyNonExplodedLooseApp();
	   }
	   
	   @Test
	   public void configureWarOverlay() throws Exception {
		   // Add filtering config to pom war plugin (directory)
		   replaceString("<!-- Overlay configuration start", 
					  "<!-- Overlay configuration start -->", pom);
		   
		   replaceString("Overlay configuration end -->", 
					  "<!-- Overlay configuration end -->", pom);
		   
		   // Verify exploded goal running and redeploy
		   verifyLogMessageExists("Running liberty:deploy", 2000);
		   verifyLogMessageExists("Running maven-war-plugin:exploded", 2000);
		   
		   // Verify loose app xml is correct
		   verifyExplodedLooseApp();   
		   
		   // Remove filtering config
		   replaceString("<!-- Overlay configuration start -->", 
					  "<!-- Overlay configuration start", pom);
		   
		   replaceString("<!-- Overlay configuration end -->", 
					  "Overlay configuration end -->", pom);
		   
		   // Verify redeploy
		   verifyLogMessageExists("Running liberty:deploy", 2000);
		   
		   // Verify loose app xml is back to how it was
		   verifyNonExplodedLooseApp();
	   }
	   
	   private void verifyExplodedLooseApp() throws Exception {
		   String looseAppXml = tempProj.getAbsolutePath() + "/" + appsDir + projectArtifact + ".war.xml";
		   
		   // Verify the target/<projectArtifact> entry
		   String explodedWar = basicDevProj.getAbsolutePath() + "/target/" + projectArtifact;
		   verifyLogMessageExists("<dir sourceOnDisk=\"" + explodedWar + "\" targetInArchive=\"/\"/>", 2000, new File(looseAppXml));
	   }
	   
	   private void verifyNonExplodedLooseApp() throws Exception {
		   String looseAppXml = tempProj.getAbsolutePath() + "/" + appsDir + projectArtifact + ".war.xml";
		   
		   // Verify the src/main/webapp entry
		   String srcMain = basicDevProj.getAbsolutePath() + "/src/main/webapp";
		   verifyLogMessageExists("<dir sourceOnDisk=\"" + srcMain + "\" targetInArchive=\"/\"/>", 2000, new File(looseAppXml));
	       
		   // Verify the target/classes entry
		   String targetClasses = basicDevProj.getAbsolutePath() + "/target/classes";
		   verifyLogMessageExists("<dir sourceOnDisk=\"" + targetClasses + "\" targetInArchive=\"/WEB-INF/classes\"/>", 2000, new File(looseAppXml));
	   }
}
