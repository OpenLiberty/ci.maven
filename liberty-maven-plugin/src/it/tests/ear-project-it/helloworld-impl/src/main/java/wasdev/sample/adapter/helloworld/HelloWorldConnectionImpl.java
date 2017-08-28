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

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionMetaData;
import javax.resource.cci.Interaction;
import javax.resource.cci.LocalTransaction;
import javax.resource.cci.ResultSetInfo;
import javax.resource.spi.ManagedConnection;

public class HelloWorldConnectionImpl implements Connection {

	private static final String CLOSED_ERROR = "Connection closed";
	private static final String TRANSACTIONS_NOT_SUPPORTED =
		"Local transactions not supported";
	private static final String RESULT_SETS_NOT_SUPPORTED =
		"Result sets not supported";
	private boolean valid;

	private ManagedConnection mc;

	/**
	 * Constructor for HelloWorldConnectionImpl
	 */
	public HelloWorldConnectionImpl(ManagedConnection mc) {

		super();
		this.mc = mc;
		valid = true;
	}

	void invalidate() {

		mc = null;
		valid = false;
	}

	/**
	 * @see Connection#createInteraction()
	 */
	public Interaction createInteraction() throws ResourceException {

		if (valid) {
			return new HelloWorldInteractionImpl(this);
		} else {
			throw new ResourceException(CLOSED_ERROR);
		}
	}

	/**
	 * @see Connection#getLocalTransaction()
	 */
	public LocalTransaction getLocalTransaction() throws ResourceException {

		throw new NotSupportedException(TRANSACTIONS_NOT_SUPPORTED);
	}

	/**
	 * @see Connection#getMetaData()
	 */
	public ConnectionMetaData getMetaData() throws ResourceException {

		if (valid) {
			return new HelloWorldConnectionMetaDataImpl();
		} else {
			throw new ResourceException(CLOSED_ERROR);
		}
	}

	/**
	 * @see Connection#getResultSetInfo()
	 */
	public ResultSetInfo getResultSetInfo() throws ResourceException {

		throw new NotSupportedException(RESULT_SETS_NOT_SUPPORTED);
	}

	/**
	 * @see Connection#close()
	 */
	public void close() throws ResourceException {

		if (valid) {
			((HelloWorldManagedConnectionImpl) mc).close();
		}
	}

}

