/*
 * sk.upjs.gursky.bplustree.BPInnerNode.java ver 1.0, February 5th 2009
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
 * @see	    BPLeafNode
 * @see	    BPObject
 * @see	    BPKey
 *
 * @param <K> Key class used in inner nodes 
 * @param <O> Entry class used in leaf nodes
 */
class BPInnerNode<K extends BPKey<K>, O extends BPObject<K, O>> extends BPNode<K,O> implements Serializable
{

	private static final long serialVersionUID = 3684595713571630960L;
	BPInnerNode<K,O>  superNode;
	long[]            offsets;
	K[]				  entries;

	/**
	 * Creates new empty inner node
	 */
	@SuppressWarnings("unchecked")
	BPInnerNode(BPTree<K,O> tree)		
	{
		isChanged = true;
		this.tree = tree;
		offset = tree.getNewOffset();
		numberOfEntries = 0;
		entries = (K[]) Array.newInstance(tree.classK, tree.internalNodeCapacity);
		offsets = new long[tree.internalNodeCapacity + 1];
	}

	/**
	 * Creates an inner node from {@link ByteBuffer} (i.e. from file)
	 */
	@SuppressWarnings("unchecked")
	BPInnerNode(long offset, ByteBuffer bb, BPTree<K,O> tree)
	{
		isChanged = false;
		this.tree = tree;
		this.offset = offset;
		numberOfEntries = bb.getInt();
		entries = (K[]) Array.newInstance(tree.classK, tree.internalNodeCapacity);
		for (int i = 0; i < numberOfEntries; i++) {
			try {
				entries[i] = tree.classK.newInstance();
			}
			catch (Exception e) {
				throw new Error("Class " + tree.classK + "should have empty constructor!");
			}
			entries[i].load(bb);
		}
		offsets = new long[tree.internalNodeCapacity + 1];
		for (int i = 0; i <= numberOfEntries; i++) {
			offsets[i]= bb.getLong();
		}
	}
	
	/**
	 * Finds the offset of the most left child node in which the entry with the given key should be. 
	 *   
	 * @param key searched key
	 * @return the offset most left child node in which the entry with the given key should be.
	 */
	private long getChildOffset(K key) {
		int pos = Arrays.binarySearch(entries, 0, numberOfEntries, key);
		if (pos < 0) 
			pos = -1 - pos;
		else {
			while ((pos > 0)&&(key.compareTo(entries[pos - 1]) == 0)) 
				pos--;
			pos++; // because the object with given key is included in the right child subtree 
		}
		return offsets[pos];
	}

	/**
	 * Finds the offset of the child node in which the given entry should be. 
	 *   
	 * @param entry searched entry
	 * @return the offset of the child node in which the given entry should be.
	 */
	private long getChildOffset(O entry) {
		int pos = Arrays.binarySearch(entries, 0, numberOfEntries, entry.getKey());
		if (pos < 0) 
			pos = -1 - pos;
		else {
			pos++; // because the object with given key is included in the right child subtree 
		}
		return offsets[pos];
	}
	
	@Override
	BPLeafNode<K,O> findLeft(O entry) {
		BPNode<K,O> child = tree.getBPNode(getChildOffset(entry));
		return child.findLeft(entry);
	}

	@Override
	BPLeafNode<K,O> findLeafLeft(K key) {
		BPNode<K,O> child = tree.getBPNode(getChildOffset(key));
		return child.findLeafLeft(key);
	}

	@Override
	BPLeafNode<K,O> findLeafLeft() {
		BPNode<K,O> child = tree.getBPNode(offsets[0]);
		return child.findLeafLeft();
	}

	@Override
	BPLeafNode<K,O> findLeafRight()	{
		BPNode<K,O> child = tree.getBPNode(offsets[numberOfEntries]);
		return child.findLeafRight();
	}

	@Override
	KeyOffsetPair<K> add(O entry) {
		long childOffset = getChildOffset(entry);
		KeyOffsetPair<K> pair = tree.getBPNode(childOffset).add(entry);
		return pair==null ? null : addFromChild(childOffset, pair.key, pair.offset);
	}
	
