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

import javax.resource.cci.ResourceAdapterMetaData;

public class HelloWorldResourceAdapterMetaDataImpl
	implements ResourceAdapterMetaData {

	private static final String ADAPTER_VERSION = "1.0";
	private static final String ADAPTER_VENDOR_NAME = "Willy Farrell";
	private static final String ADAPTER_NAME = "Hello World Resource Adapter";
	private static final String ADAPTER_DESCRIPTION =
		"A simple sample resource adapter";
	private static final String SPEC_VERSION = "1.0";
	private static final String[] INTERACTION_SPECS_SUPPORTED =
		{ "wasdev.sample.adapter.helloworld.HelloWorldInteractionSpecImpl" };

	/**
	 * Constructor for HelloWorldResourceAdapterMetaDataImpl
	 */
	public HelloWorldResourceAdapterMetaDataImpl() {
		
		super();
	}

	/**
	 * @see ResourceAdapterMetaData#getAdapterVersion()
	 */
	public String getAdapterVersion() {

		return ADAPTER_VERSION;
	}

	/**
	 * @see ResourceAdapterMetaData#getAdapterVendorName()
	 */
	public String getAdapterVendorName() {

		return ADAPTER_VENDOR_NAME;
	}

	/**
	 * @see ResourceAdapterMetaData#getAdapterName()
	 */
	public String getAdapterName() {

		return ADAPTER_NAME;
	}

	/**
	 * @see ResourceAdapterMetaData#getAdapterShortDescription()
	 */
	public String getAdapterShortDescription() {

		return ADAPTER_DESCRIPTION;
	}

	/**
	 * @see ResourceAdapterMetaData#getSpecVersion()
	 */
	public String getSpecVersion() {

		return SPEC_VERSION;
	}

	/**
	 * @see ResourceAdapterMetaData#getInteractionSpecsSupported()
	 */
	public String[] getInteractionSpecsSupported() {
		
		return INTERACTION_SPECS_SUPPORTED;
	}

	/**
	 * @see ResourceAdapterMetaData#supportsExecuteWithInputAndOutputRecord()
	 */
	public boolean supportsExecuteWithInputAndOutputRecord() {
		
		return true;
	}

	/**
	 * @see ResourceAdapterMetaData#supportsExecuteWithInputRecordOnly()
	 */
	public boolean supportsExecuteWithInputRecordOnly() {
		
		return false;
	}

	/**
	 * @see ResourceAdapterMetaData#supportsLocalTransactionDemarcation()
	 */
	public boolean supportsLocalTransactionDemarcation() {

		return false;
	}

}

