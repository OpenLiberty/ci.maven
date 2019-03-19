/*******************************************************************************
 * (c) Copyright IBM Corporation 2017.
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
package wasdev.sample.adapter.helloworld;

import javax.resource.ResourceException;
import javax.resource.cci.ConnectionMetaData;

public class HelloWorldConnectionMetaDataImpl implements ConnectionMetaData {

	private static final String PRODUCT_NAME = "Hello World EIS";
	private static final String PRODUCT_VERSION = "1.0";
	private static final String USER_NAME = "Not applicable";

	/**
	 * Constructor for HelloWorldConnectionMetaDataImpl
	 */
	public HelloWorldConnectionMetaDataImpl() {
		
		super();
	}

	/**
	 * @see ConnectionMetaData#getEISProductName()
	 */
	public String getEISProductName() throws ResourceException {
		
		return PRODUCT_NAME;
	}

	/**
	 * @see ConnectionMetaData#getEISProductVersion()
	 */
	public String getEISProductVersion() throws ResourceException {
		
		return PRODUCT_VERSION;
	}

	/**
	 * @see ConnectionMetaData#getUserName()
	 */
	public String getUserName() throws ResourceException {

		return USER_NAME;
	}

}

