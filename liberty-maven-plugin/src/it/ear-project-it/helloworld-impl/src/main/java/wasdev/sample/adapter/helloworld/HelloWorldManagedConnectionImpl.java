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

import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Vector;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

public class HelloWorldManagedConnectionImpl implements ManagedConnection {

	private static final String TRANSACTIONS_NOT_SUPPORTED_ERROR =
		"Transactions not supported";

	private HelloWorldConnectionImpl connection;
	private Vector listeners = new Vector();
	private PrintWriter out;

	/**
	 * Constructor for HelloWorldManagedConnectionImpl
	 */
	public HelloWorldManagedConnectionImpl() {

		super();
	}

	public void close() {

		Enumeration list = listeners.elements();
		ConnectionEvent event =
			new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
		event.setConnectionHandle(connection);
		while (list.hasMoreElements()) {
			((ConnectionEventListener) list.nextElement()).connectionClosed(event);
		}
	}

	/**
	 * @see ManagedConnection#getConnection(Subject, ConnectionRequestInfo)
	 */
	public Object getConnection(
		Subject subject,
		ConnectionRequestInfo cxRequestInfo)
		throws ResourceException {

		connection = new HelloWorldConnectionImpl(this);
		return connection;
	}

	/**
	 * @see ManagedConnection#destroy()
	 */
	public void destroy() throws ResourceException {

		connection.invalidate();
		connection = null;
		listeners = null;
	}

	/**
	 * @see ManagedConnection#cleanup()
	 */
	public void cleanup() throws ResourceException {

		connection.invalidate();
	}

	/**
	 * @see ManagedConnection#associateConnection(Object)
	 */
	public void associateConnection(Object connection) throws ResourceException {
	}

	/**
	 * @see ManagedConnection#addConnectionEventListener(ConnectionEventListener)
	 */
	public void addConnectionEventListener(ConnectionEventListener listener) {

		listeners.add(listener);
	}

	/**
	 * @see ManagedConnection#removeConnectionEventListener(ConnectionEventListener)
	 */
	public void removeConnectionEventListener(ConnectionEventListener listener) {

		listeners.remove(listener);
	}

	/**
	 * @see ManagedConnection#getXAResource()
	 */
	public XAResource getXAResource() throws ResourceException {

		throw new NotSupportedException(TRANSACTIONS_NOT_SUPPORTED_ERROR);
	}

	/**
	 * @see ManagedConnection#getLocalTransaction()
	 */
	public LocalTransaction getLocalTransaction() throws ResourceException {

		throw new NotSupportedException(TRANSACTIONS_NOT_SUPPORTED_ERROR);
	}

	/**
	 * @see ManagedConnection#getMetaData()
	 */
	public ManagedConnectionMetaData getMetaData() throws ResourceException {

		return new HelloWorldManagedConnectionMetaDataImpl(connection.getMetaData());
	}

	/**
	 * @see ManagedConnection#setLogWriter(PrintWriter)
	 */
	public void setLogWriter(PrintWriter out) throws ResourceException {

		this.out = out;
	}

	/**
	 * @see ManagedConnection#getLogWriter()
	 */
	public PrintWriter getLogWriter() throws ResourceException {

		return out;
	}

}

