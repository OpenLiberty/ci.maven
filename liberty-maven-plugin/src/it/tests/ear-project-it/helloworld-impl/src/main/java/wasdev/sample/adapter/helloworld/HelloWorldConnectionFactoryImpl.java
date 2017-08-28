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

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.RecordFactory;
import javax.resource.cci.ResourceAdapterMetaData;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ManagedConnectionFactory;

public class HelloWorldConnectionFactoryImpl implements ConnectionFactory {

    private static final long serialVersionUID = 638839467578827191L;
    private Reference reference;
	private ConnectionManager cm;
	private ManagedConnectionFactory mcf;

	/**
	 * Constructor for HelloWorldConnectionFactoryImpl
	 */
	public HelloWorldConnectionFactoryImpl(
		ManagedConnectionFactory mcf,
		ConnectionManager cm) {

		super();
		this.mcf = mcf;
		this.cm = cm;
	}

	/**
	 * @see ConnectionFactory#getConnection()
	 */
	public Connection getConnection() throws ResourceException {

		return (Connection) cm.allocateConnection(mcf, null);
	}

	/**
	 * @see ConnectionFactory#getConnection(ConnectionSpec)
	 */
	public Connection getConnection(ConnectionSpec connectionSpec) throws ResourceException {

		return getConnection();
	}

	/**
	 * @see ConnectionFactory#getRecordFactory()
	 */
	public RecordFactory getRecordFactory() throws ResourceException {

		return new HelloWorldRecordFactoryImpl();
	}

	/**
	 * @see ConnectionFactory#getMetaData()
	 */
	public ResourceAdapterMetaData getMetaData() throws ResourceException {

		return new HelloWorldResourceAdapterMetaDataImpl();
	}

	/**
	 * @see Referenceable#setReference(Reference)
	 */
	public void setReference(Reference reference) {

		this.reference = reference;
	}

	/**
	 * @see Referenceable#getReference()
	 */
	public Reference getReference() throws NamingException {

		return reference;
	}

}

