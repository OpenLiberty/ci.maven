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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

import org.apache.maven.shared.utils.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import io.openliberty.tools.maven.server.DevMojo;

public class MultiModuleSkipUnitTest extends BaseMultiModuleTest {

   @Test
   public void getFirstNonNullValueTest() throws Exception {
      assertTrue(DevMojo.getFirstNonNullValue(null, null, Boolean.valueOf(true)));
      assertTrue(DevMojo.getFirstNonNullValue(null, Boolean.valueOf(true), Boolean.valueOf(false)));
      assertFalse(DevMojo.getFirstNonNullValue(null, Boolean.valueOf(false), Boolean.valueOf(true)));
      assertFalse(DevMojo.getFirstNonNullValue(Boolean.valueOf(false), Boolean.valueOf(true), Boolean.valueOf(false)));
      assertTrue(DevMojo.getFirstNonNullValue(Boolean.valueOf(true), Boolean.valueOf(true), Boolean.valueOf(false)));
      assertFalse(DevMojo.getFirstNonNullValue(null, null, null));
   }

   @Test
   public void getBooleanFlagTest() throws Exception {
      testFlag("skipTests");
      testFlag("skipITs");
      testFlag("skipUTs");
   }

   private void testFlag(String param) {
      assertTrue(processFlagsInDevMojo(null, null, Boolean.valueOf(true), param));
      assertTrue(processFlagsInDevMojo(null, Boolean.valueOf(true), Boolean.valueOf(false), param));
      assertFalse(processFlagsInDevMojo(null, Boolean.valueOf(false), Boolean.valueOf(true), param));
      assertFalse(processFlagsInDevMojo(Boolean.valueOf(false), Boolean.valueOf(true), Boolean.valueOf(false), param));
      assertTrue(processFlagsInDevMojo(Boolean.valueOf(true), Boolean.valueOf(true), Boolean.valueOf(false), param));
      assertFalse(processFlagsInDevMojo(null, null, null, param));
   }

   private boolean processFlagsInDevMojo(Boolean configBool, Boolean userPropBool, Boolean propBool, String param) {
      Xpp3Dom dom = new Xpp3Dom("configuration");
      if (configBool != null) {
         Xpp3Dom child = new Xpp3Dom(param);
         dom.addChild(child);
         child.setValue(configBool.toString());
      }
      
      Properties userProps = new Properties();
      if (userPropBool != null) {
         userProps.setProperty(param, userPropBool.toString());
      }

      Properties props = new Properties();
      if (propBool != null) {
         props.setProperty(param, propBool.toString());
      }

      return DevMojo.getBooleanFlag(dom, userProps, props, param);
   }

}

