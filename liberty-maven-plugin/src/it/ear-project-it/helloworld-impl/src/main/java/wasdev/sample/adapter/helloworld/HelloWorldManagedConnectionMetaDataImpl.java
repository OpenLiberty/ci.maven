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
import javax.resource.spi.ManagedConnectionMetaData;

public class HelloWorldManagedConnectionMetaDataImpl
	implements ManagedConnectionMetaData {

	private static final int MAX_CONNECTIONS = 1;

	private ConnectionMetaData cxMetaData;

	/**
	 * Constructor for HelloWorldManagedConnectionMetaDataImpl
	 */
	public HelloWorldManagedConnectionMetaDataImpl(ConnectionMetaData cxMetaData) {

		super();
		this.cxMetaData = cxMetaData;
	}

	/**
	 * @see ManagedConnectionMetaData#getEISProductName()
	 */
	public String getEISProductName() throws ResourceException {

		return cxMetaData.getEISProductName();
	}

	/**
	 * @see ManagedConnectionMetaData#getEISProductVersion()
	 */
	public String getEISProductVersion() throws ResourceException {

		return cxMetaData.getEISProductVersion();
	}

	/**
	 * @see ManagedConnectionMetaData#getMaxConnections()
	 */
	public int getMaxConnections() throws ResourceException {

		return MAX_CONNECTIONS;
	}

	/**
	 * @see ManagedConnectionMetaData#getUserName()
	 */
	public String getUserName() throws ResourceException {

		return cxMetaData.getUserName();
	}

}

