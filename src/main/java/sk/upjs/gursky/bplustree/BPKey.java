/*
 * sk.upjs.gursky.bplustree.BPKey.java	ver 1.0, February 5th 2009
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
 * In {@link BPTree} entries are organized and sorted by key values first. Every key value must be able to serialize itself to
 * the {@link ByteBuffer}. 
 * 
 * Every key in the tree must have equal size, i.e. the method getSize() must return a constant equal value 
 * for all keys in a tree.
 * 
 * Two keys must be comparable to manage a sorted character of the B+tree structure.
 *
 * @author Peter Gursky
 * @version 1.0, February 5th 2009
 * @param <K> The implementing class itself
 */
public interface BPKey<K> extends Comparable<K>,Serializable
{
	/**
	 * Method is used to load information about instance variable(s) of the key from the current position of the {@link ByteBuffer}. 
	 * 
	 * @param bb buffer to read from
	 */
	public void           load(ByteBuffer bb);
	
	/**
	 * Method is used to save information about instance variable(s) of the key to the current position of the {@link ByteBuffer}. 

	 * @param bb buffer to write to
	 */
	public void           save(ByteBuffer bb);
	
	/**
	 * Method returns the maximal number of bytes needed to store a key value to a {@link ByteBuffer}.
	 * Method must return a constant (final static) value for all instances. For example, if the key is of type <code>int</code>, 
	 * this method should always return 4. If the size of the keys differs between instances, this value stands for
	 * the maximal size.
	 * 
	 * @return size needed to store this type of the key.	 
	 */
	public int	          getSize();
	
}
