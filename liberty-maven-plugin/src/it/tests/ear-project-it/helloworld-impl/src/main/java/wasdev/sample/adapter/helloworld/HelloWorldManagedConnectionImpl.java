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