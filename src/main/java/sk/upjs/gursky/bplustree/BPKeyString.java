/*
 * sk.upjs.gursky.bplustree.BPKeyString.java	ver 1.0, February 5th 2009
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
 * This is an abstract class for String keys in general. However the BPTree cannot manage strings with unbounded size,
 * you need to add methods <code>getSize()</code> and <code>getMaxLength()</code> to the extending class to create
 * so called varchar type known from databases. 
 * 
 * @author Peter Gursky
 * @version 1.0, February 5th 2009
 * @see	    BPKey
 */
public abstract class BPKeyString implements BPKey<BPKeyString>, Serializable
{

	private static final long serialVersionUID = 574101229547812145L;
	String key;

	public BPKeyString() {}

	public BPKeyString(String key) {
		this.key = key;
	}

	protected abstract int getMaxLength();
	
	public void load(ByteBuffer bb)	{
		int size = bb.getInt();
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<size; i++) {
			sb.append(bb.getChar());
		}
		key = sb.toString();
	}
	
	public void save(ByteBuffer bb)	{
		int size = key.length();
		if (size > getMaxLength()) 
			throw new RuntimeException("Key " + key + " is longer than "+getMaxLength()+".");
		bb.putInt(size);
		for (int i=0;i<size;i++) {
			bb.putChar(key.charAt(i));
		}
	}
	
	@Override
	public String toString() {
		return key;
	}
	
	/**
	 * Compares this object with the specified object for order. 
	 * Returns a negative integer, zero, or a positive integer as this object is less than, 
	 * equal to, or greater than the specified object.
	 * 
	 * @param o The object to be compared. 
	 * @return A negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object. 
	 */
	public int compareTo(BPKeyString o) {
		return key.compareTo(o.key);
	}

	@Override
	public boolean equals(Object obj) {
		if (! (obj instanceof BPKeyString)) return false;
		return key.equals(((BPKeyString)obj).key);
	}

}
