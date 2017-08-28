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
import javax.resource.cci.IndexedRecord;
import javax.resource.cci.MappedRecord;
import javax.resource.cci.RecordFactory;

public class HelloWorldRecordFactoryImpl implements RecordFactory {

	private static final String MAPPED_RECORD_NOT_SUPPORTED_ERROR = "Mapped record not supported";
	private static final String INVALID_RECORD_NAME = "Invalid record name";

	/**
	 * Constructor for HelloWorldRecordFactoryImpl
	 */
	public HelloWorldRecordFactoryImpl() {
		
		super();
	}

	/**
	 * @see RecordFactory#createMappedRecord(String)
	 */
	public MappedRecord createMappedRecord(String recordName)
		throws ResourceException {

		throw new NotSupportedException(MAPPED_RECORD_NOT_SUPPORTED_ERROR);
	}

	/**
	 * @see RecordFactory#createIndexedRecord(String)
	 */
	public IndexedRecord createIndexedRecord(String recordName)
		throws ResourceException {

		HelloWorldIndexedRecordImpl record = null;

		if ((recordName.equals(HelloWorldIndexedRecordImpl.INPUT))
			|| (recordName.equals(HelloWorldIndexedRecordImpl.OUTPUT))) {
			record = new HelloWorldIndexedRecordImpl();
			record.setRecordName(recordName);
		}
		if (record == null) {
			throw new ResourceException(INVALID_RECORD_NAME);
		} else {
			return record;
		}
	}

}

