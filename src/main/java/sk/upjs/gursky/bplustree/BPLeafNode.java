/*
 * sk.upjs.gursky.bplustree.BPLeafNode.java	ver 1.0, February 5th 2009
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
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Inner node class of the {@link BPTree}. This class is not public.  
 * 
 * @author Peter Gursky
 * @version 1.0, February 5th 2009
 * @see	    BPTree
 * @see	    BPInnerNode
 * @see	    BPKey
 * @see	    BPObject
 *
 * @param <K> Key class used in innner nodes 
 * @param <O> Entry class used in leaf nodes
 */
class BPLeafNode<K extends BPKey<K>, O extends BPObject<K, O>> extends BPNode<K,O> implements Serializable
{
	private static final long serialVersionUID = 1411879618254833463L;
	/**
	 * if the value is -1, the node doesn't exists
	 */
	long                    offsetLeftNode, offsetRightNode;						// offsets of neighbors (-1 means that there is no neighbor)
	O[]						entries;
	
	/**
	 * Creates new empty leaf node
	 */
	@SuppressWarnings("unchecked")
	BPLeafNode(BPTree<K,O> tree) {
		isChanged = true;
		this.tree = tree;
		offset = tree.getNewOffset();
		offsetLeftNode = offsetRightNode = -1;
		numberOfEntries = 0;
		entries = (O[]) Array.newInstance(tree.classO, tree.leafCapacity);
	}

	/**
	 * Creates a leaf node from {@link ByteBuffer} (i.e. from file)
	 */
	@SuppressWarnings("unchecked")
	BPLeafNode(long offset, ByteBuffer bb, BPTree<K,O> tree) {
		isChanged = false;
		this.tree = tree;
		this.offset = offset;
		offsetLeftNode = bb.getLong();
		offsetRightNode = bb.getLong();
		numberOfEntries = bb.getInt();
		entries = (O[]) Array.newInstance(tree.classO, tree.leafCapacity);
		for (int i = 0; i < numberOfEntries; i++) {
			try {
				entries[i] = tree.classO.newInstance();
			}
			catch (Exception e) {
				throw new Error("Class " + tree.classO + "should have empty constructor!");
			}
			entries[i].load(bb);
		}
	}
	
	KeyOffsetPair<K> add(O entry) {
		int pos = Arrays.binarySearch(entries, 0, numberOfEntries, entry);
		if (pos >= 0) return null; //there cannot be 2 equal entries
		isChanged = true;
		if (numberOfEntries == 0) {
			entries[numberOfEntries++] = entry;
			tree.putBPNode(this);
			return null;
		}
		int	i;
		pos = -1 - pos;
		if (numberOfEntries < tree.leafCapacity) {
			for (i = numberOfEntries; i > pos; i--) {
				entries[i] = entries[i - 1];
			}
			entries[pos] = entry;
			numberOfEntries++;
			tree.putBPNode(this);
			return null;
		}
		BPLeafNode<K,O> rightNode = new BPLeafNode<K,O>(tree);					              
		if (offsetRightNode >= 0) {															  
			BPLeafNode<K,O> rightNodeOld = (BPLeafNode<K,O>) tree.getBPNode(offsetRightNode); 
			rightNodeOld.offsetLeftNode = rightNode.offset;
			rightNodeOld.isChanged = true;	
		}
		K key = entries[numberOfEntries = tree.leafCapacity / 2].getKey();
		rightNode.offsetLeftNode = offset;
		rightNode.offsetRightNode = offsetRightNode;
		offsetRightNode = rightNode.offset;
		if (pos <= numberOfEntries) {
			rightNode.numberOfEntries = tree.leafCapacity - numberOfEntries;
			System.arraycopy(entries, numberOfEntries, rightNode.entries, 0, rightNode.numberOfEntries);
			for (i = numberOfEntries; i > pos; i--) {
				entries[i] = entries[i - 1];
			}
			entries[pos] = entry;
			numberOfEntries++;
		}
		else {
			rightNode.numberOfEntries = tree.leafCapacity - numberOfEntries + 1;
			System.arraycopy(entries, numberOfEntries, rightNode.entries, 0, pos - numberOfEntries);
			rightNode.entries[pos - numberOfEntries] = entry;
			System.arraycopy(entries, pos, rightNode.entries, pos - numberOfEntries + 1, tree.leafCapacity - pos);
		}
		
		tree.putBPNode(this);
		tree.putBPNode(rightNode);
		return new KeyOffsetPair<K>(key,offsetRightNode);
	}
	
