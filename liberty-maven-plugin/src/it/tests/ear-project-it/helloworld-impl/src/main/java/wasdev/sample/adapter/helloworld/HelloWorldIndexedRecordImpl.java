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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;


public class HelloWorldIndexedRecordImpl implements HelloWorldIndexedRecord {

    private static final long serialVersionUID = -2866469109320308474L;
    private ArrayList list = new ArrayList();
	private String name;
	private String description;
	
	/**
	 * Constructor for HelloWorldIndexedRecordImpl
	 */
	public HelloWorldIndexedRecordImpl() {
		
		super();
	}

	/**
	 * @see Record#getRecordName()
	 */
	public String getRecordName() {
		
		return name;
	}

	/**
	 * @see Record#setRecordName(String)
	 */
	public void setRecordName(String name) {
		
		this.name = name;
	}

	/**
	 * @see Record#setRecordShortDescription(String)
	 */
	public void setRecordShortDescription(String description) {
		
		this.description = description;
	}

	/**
	 * @see Record#getRecordShortDescription()
	 */
	public String getRecordShortDescription() {
		
		return description;
	}

	/**
	 * @see List#size()
	 */
	public int size() {
		
		return list.size();
	}

	/**
	 * @see List#isEmpty()
	 */
	public boolean isEmpty() {
		
		return list.isEmpty();
	}

	/**
	 * @see List#contains(Object)
	 */
	public boolean contains(Object o) {
		
		return list.contains(o);
	}

	/**
	 * @see List#iterator()
	 */
	public Iterator iterator() {
		
		return list.iterator();
	}

	/**
	 * @see List#toArray()
	 */
	public Object[] toArray() {
		
		return list.toArray();
	}

	/**
	 * @see List#toArray(Object[])
	 */
	public Object[] toArray(Object[] a) {
		
		return list.toArray(a);
	}

	/**
	 * @see List#add(Object)
	 */
	public boolean add(Object o) {
		
		return list.add(o);
	}

	/**
	 * @see List#remove(Object)
	 */
	public boolean remove(Object o) {
		
		return list.remove(o);
	}

	/**
	 * @see List#containsAll(Collection)
	 */
	public boolean containsAll(Collection c) {
		
		return list.containsAll(c);
	}

	/**
	 * @see List#addAll(Collection)
	 */
	public boolean addAll(Collection c) {
		
		return list.addAll(c);
	}

	/**
	 * @see List#addAll(int, Collection)
	 */
	public boolean addAll(int index, Collection c) {
		
		return list.addAll(index, c);
	}

	/**
	 * @see List#removeAll(Collection)
	 */
	public boolean removeAll(Collection c) {
		
		return list.removeAll(c);
	}

	/**
	 * @see List#retainAll(Collection)
	 */
	public boolean retainAll(Collection c) {
		
		return list.retainAll(c);
	}

	/**
	 * @see List#clear()
	 */
	public void clear() {
		
		list.clear();
	}

	/**
	 * @see List#get(int)
	 */
	public Object get(int index) {
		
		return list.get(index);
	}

	/**
	 * @see List#set(int, Object)
	 */
	public Object set(int index, Object o) {
		
		return list.set(index, o);
	}

	/**
	 * @see List#add(int, Object)
	 */
	public void add(int index, Object o) {
		
		list.add(index, o);
	}

	/**
	 * @see List#remove(int)
	 */
	public Object remove(int index) {
		
		return list.remove(index);
	}

	/**
	 * @see List#indexOf(Object)
	 */
	public int indexOf(Object o) {
		
		return list.indexOf(o);
	}

	/**
	 * @see List#lastIndexOf(Object)
	 */
	public int lastIndexOf(Object o) {
		
		return list.lastIndexOf(o);
	}

	/**
	 * @see List#listIterator()
	 */
	public ListIterator listIterator() {
		
		return list.listIterator();
	}

	/**
	 * @see List#listIterator(int)
	 */
	public ListIterator listIterator(int index) {
		
		return list.listIterator(index);
	}

	/**
	 * @see List#subList(int, int)
	 */
	public List subList(int fromIndex, int toIndex) {
		
		return list.subList(fromIndex, toIndex);
	}
	
	/**
	 * @see Record#clone()
	 */
	public Object clone() throws CloneNotSupportedException{
		
		throw new CloneNotSupportedException();
	}

}

