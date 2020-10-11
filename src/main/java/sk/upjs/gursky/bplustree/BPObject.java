/*
 * sk.upjs.gursky.bplustree.BPObject.java	ver 1.0, February 5th 2009
 *
 *	   Copyright 2009 Peter Gursky. All rights reserved.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *     
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *     
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package sk.upjs.gursky.bplustree;

import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * In {@link BPTree} entries are organized and sorted by key values in inner nodes and by BPObjects in leafs. 
 * 
 * Key values are parts of BPObjects. 
 * 
 * Two BPObjects MUST be comparable first by key value and than by other values in BPObjects.
 * 
 * Every BPObject must be able to serialize itself to the {@link ByteBuffer}. 
 * 
 * Every BPObject in the tree must have equal maximal size, i.e. the method getSize() must return a constant equal value 
 * for BPObjects in a tree.
 * 
 * @author Peter Gursky
 * @version 1.0, February 5th 2009
 * @param <K> The class of the BPKey implementation
 * @param <O> The class of the BPObject implementation
 */
public interface BPObject<K,O> extends Comparable<O>, Serializable
{
	/**
	 * Method is used to load information about instance variable(s) of the BPObject from the current position of the {@link ByteBuffer}. 
	 * 
	 * @param bb buffer to read from
	 */
	public void     load(ByteBuffer bb);
	
	/**
	 * Method is used to save information about instance variable(s) of the key to the current position of the {@link ByteBuffer}. 
	 * 
	 * @param bb buffer to write to
	 */
	public void     save(ByteBuffer bb);

	/**
	 * Method returns the maximal number of bytes needed to store a BPObject to a {@link ByteBuffer}.
	 * Method must return a constant (final static) value for all instances. For example, if the key is of type <code>int</code>, 
	 * and the other value in BPObject is <code>double</code> this method should always return 12 (4+8). 
	 * If the size of the BPObjects differs between instances, this value stands for the maximal size.
	 * 
	 * @return size needed to store this type of BPObject.	 
	 */
	public int	    getSize();
	
	/**
	 * Returns key of the given entry
	 * @return key of the given entry
	 */
	public K        getKey();
}