	/**
	 * Adds new entry to the node after its child was split to two nodes. 
	 * 
	 * @param leftOffset offset of new left child node
	 * @param key delimiter value between the children nodes
	 * @param rightOffset offset of new right child node
	 * @return pair of key and offset after splitting the nodes.
	 */
	KeyOffsetPair<K> addFromChild(long leftOffset, K key, long rightOffset)	{
		isChanged = true;
		if (numberOfEntries == 0) {
			entries[0] = key;
			offsets[0] = leftOffset;
			offsets[++numberOfEntries] = rightOffset;
			tree.putBPNode(this);
			return null;
		}
		int i;
		if (numberOfEntries < tree.internalNodeCapacity) {
			i = numberOfEntries++;
			while (offsets[i] != leftOffset) {
				offsets[i + 1] = offsets[i];
				entries[i] = entries[i - 1];
				i--;
			}
			offsets[i + 1] = rightOffset;
			entries[i] = key;
			tree.putBPNode(this);
			return null;
		}
		K  keyUp;														// key for parent node
		BPInnerNode<K,O> rightNode = new BPInnerNode<K,O>(tree);
		i = tree.internalNodeCapacity;
		numberOfEntries = tree.internalNodeCapacity / 2;
		rightNode.numberOfEntries = tree.internalNodeCapacity - numberOfEntries;
		while ((offsets[i] != leftOffset)&&(i >= numberOfEntries)) {
			rightNode.offsets[i - numberOfEntries] = offsets[i];
			i--;
		}
		if (i < numberOfEntries) {
			System.arraycopy(entries, numberOfEntries, rightNode.entries, 0, rightNode.numberOfEntries);
			keyUp = entries[i];
			while (offsets[i] != leftOffset) {
				offsets[i + 1] = offsets[i];
				entries[i] = entries[i - 1];
				i--;
			}
			offsets[i + 1] = rightOffset;
			entries[i] = key;
		}
		else {
			System.arraycopy(entries, i, rightNode.entries, i - numberOfEntries, tree.internalNodeCapacity - i);
			if (i == numberOfEntries) {
				rightNode.offsets[0] = rightOffset;
				keyUp = key;
			}
			else {
				rightNode.offsets[i - numberOfEntries] = rightOffset;
				rightNode.entries[i - numberOfEntries - 1] = key;
				System.arraycopy(offsets, numberOfEntries + 1, rightNode.offsets, 0, i - numberOfEntries);
				System.arraycopy(entries, numberOfEntries + 1, rightNode.entries, 0, i - numberOfEntries - 1);
				keyUp = entries[numberOfEntries];
			}
		}
		tree.putBPNode(this);
		tree.putBPNode(rightNode);
		return new KeyOffsetPair<K>(keyUp,rightNode.offset);
	}
	
	@Override
	K batchUpdate(Iterator<O> iterator, int size, int height, ArrayList<Integer> maxSizes, int leftOffsetCorrection, int rightOffsetCorrection) {
		isChanged = true;
		int childHeight = height - 1;
		int leftC, rightC;
		K leftKey = null;
		BPNode<K,O> child;
		do {
			child = childHeight == 0 ? new BPLeafNode<K, O>(tree) : new BPInnerNode<K, O>(tree);
			leftC = numberOfEntries == 0 ? leftOffsetCorrection : height;
			rightC = size - maxSizes.get(childHeight) <= 0 ? rightOffsetCorrection : height;
			K key = child.batchUpdate(iterator, Math.min(size,maxSizes.get(childHeight)), childHeight, maxSizes, leftC, rightC);
			if (leftKey == null) {
				leftKey = key;
				offsets[0] = child.offset;
			}
			else {
				entries[numberOfEntries++] = key;
				offsets[numberOfEntries] = child.offset;
			}
			size -= maxSizes.get(childHeight); 
		} while (size > 0);
		tree.putBPNode(this);
		return leftKey;
	}

