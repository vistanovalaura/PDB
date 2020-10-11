/*
 * sk.upjs.gursky.bplustree.BPNode.java	ver 1.0, February 5th 2009
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
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Abstract class for both node types of the {@link BPTree}. This class is not public.  
 * 
 * @author Peter Gursky
 * @version 1.0, February 5th 2009
 * @see	    BPTree
 * @see	    BPInnerNode
 * @see	    BPLeafNode
 * @see	    BPKey
 * @see	    BPObject
 *
 * @param <K> Key class used in innner nodes 
 * @param <O> Entry class used in leaf nodes
 */
abstract class BPNode<K extends BPKey<K>, O extends BPObject<K, O>> implements Serializable
{
	private static final long serialVersionUID = -6641905492223425755L;
	/**
	 * number of entries in leaf nodes and keys in inner nodes, number of offsets is + 1
	 */
	int      			numberOfEntries;						
	/**
	 * place in file, where this node belongs
	 */
	long	 			offset;
	/**
	 * Tells if the node was changed since last read from disk.
	 * This is used in cache to find out if the node need to be rewritten 
	 */
	boolean  			isChanged;
	/**
	 * reference to the tree
	 */
	BPTree<K,O>  		tree;
	/**
	 * Returns the most left leaf in the B+tree
	 * 
	 * @return the most left leaf in the B+tree
	 */
	abstract BPLeafNode<K,O> findLeafLeft();
	/**
	 * Returns the most right leaf in the B+tree
	 * 
	 * @return the most right leaf in the B+tree
	 */
	abstract BPLeafNode<K,O> findLeafRight();
	/**
	 * Returns the leaf in the B+tree that contains given entry or 
	 * if there is no such entry it is the leaf, in which it should be according to inner nodes
	 *  
	 * @param entry Entry to find
	 * @return the leaf in which the entry is or should be
	 */
	abstract BPLeafNode<K,O> findLeft(O entry);
	/**
	 * Returns the most left leaf in the B+tree that contains entry with the given tree (it is not necessary that keys are unique) or 
	 * if there is no such entry it is the leaf, in which it should be according to inner nodes
	 *  
	 * @param key Key to find
	 * @return the most left leaf in which the entry with the given key is or should be
	 */
	abstract BPLeafNode<K,O> findLeafLeft(K key);
	/**
	 * Adds a new entry to the tree. If the entry in the tree already exists nothing changes.
	 *  
	 * @param entry Entry to add
	 * @return pair of key and offset after splitting the nodes.
	 */
	abstract KeyOffsetPair<K> add(O entry);
	/**
	 * Method use the iterator to create the subtree of the node.
	 * 
	 * @param iterator data source for the subtree
	 * @param size number of entries in the subtree to store
	 * @param height height of the node in the future B+tree
	 * @param maxSizes maximal number of entries in subtrees of different heights
	 * @param leftOffsetCorrection used to compute correct offsets between leaf nodes
	 * @param rightOffsetCorrection used to compute correct offsets between leaf nodes
	 * @return left key of the subtree
	 */
	abstract K batchUpdate(Iterator<O> iterator, int size, int height, ArrayList<Integer> maxSizes, int leftOffsetCorrection, int rightOffsetCorrection); // vrati svoj lavy kluc
	/**
	 * Removes entry from the subtree.
	 * 
	 * @param entry Entry to remove
	 * @param amIRoot true if the node is root
	 * @param myHeight height of the node
	 * @return true if the entry has been found and erased, false if there has been no such entry in a tree
	 */
	abstract boolean remove(O entry, boolean amIRoot, int myHeight);
	/**
	 * Stores the node in the {@link ByteBuffer}. Uses the <code>save</code> of the {@link BPKey}s or {@link BPObject}s.
	 * @param bb ByteBuffer to store to.
	 */
	abstract void    save(ByteBuffer bb);
	/**
	 * Adds the subtree dump to the String.  
	 * @param level string to add to
	 * @return result of the addition
	 */
	abstract String  toString(String level);
	/**
	 * Returns the height of the node.
	 * @return the height of the node
	 */
	abstract int	 getTreeHeight();
}
