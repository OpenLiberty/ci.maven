/*******************************************************************************
 * (c) Copyright IBM Corporation 2023.
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
package mypkg.utils;

import java.io.File;

import org.junit.Assert;
import org.w3c.dom.Node;

public class LooseConfigUtils {

    public static void validateSrcMainWebAppRoot(Node archiveElem) {
        // validate:
        //    <dir sourceOnDisk="...\src\main\webapp" targetInArchive="/"/>
        String s1 = archiveElem.getAttributes().getNamedItem("sourceOnDisk").getNodeValue();
        Assert.assertTrue("Bad node: " + s1, s1.endsWith("src" + File.separator + "main" + File.separator + "webapp"));
        String t1 = archiveElem.getAttributes().getNamedItem("targetInArchive").getNodeValue();
        Assert.assertEquals("Bad node: " + t1, "/", t1);
    }
    
    public static void validateSrcResourceRoot(Node archiveElem, String resourcePath) {

        // validate:
        //   <dir sourceOnDisk="...\target\it\deploy-loose-config-exploded-it\<resourcePath>" targetInArchive="/"/>
        String s1 = archiveElem.getAttributes().getNamedItem("sourceOnDisk").getNodeValue();
        Assert.assertTrue("Bad node: " + s1, s1.endsWith(resourcePath));
        String t1 = archiveElem.getAttributes().getNamedItem("targetInArchive").getNodeValue();
        Assert.assertEquals("Bad node: " + t1, "/", t1);
    }
    
    public static void validateWebAppDirRoot(Node archiveElem, String webAppName) {

        // validate:
        //     <dir sourceOnDisk="...\target\deploy-loose-config-exploded-it-1.0-SNAPSHOT" targetInArchive="/"/>
        String s1 = archiveElem.getAttributes().getNamedItem("sourceOnDisk").getNodeValue();
        Assert.assertTrue("Bad node: " + s1, s1.endsWith(webAppName));
        String t1 = archiveElem.getAttributes().getNamedItem("targetInArchive").getNodeValue();
        Assert.assertEquals("Bad node: " + t1, "/", t1);
    }
    
    // validate: 
    //    <dir sourceOnDisk="...\target\classes" targetInArchive="/WEB-INF/classes"/>
    public static void validateTargetClasses(Node archiveElem) {
        String s1 = archiveElem.getAttributes().getNamedItem("sourceOnDisk").getNodeValue();
        Assert.assertTrue("Bad node: " + s1, s1.endsWith("target" + File.separator + "classes"));
        String t1 = archiveElem.getAttributes().getNamedItem("targetInArchive").getNodeValue();
        Assert.assertEquals("Bad node: " + t1, "/WEB-INF/classes", t1);
    }
}