	boolean remove(O entry, boolean amIRoot, int myHeight)
	{
		int pos = Arrays.binarySearch(entries, 0, numberOfEntries, entry.getKey());
		if (pos < 0) 
			pos = -1 - pos;
		else {
			while ((pos > 0)&&(entry.getKey().compareTo(entries[pos - 1]) == 0)) 
				pos--;
			pos++; // because the object with given key is included in the right tree
		}
		long childOffset = offsets[pos];

		boolean result;
		if (myHeight == 1) {															//child is leaf
			BPLeafNode<K,O> child = (BPLeafNode<K,O>) tree.getBPNode(childOffset);
			result = child.remove(entry, false, 0);
			if (! result) return false;
			if (child.numberOfEntries >= tree.minLeafQuantity) {
				tree.putBPNode(child);
				return result;
			}
			isChanged = true;
			BPLeafNode<K,O> leftChild = null;
			BPLeafNode<K,O> rightChild = null;
			if (pos>0) {
				leftChild = (BPLeafNode<K,O>) tree.getBPNode(offsets[pos-1]);
				if (leftChild.numberOfEntries>tree.minLeafQuantity) {				// taking entry from left child
					for (int i = child.numberOfEntries; i > 0; i--) {
						child.entries[i] = child.entries[i-1];
					}
					child.numberOfEntries++;
					child.entries[0] = leftChild.entries[--leftChild.numberOfEntries];
					entries[pos-1] = child.entries[0].getKey();
					tree.putBPNode(child);
					leftChild.isChanged = true;
					tree.putBPNode(leftChild);
					return result;
				}
			}
			if (pos < numberOfEntries) {
				rightChild = (BPLeafNode<K,O>) tree.getBPNode(offsets[pos+1]);
				if (rightChild.numberOfEntries>tree.minLeafQuantity) {				// taking entry from right child
					child.entries[child.numberOfEntries++] = rightChild.entries[0];
					for (int i = 1; i < rightChild.numberOfEntries; i++) {
						rightChild.entries[i-1] = rightChild.entries[i];
					}
					rightChild.numberOfEntries--;
					entries[pos] = rightChild.entries[0].getKey();
					tree.putBPNode(child);
					rightChild.isChanged = true;
					tree.putBPNode(rightChild);
					return result;
				}
			}
			if (pos > 0) {														// integration with left child
				for (int i = 0; i < child.numberOfEntries; i++) {
					leftChild.entries[leftChild.numberOfEntries] = child.entries[i];
					leftChild.numberOfEntries++;
				}
				leftChild.offsetRightNode = child.offsetRightNode;
				if (child.offsetRightNode >= 0) {
					rightChild = (BPLeafNode<K,O>) tree.getBPNode(child.offsetRightNode);
					rightChild.offsetLeftNode = leftChild.offset;
					rightChild.isChanged = true;
					tree.putBPNode(rightChild);
				}
				tree.addNewFreeOffset(child.offset);
				leftChild.isChanged = true;
				tree.putBPNode(leftChild);
				// removing offset at position pos and entry at position pos-1
				for (int i = pos; i < numberOfEntries; i++) {
					offsets[i] = offsets[i+1];
					entries[i-1] = entries[i];
				}
				numberOfEntries--;
				return result;
			}
			// integration with right child (child is at position 0)
			for (int i = 0; i < rightChild.numberOfEntries; i++) {
				child.entries[child.numberOfEntries] = rightChild.entries[i];
				child.numberOfEntries++;
			}
			rightChild.entries = child.entries;
			rightChild.numberOfEntries = child.numberOfEntries;
			rightChild.offsetLeftNode = child.offsetLeftNode;
			if (child.offsetLeftNode >= 0) {
				leftChild = (BPLeafNode<K,O>) tree.getBPNode(child.offsetLeftNode);
				leftChild.offsetRightNode = rightChild.offset;
				leftChild.isChanged = true;
				tree.putBPNode(leftChild);
			}
			tree.addNewFreeOffset(child.offset);
			// removing offset at position 0 and entry at position 0
			for (int i = 1; i < numberOfEntries; i++) {
				offsets[i-1] = offsets[i];
				entries[i-1] = entries[i];
			}
			offsets[numberOfEntries-1] = offsets[numberOfEntries];
			numberOfEntries--;
			isChanged = true;
			rightChild.isChanged = true;
			if (numberOfEntries == 0 && amIRoot) {
				tree.setNewRootAfterRemoving(rightChild);
				tree.addNewFreeOffset(offset);
				return result;
			}
			else {
				tree.putBPNode(rightChild);
				return result;
			}
		}
		else {						 													// child is inner node
			BPInnerNode<K,O> child = (BPInnerNode<K,O>) tree.getBPNode(childOffset);
			result = child.remove(entry, false, myHeight - 1);
			if (! result) return false;
			if (child.numberOfEntries >= tree.minInternalNodeQuantity) {
				tree.putBPNode(child);
				return result;
			}
			isChanged = true;
			BPInnerNode<K,O> leftChild = null;
			BPInnerNode<K,O> rightChild = null;
			if (pos>0) {
				leftChild = (BPInnerNode<K,O>) tree.getBPNode(offsets[pos-1]);
				if (leftChild.numberOfEntries>tree.minInternalNodeQuantity) {				// taking entry from left child
					child.offsets[child.numberOfEntries+1] = child.offsets[child.numberOfEntries];
					for (int i = child.numberOfEntries; i > 0; i--) {
						child.entries[i] = child.entries[i-1];
						child.offsets[i] = child.offsets[i-1];
					}
					child.numberOfEntries++;
					child.entries[0] = entries[pos-1];
					child.offsets[0] = leftChild.offsets[leftChild.numberOfEntries];
					entries[pos-1] = leftChild.entries[--leftChild.numberOfEntries];
					tree.putBPNode(child);
					leftChild.isChanged = true;
					tree.putBPNode(leftChild);
					return result;
				}
			}
			if (pos < numberOfEntries) {
				rightChild = (BPInnerNode<K,O>) tree.getBPNode(offsets[pos+1]);
				if (rightChild.numberOfEntries>tree.minInternalNodeQuantity) {				// taking entry from right child
					child.entries[child.numberOfEntries++] = entries[pos];
					child.offsets[child.numberOfEntries] = rightChild.offsets[0];
					entries[pos] = rightChild.entries[0];
					for (int i = 1; i < rightChild.numberOfEntries; i++) {
						rightChild.entries[i-1] = rightChild.entries[i];
						rightChild.offsets[i-1] = rightChild.offsets[i];
					}
					rightChild.offsets[rightChild.numberOfEntries-1] = rightChild.offsets[rightChild.numberOfEntries]; 
					rightChild.numberOfEntries--;
					tree.putBPNode(child);
					rightChild.isChanged = true;
					tree.putBPNode(rightChild);
					return result;
				}
			}
			if (pos > 0) {														// integration with left child
				leftChild.entries[leftChild.numberOfEntries] = entries[pos-1];
				leftChild.numberOfEntries++;
				for (int i = 0; i < child.numberOfEntries; i++) {
					leftChild.entries[leftChild.numberOfEntries] = child.entries[i];
					leftChild.offsets[leftChild.numberOfEntries] = child.offsets[i];
					leftChild.numberOfEntries++;
				}
				leftChild.offsets[leftChild.numberOfEntries] = child.offsets[child.numberOfEntries];
				tree.addNewFreeOffset(child.offset);
				leftChild.isChanged = true;
				tree.putBPNode(leftChild);
				// removing offset at position pos and entry at position pos-1
				for (int i = pos; i < numberOfEntries; i++) {
					offsets[i] = offsets[i+1];
					entries[i-1] = entries[i];
				}
				numberOfEntries--;
				return result;
			}
			// integration with right child (child is at position 0)
			child.entries[child.numberOfEntries] = entries[pos];
			child.numberOfEntries++;
			for (int i = 0; i < rightChild.numberOfEntries; i++) {
				child.entries[child.numberOfEntries] = rightChild.entries[i];
				child.offsets[child.numberOfEntries] = rightChild.offsets[i];
				child.numberOfEntries++;
			}
			child.offsets[child.numberOfEntries] = rightChild.offsets[rightChild.numberOfEntries];
			rightChild.entries = child.entries;
			rightChild.offsets = child.offsets;
			rightChild.numberOfEntries = child.numberOfEntries;
			tree.addNewFreeOffset(child.offset);
			// removing offset at position 0 and entry at position 0
			for (int i = 1; i < numberOfEntries; i++) {
				offsets[i-1] = offsets[i];
				entries[i-1] = entries[i];
			}
			offsets[numberOfEntries-1] = offsets[numberOfEntries];
			numberOfEntries--;
			isChanged = true;
			rightChild.isChanged = true;
			if (numberOfEntries == 0 && amIRoot) {
				tree.setNewRootAfterRemoving(rightChild);
				tree.addNewFreeOffset(offset);
			}
			else
				tree.putBPNode(rightChild);
			return result;
		}
	}

	@Override
	void save(ByteBuffer bb)
	{
		bb.put((byte)1); //inner node
		bb.putInt(numberOfEntries);
		for (int i = 0; i < numberOfEntries; i++) {
			entries[i].save(bb);
		}
		for (int i = 0; i <= numberOfEntries; i++) {
			bb.putLong(offsets[i]);
		}
	}


	@Override
	String toString(String level) {
		String output = "" + level;
		output += getClass().getCanonicalName() + "\r\n";
		for (int i = 0; i <= numberOfEntries; i++) {
			if (i>0) output += entries[i-1] + " ";
			output += tree.getBPNode(offsets[i]).toString(level + "\t");
		}
		return output;
	}

	
	@Override
	int getTreeHeight() {
		return tree.getBPNode(offsets[0]).getTreeHeight()+1;
	}
}
