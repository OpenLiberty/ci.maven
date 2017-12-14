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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class HelloWorldInteractionSpecImpl implements HelloWorldInteractionSpec {
	
    private static final long serialVersionUID = -8074485262927952111L;
    private String functionName;
	protected transient PropertyChangeSupport propertyChange;

	/**
	 * Constructor for HelloWorldInteractionSpecImpl
	 */
	public HelloWorldInteractionSpecImpl() {

		super();
	}

	/**
	 * Gets the functionName
	 * @return Returns a String
	 * @see HelloWorldInteractionSpec#getFunctionName()
	 */
	public String getFunctionName() {

		return functionName;
	}

	/**
	 * Sets the functionName
	 * @param functionName The functionName to set
	 * @see HelloWorldInteractionSpec#setFunctionName(String)
	 */
	public void setFunctionName(String functionName) {

		String oldFunctionName = functionName;
		this.functionName = functionName;
		firePropertyChange("FunctionName", oldFunctionName, functionName);
	}

	/**
	 * The addPropertyChangeListener method was generated to support the propertyChange field.
	 */
	public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {

		getPropertyChange().addPropertyChangeListener(listener);
	}

	/**
	 * The addPropertyChangeListener method was generated to support the propertyChange field.
	 */
	public synchronized void addPropertyChangeListener(
		String propertyName,
		PropertyChangeListener listener) {
			
		getPropertyChange().addPropertyChangeListener(propertyName, listener);
	}

	/**
	 * The firePropertyChange method was generated to support the propertyChange field.
	 */
	public void firePropertyChange(PropertyChangeEvent evt) {

		getPropertyChange().firePropertyChange(evt);
	}

	/**
	 * The firePropertyChange method was generated to support the propertyChange field.
	 */
	public void firePropertyChange(
		String propertyName,
		int oldValue,
		int newValue) {

		getPropertyChange().firePropertyChange(propertyName, oldValue, newValue);
	}

	/**
	 * The firePropertyChange method was generated to support the propertyChange field.
	 */
	public void firePropertyChange(
		String propertyName,
		Object oldValue,
		Object newValue) {

		getPropertyChange().firePropertyChange(propertyName, oldValue, newValue);
	}

	/**
	 * The firePropertyChange method was generated to support the propertyChange field.
	 */
	public void firePropertyChange(
		String propertyName,
		boolean oldValue,
		boolean newValue) {

		getPropertyChange().firePropertyChange(propertyName, oldValue, newValue);
	}

	/**
	 * Accessor for the propertyChange field.
	 */
	protected PropertyChangeSupport getPropertyChange() {

		if (propertyChange == null) {
			propertyChange = new PropertyChangeSupport(this);
		}
		return propertyChange;
	}

	/**
	 * The hasListeners method was generated to support the propertyChange field.
	 */
	public synchronized boolean hasListeners(String propertyName) {

		return getPropertyChange().hasListeners(propertyName);
	}

	/**
	 * The removePropertyChangeListener method was generated to support the propertyChange field.
	 */
	public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {

		getPropertyChange().removePropertyChangeListener(listener);
	}

	/**
	 * The removePropertyChangeListener method was generated to support the propertyChange field.
	 */
	public synchronized void removePropertyChangeListener(
		String propertyName,
		PropertyChangeListener listener) {

		getPropertyChange().removePropertyChangeListener(propertyName, listener);
	}
}