	@Override
	K batchUpdate(Iterator<O> iterator, int size, int height, ArrayList<Integer> maxSizes, int leftOffsetCorrection, int rightOffsetCorrection) {
		isChanged = true;
		numberOfEntries = size;
		for (int i = 0; i < numberOfEntries; i++) {
			entries[i] = iterator.next();
		}
		offsetLeftNode = leftOffsetCorrection == -1 ? -1 : offset - leftOffsetCorrection * tree.nodeSize;
		offsetRightNode = rightOffsetCorrection == -1 ? -1 : offset + rightOffsetCorrection * tree.nodeSize;
		tree.putBPNode(this);
		return entries[0].getKey();
	}

	boolean remove(O entry, boolean amIRoot, int myHeight)
	{
		int pos = Arrays.binarySearch(entries, 0, numberOfEntries, entry);
		if (pos < 0) return false; // there is no such entry		
		numberOfEntries--;
		for (int i = pos; i < numberOfEntries; i++) {
			entries[i] = entries[i + 1];
		}
		isChanged=true;
		return true; // entry has been removed
	}
	
	void save(ByteBuffer bb) {
		bb.put((byte)2); //leaf node
		bb.putLong(offsetLeftNode);
		bb.putLong(offsetRightNode);
		bb.putInt(numberOfEntries);
		for (int i = 0; i < numberOfEntries; i++) {
			entries[i].save(bb);
		}
	}

	@Override
	BPLeafNode<K,O> findLeft(O entry) {
		return this;
	}
	
	@Override
	BPLeafNode<K,O> findLeafLeft(K key) {
		return this;
	}
	
	@Override
	BPLeafNode<K,O> findLeafLeft() {
		return this;
	}
	
	@Override
	BPLeafNode<K,O> findLeafRight() {
		return this;
	}
	
	/**
	 * If we have key and want it find in the array of entries.
	 * @param key Key of the entry to find
	 * @return position of the entry with the given tree if it is contained in the array of entries
     *	       otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
     *	       <i>insertion point</i> is defined as the point at which the
     *	       key would be inserted into the array: the index of the first
     *	       entry in the range greater than the key,
     *	       or <tt>numberOfEntries</tt> if all
     *	       entries in the range are less than the specified key.  Note
     *	       that this guarantees that the return value will be &gt;= 0 if
     *	       and only if the entry with the given key is found.
	 */
	int binarySearch(K key) {
		int low = 0;
		int high = numberOfEntries -1;
		int mid, cmp;
		
		while (low <= high) {
			mid = (low + high) >> 1;
			O midVal = entries[mid];
			cmp = midVal.getKey().compareTo(key);
			if (cmp < 0)
				low = mid + 1;
			else if (cmp > 0)
				high = mid - 1;
			else
				return mid; // key found
		}
		return -(low + 1); // key not found.
	}
	
	/**
	 * Uses the <code>binarySearch</code> to find one position of the entry with the given key. 
	 * If the position is &lt; 0, method returns the same as <code>binarySearch</code>.
	 * Otherwise it returns the position of the most left entry with the given key.  
	 *  
	 * @param key Key of the entry to find
	 * @return The position of the most left entry with the given key if it is contained in the array of entries. 
     *	       Otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
     *	       <i>insertion point</i> is defined as the point at which the
     *	       key would be inserted into the array: the index of the first
     *	       entry in the range greater than the key,
     *	       or <tt>numberOfEntries</tt> if all
     *	       entries in the range are less than the specified key.  Note
     *	       that this guarantees that the return value will be &gt;= 0 if
     *	       and only if the entry with the given key is found.
	 */
	public int getLeftObjectPosition(K key) {
		int	position = binarySearch(key);
		if (position<0) return position;
		while (position>0 && entries[position-1].getKey().equals(entries[position].getKey())) position--;
		return position;
	}

	/**
	 * Returns leaf node on the left if exist.
	 * @return left leaf node if exists, otherwise returns <code>null</code>.
	 */
	public BPLeafNode<K,O> getRightNode() {	
		return offsetRightNode < 0 ? null : (BPLeafNode<K,O>) tree.getBPNode(offsetRightNode);
	}
	
	/**
	 * Returns leaf node on the right if exist.
	 * @return right leaf node if exists, otherwise returns <code>null</code>.
	 */
	public BPLeafNode<K,O> getLeftNode() {
		return offsetLeftNode < 0 ? null : (BPLeafNode<K,O>) tree.getBPNode(offsetLeftNode);
	}
	
	@Override
	String toString(String level) {
		String output = "" + level;
		output += getClass().getCanonicalName() + "\r\n";
		for (int i = 0; i < numberOfEntries; i++) {
			output += level + "\t" + entries[i].toString() + "\r\n";
		}
		return output;
	}

	@Override
	int getTreeHeight() {
		return 0;
	}
}
