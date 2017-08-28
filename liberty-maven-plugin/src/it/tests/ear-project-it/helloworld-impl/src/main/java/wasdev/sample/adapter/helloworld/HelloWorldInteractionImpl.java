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
import javax.resource.cci.Interaction;
import javax.resource.cci.InteractionSpec;
import javax.resource.cci.Record;
import javax.resource.cci.ResourceWarning;

public class HelloWorldInteractionImpl implements Interaction {

	private static final String CLOSED_ERROR = "Connection closed";
	private static final String INVALID_FUNCTION_ERROR = "Invalid function";
	private static final String INVALID_INPUT_ERROR =
		"Invalid input record for function";
	private static final String INVALID_OUTPUT_ERROR =
		"Invalid output record for function";
	private static final String OUTPUT_RECORD_FIELD_01 = "Hello World!";
	private static final String EXECUTE_WITH_INPUT_RECORD_ONLY_NOT_SUPPORTED =
		"execute() with input record only not supported";

	private Connection connection;
	private boolean valid;

	/**
	 * Constructor for HelloWorldInteractionImpl
	 */
	public HelloWorldInteractionImpl(Connection connection) {

		super();
		this.connection = connection;
		valid = true;
	}

	/**
	 * @see Interaction#close()
	 */
	public void close() throws ResourceException {

		connection = null;
		valid = false;
	}

	/**
	 * @see Interaction#getConnection()
	 */
	public Connection getConnection() {

		return connection;
	}

	/**
	 * @see Interaction#execute(InteractionSpec, Record, Record)
	 */
	public boolean execute(InteractionSpec ispec, Record input, Record output)
		throws ResourceException {

		if (valid) {
			if (((HelloWorldInteractionSpecImpl) ispec)
				.getFunctionName()
				.equals(HelloWorldInteractionSpec.SAY_HELLO_FUNCTION)) {
				if (input.getRecordName().equals(HelloWorldIndexedRecord.INPUT)) {
					if (output.getRecordName().equals(HelloWorldIndexedRecord.OUTPUT)) {
						((HelloWorldIndexedRecord) output).clear();
						((HelloWorldIndexedRecord) output).add(OUTPUT_RECORD_FIELD_01);
					} else {
						throw new ResourceException(INVALID_OUTPUT_ERROR);
					}
				} else {
					throw new ResourceException(INVALID_INPUT_ERROR);
				}

			} else {
				throw new ResourceException(INVALID_FUNCTION_ERROR);
			}
		} else {
			throw new ResourceException(CLOSED_ERROR);
		}
		return true;
	}

	/**
	 * @see Interaction#execute(InteractionSpec, Record)
	 */
	public Record execute(InteractionSpec ispec, Record input)
		throws ResourceException {

		throw new NotSupportedException(EXECUTE_WITH_INPUT_RECORD_ONLY_NOT_SUPPORTED);
	}

	/**
	* @see Interaction#getWarnings()
	*/
	public ResourceWarning getWarnings() throws ResourceException {

		return null;
	}

	/**
	* @see Interaction#clearWarnings()
	*/
	public void clearWarnings() throws ResourceException {
	}

}

