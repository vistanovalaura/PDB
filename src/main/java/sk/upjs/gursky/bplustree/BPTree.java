/*
 * sk.upjs.gursky.bplustree.BPTree.java	ver 1.0, February 5th 2009
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

/**
 * The main class of the B+tree index structure. BPTree can be used similarly to
 * memory structure {@link TreeMap} to store pairs of keys and entries on disk.
 * The difference is that all keys and entries must have their maximal byte size
 * to be stored in. For example it can store pairs of integer (4 bytes max) and
 * string as varchar(20) i.e. with maximal length of 20 characters i.e. 40 bytes
 * for characters and 4 bytes for length.
 * 
 * Therefore both keys and entries must implement interfaces {@link BPKey} and
 * {@link BPObject} respectively. These interfaces extends the values with their
 * maximal size and ability to store themselves to a {@link ByteBuffer}.
 * 
 * The typical use of the BPTree:
 * 
 * <pre>
 * BPTree<BPKeyInt, BPObjectIntDouble> tree = new BPTree<BPKeyInt, BPObjectIntDouble>(BPObjectIntDouble.class,
 * 		new File("/var/tmp/indexBP.idx"));
 * tree.setNodeSize(8192); // default is 4096
 * tree.setCacheCapacity(100); // default is 10
 * tree.openNewFile();
 * for (int i = 0; i < values.size(); i++) {
 * 	tree.add(new BPObjectIntDouble((int) (Math.random() * 1000000000), Math.random()));
 * }
 * for (BPObjectIntDouble entry : tree) {
 * 	System.out.println(entry);
 * }
 * tree.close();
 * </pre>
 * 
 * Tree also supports the batch update of the sorted entries. This is much
 * faster than the previous example and moreover the tree is lower and almost
 * all leafs are full (i.e. faster when querying).
 * 
 * This class in not synchronized.
 * 
 * BPTree has no support for concurrent manipulation except concurrent reading.
 * Therefore it has no support for transactions. On the other way it is faster
 * than transaction variant.
 * 
 * @author Peter Gursky
 * @version 1.0, February 5th 2009
 * @see BPKey
 * @see BPObject
 * @see TreeMap
 *
 * @param <K> Key class used in innner nodes
 * @param <O> Entry class used in leaf nodes
 */
public class BPTree<K extends BPKey<K>, O extends BPObject<K, O>> implements Serializable, Iterable<O> {
	private static final long serialVersionUID = 3547521107441747404L;

	int nodeSize; // number of bytes for one node
	int leafCapacity; // maximal number of entries in leaf, must be at least 2, is computed from
						// nodeSize and objectSize
	int minLeafQuantity; // minimal number of entries in leaf, upper integer half of the leafCapacity
	int internalNodeCapacity; // maximal number of entries in inner node, must be at least 2, is computed from
								// nodeSize and keySize
	int minInternalNodeQuantity;// minimal number of entries in inner node, lower integer half of the
								// internalNodeCapacity
	Class<O> classO;
	Class<K> classK;
	private int treeHeight;
	private File indexFile;
	private FileChannel channel;
	private RandomAccessFile raf;
	private K min, max; // minimal and maximal key
	private long rootOffset;
	private BPNode<K, O> root;
	private int countIOs;
	private int numberOfEntries;
	private long lastOffset;
	private boolean opened;

	private ByteBuffer buffer;
	private int cacheCapacity = 10; // maximal number of nodes in cache (HashMap), must be at least 1
	private HashMap<Long, BPNode<K, O>> cache;
	private LinkedList<Long> cachedOffsets;
	private LinkedList<Long> freeOffsets;

	/**
	 * Creates new B+tree index. The index is not allocating the indexFile until
	 * some of the <code>open*</code> methods are called.
	 * 
	 * @param classO    class for objects stored in B+tree
	 * @param indexFile file in which the index will be stored
	 */
	@SuppressWarnings("unchecked")
	public BPTree(Class<O> classO, File indexFile) {
		nodeSize = 4096;
		this.classO = classO;
		this.indexFile = indexFile;
		O obj = null;
		try {
			obj = classO.newInstance();
		} catch (Exception e) {
			throw new Error("Class " + classO + "should have empty constructor!");
		}
		int objectSize = obj.getSize();
		int keySize = obj.getKey().getSize();
		classK = (Class<K>) obj.getKey().getClass();
		leafCapacity = (nodeSize - 21) / objectSize; // 16 for offsets of neighbors and 4 for number of entries, 1 for
														// leaf identification
		minLeafQuantity = ((leafCapacity - 1) / 2) + 1; // upper integer half of the leafCapacity
		internalNodeCapacity = (nodeSize - 13) / (keySize + 8); // 4 for number of keys, 1 for inner node
																// identification, 8 for 1 extra offset and with each
																// key one offset (8)
		minInternalNodeQuantity = internalNodeCapacity / 2; // lower integer half of the internalNodeCapacity
		countIOs = 0;
		numberOfEntries = 0;
		treeHeight = -1;
		freeOffsets = new LinkedList<Long>();
		opened = false;
	}

	/**
	 * Creates new B+tree index. The index is not allocating the indexFile until
	 * some of the <code>open*</code> methods are called.
	 * 
	 * @param classO    class for objects stored in B+tree
	 * @param indexFile file in which the index will be stored
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings("unchecked")
	public BPTree(Class<O> classO, File indexFile, File treeFile)
			throws FileNotFoundException, IOException, ClassNotFoundException {
		this.classO = classO;
		this.indexFile = indexFile;
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(treeFile));
		nodeSize = ois.readInt();
		rootOffset = ois.readLong();
		numberOfEntries = ois.readInt();
		freeOffsets = (LinkedList<Long>) ois.readObject();
		ois.close();
		O obj = null;
		try {
			obj = classO.newInstance();
		} catch (Exception e) {
			throw new Error("Class " + classO + "should have empty constructor!");
		}
		int objectSize = obj.getSize();
		int keySize = obj.getKey().getSize();
		classK = (Class<K>) obj.getKey().getClass();
		leafCapacity = (nodeSize - 21) / objectSize; // 16 for offsets of neighbors and 4 for number of entries, 1 for
														// leaf identification
		minLeafQuantity = ((leafCapacity - 1) / 2) + 1; // upper integer half of the leafCapacity
		internalNodeCapacity = (nodeSize - 13) / (keySize + 8); // 4 for number of keys, 1 for inner node
																// identification, 8 for 1 extra offset and with each
																// key one offset (8)
		minInternalNodeQuantity = internalNodeCapacity / 2; // lower integer half of the internalNodeCapacity
		countIOs = 0;
		treeHeight = -1;
		opened = false;
	}

	/**
	 * Stores this object to a file.
	 * 
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public void store(File file) throws FileNotFoundException, IOException {
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
		oos.writeInt(nodeSize);
		oos.writeLong(rootOffset);
		oos.writeInt(numberOfEntries);
		oos.writeObject(freeOffsets);
		oos.close();
	}

	/**
	 * This method changes nodeSize (default is 4096 Bytes). It also computes: <br>
	 * <br>
	 * <code>leafCapacity</code> - maximal number of entries in leaf, must be at
	 * least 2, is computed from nodeSize and objectSize, <br>
	 * <code>leafCapacity = (nodeSize - 21)/objectSize;</code> <br>
	 * <br>
	 * <code>minLeafQuantity</code> - minimal number of entries in leaf, upper
	 * integer half of the leafCapacity, <br>
	 * <code>minLeafQuantity = ((leafCapacity - 1) / 2) + 1;</code> <br>
	 * <br>
	 * <code>internalNodeCapacity</code> - maximal number of entries in inner node,
	 * must be at least 2, is computed from nodeSize and keySize and <br>
	 * <code>internalNodeCapacity = (nodeSize - 13) / (keySize + 8);</code> <br>
	 * <br>
	 * <code>minInternalNodeQuantity</code> - minimal number of entries in inner
	 * node, lower integer half of the internalNodeCapacity. <br>
	 * <code>minInternalNodeQuantity = internalNodeCapacity / 2;</code> <br>
	 * <br>
	 * This method can be called only if the tree has no entries and is closed.
	 * 
	 * @param nodeSize number of bytes per node
	 */
	public void setNodeSize(int nodeSize) {
		if (opened || numberOfEntries > 0) {
			throw new RuntimeException("Cannot change nodeSize over opened tree or to the non-empty tree.");
		}
		this.nodeSize = nodeSize;
		O obj = null;
		try {
			obj = classO.newInstance();
		} catch (Exception e) {
			throw new Error("Class " + classO + "should have empty constructor!");
		}
		int objectSize = obj.getSize();
		int keySize = obj.getKey().getSize();
		leafCapacity = (nodeSize - 21) / objectSize; // 16 for offsets of neighbors and 4 for number of entries, 1 for
														// leaf identification
		minLeafQuantity = ((leafCapacity - 1) / 2) + 1; // upper integer half of the leafCapacity
		internalNodeCapacity = (nodeSize - 13) / (keySize + 8); // 4 for number of keys, 1 for inner node
																// identification, 8 for 1 extra offset and with each
																// key one offset (8)
		minInternalNodeQuantity = internalNodeCapacity / 2; // lower integer half of the internalNodeCapacity
	}

	/**
	 * Changes cache capacity i.e. the number of nodes that can be stored in memory.
	 * Default is 10.
	 * 
	 * @param cacheCapacity new cache capacity
	 */
	public void setCacheCapacity(int cacheCapacity) {
		if (opened) {
			throw new RuntimeException("Cannot change cacheCapacity over opened tree.");
		}
		this.cacheCapacity = cacheCapacity;
	}

	/**
	 * Opens a new index file for read and write, if the file exists, it is reduced
	 * to zero size.
	 * 
	 * @throws IOException
	 */
	public void openNewFile() throws IOException {
		if (!indexFile.exists())
			indexFile.createNewFile();
		raf = new RandomAccessFile(indexFile, "rw");
		raf.setLength(0);
		channel = raf.getChannel();
		buffer = ByteBuffer.allocateDirect(nodeSize);
		root = new BPLeafNode<K, O>(this);
		rootOffset = root.offset;
		cachedOffsets = new LinkedList<Long>();
		cache = new HashMap<Long, BPNode<K, O>>(1 + ((cacheCapacity * 4) / 3));
		opened = true;
	}

	/**
	 * Opens a (not empty) B+tree index file for read and write (not concurrent).
	 * 
	 * @throws IOException
	 */
	public void open() throws IOException {
		raf = new RandomAccessFile(indexFile, "rw");
		channel = raf.getChannel();
		buffer = ByteBuffer.allocateDirect(nodeSize);
		cachedOffsets = new LinkedList<Long>();
		cache = new HashMap<Long, BPNode<K, O>>(1 + ((cacheCapacity * 4) / 3));
		if (root == null)
			root = loadBPNode(rootOffset);
		lastOffset = raf.length();
		opened = true;
	}

	/**
	 * Opens a (not empty) B+tree index file for read only. If more instances of the
	 * BPTree access to the file concurrently then all the instances must be opened
	 * for read only.
	 * 
	 * @throws IOException
	 */
	public void openForRead() throws IOException {
		raf = new RandomAccessFile(indexFile, "r");
		channel = raf.getChannel();
		buffer = ByteBuffer.allocateDirect(nodeSize);
		cachedOffsets = new LinkedList<Long>();
		cache = new HashMap<Long, BPNode<K, O>>(1 + ((cacheCapacity * 4) / 3));
		if (root == null)
			root = loadBPNode(rootOffset);
		opened = true;
	}

	/**
	 * Stores root the file if it was changed and closes the index file.
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		for (BPNode<K, O> node : cache.values()) {
			if (node.isChanged) {
				saveBPNode(node);
			}
		}
		if (root.isChanged)
			saveBPNode(root);
		raf.close();
		raf = null;
		channel = null;
		buffer = null;
		cachedOffsets = null;
		cache = null;
		opened = false;
	}

	/**
	 * Stores a node to the index file.
	 * 
	 * @param node Node to store
	 */
	private void saveBPNode(BPNode<K, O> node) {
		buffer.clear();
		node.save(buffer);
		buffer.rewind();
		try {
			if (raf.length() < (node.offset + nodeSize)) // offset should be smaller than file size
				raf.setLength(node.offset + nodeSize);
			channel.write(buffer, node.offset);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Unsuccessful reading from the index file to buffer!!!");
		}
	}

	/**
	 * Returns a node having a given offset from the index file.
	 * 
	 * @param offset Offset of a node to return.
	 * @return node with given offset.
	 */
	private BPNode<K, O> loadBPNode(long offset) {
		countIOs++;
		buffer.clear();
		try {
			channel.read(buffer, offset);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Unsuccessful reading from the index file to buffer!!!");
		}
		buffer.rewind();
		if (buffer.get() == (byte) 1)
			return new BPInnerNode<K, O>(offset, buffer, this);
		else
			return new BPLeafNode<K, O>(offset, buffer, this);

	}

	/**
	 * Returns a node having a given offset from the cache or from the index file.
	 * If cache is full, some node from a cache is stored to a file according to a
	 * remove policy.
	 * 
	 * @param offset Offset of a node to return.
	 * @return node with given offset.
	 */
	BPNode<K, O> getBPNode(long offset) {
		if (offset == root.offset) {
			return root;
		}
		BPNode<K, O> node;
		if ((node = cache.get(offset)) != null) {
			return node;
		} else {
			node = loadBPNode(offset);
			if (cache.size() == cacheCapacity) {
				BPNode<K, O> removedNode = cache.remove(cachedOffsets.removeLast());
				if (removedNode.isChanged) {
					saveBPNode(removedNode);
				}
			}
			cachedOffsets.addFirst(offset);
			cache.put(offset, node);
			return node;
		}
	}

	/**
	 * Inserts a node to the cache. If cache is full, some node from a cache is
	 * stored to a file according to a remove policy.
	 * 
	 * @param node Node to insert.
	 */
	void putBPNode(BPNode<K, O> node) {
		if (node == root)
			return; // root doesn't go to cache
		BPNode<K, O> pomNode;
		if ((pomNode = cache.get(node.offset)) != null) {
			if (node == pomNode)
				return;
			else {
				throw new RuntimeException("Two different nodes with the same offset in memory"); // there shouldn't be
																									// such a situation
			}
		} else {
			if (cache.size() == cacheCapacity) {
				BPNode<K, O> removedNode = cache.remove(cachedOffsets.removeLast());
				if (removedNode.isChanged) {
					saveBPNode(removedNode);
				}
			}
			cachedOffsets.addFirst(node.offset);
			cache.put(node.offset, node);
		}
	}

	/**
	 * Returns an offset for a new node
	 * 
	 * @return unused offset
	 */
	long getNewOffset() {
		if (freeOffsets.size() > 0)
			return freeOffsets.removeFirst();
		long pom = lastOffset;
		lastOffset += nodeSize;
		return pom;
	}

	/**
	 * This method is called before the node is removed. It sometimes happened after
	 * when an entry.
	 * 
	 * @param freeOffset offset of the removed node
	 */
	void addNewFreeOffset(long freeOffset) {
		freeOffsets.add(freeOffset);
		if (cache.remove(freeOffset) != null) {
			cachedOffsets.remove(freeOffset);
		}
	}

	/**
	 * This method is called when the last two nodes of the second level of the tree
	 * collapse to one root node It sometimes happened after when an entry.
	 * 
	 * @param newRoot
	 */
	void setNewRootAfterRemoving(BPNode<K, O> newRoot) {
		root = newRoot;
		rootOffset = root.offset;
		treeHeight--;
		if (cache.remove(root.offset) != null) {
			cachedOffsets.remove(root.offset);
		}
	}

	/**
	 * Adds a new entry to the B+tree
	 * 
	 * @param entry Entry to store
	 */
	public void add(O entry) {
		if (!opened) {
			throw new ManipulationWithClosedTreeException();
		}
		KeyOffsetPair<K> pairForNewRoot = root.add(entry);
		if (pairForNewRoot != null) {
			if (treeHeight >= 0)
				treeHeight++;
			BPNode<K, O> oldRoot = root;
			root = new BPInnerNode<K, O>(this);
			rootOffset = root.offset;
			((BPInnerNode<K, O>) root).addFromChild(oldRoot.offset, pairForNewRoot.key, pairForNewRoot.offset);
			putBPNode(oldRoot);
		}
		getMin();
		getMax();
		if (min == null) {
			max = min = entry.getKey();
		} else {
			if ((min.compareTo(entry.getKey())) > 0)
				min = entry.getKey();
			if ((max.compareTo(entry.getKey())) < 0)
				max = entry.getKey();
		}
		numberOfEntries++;
	}

	/**
	 * Opens a new indexFile and updates data to the empty tree. At the end all
	 * leafs are full except the most right one. Iterator must provide an ordered
	 * data from the smallest to the biggest according to the
	 * <code>compareTo()</code> function in a {@link BPObject} and {@link BPKey}
	 * implementations.
	 * 
	 * This update is much faster than entry-by-entry adding using <code>add</code>
	 * function and the result tree is smaller and faster when querying.
	 * 
	 * This method can add data as a new index only.
	 * 
	 * Typical use:
	 * 
	 * <pre>
	 * ArrayList<BPObjectIntDouble> values = new ArrayList<BPObjectIntDouble>(numberOfEntries);
	 * for (int i = 0; i < numberOfEntries; i++) {
	 * 	values.add(new BPObjectIntDouble((int) (Math.random() * 1000000000), Math.random()));
	 * }
	 * Collections.sort(values);
	 * BPTree<BPKeyInt, BPObjectIntDouble> tree = new BPTree<BPKeyInt, BPObjectIntDouble>(BPObjectIntDouble.class,
	 * 		new File("/var/tmp/indexBP.idx"));
	 * tree.openAndBatchUpdate(values.iterator(), values.size());
	 * for (BPObjectIntDouble entry : tree) {
	 * 	System.out.println(entry);
	 * }
	 * tree.close();
	 * </pre>
	 * 
	 * @param iterator Iterator that provides objects sorted entries
	 * @param size     number of objects to store by this function
	 * @throws IOException
	 */
	public void openAndBatchUpdate(Iterator<O> iterator, int size) throws IOException {
		numberOfEntries = size;
		if (!indexFile.exists())
			indexFile.createNewFile();
		raf = new RandomAccessFile(indexFile, "rw");
		raf.setLength(0);
		channel = raf.getChannel();
		buffer = ByteBuffer.allocateDirect(nodeSize);
		cachedOffsets = new LinkedList<Long>();
		cache = new HashMap<Long, BPNode<K, O>>(1 + ((cacheCapacity * 4) / 3));
		opened = true;
		ArrayList<Integer> maxSizes = new ArrayList<Integer>();
		maxSizes.add(leafCapacity);
		if (size > leafCapacity) {
			root = new BPInnerNode<K, O>(this);
			treeHeight = 1;
			int maxSize = leafCapacity * (internalNodeCapacity + 1);
			maxSizes.add(maxSize);
			while (maxSize < size) {
				maxSize *= (internalNodeCapacity + 1);
				maxSizes.add(maxSize);
				treeHeight++;
			}
		} else {
			root = new BPLeafNode<K, O>(this);
			treeHeight = 0;
		}
		rootOffset = root.offset;
		min = root.batchUpdate(iterator, size, treeHeight, maxSizes, -1, -1);
	}

	/**
	 * Removes entry from the index.
	 * 
	 * @param entry Entry to remove
	 * @return true if the entry has been found and erased, false if there has been
	 *         no such entry in a tree
	 */
	public boolean remove(O entry) {
		if (!opened) {
			throw new ManipulationWithClosedTreeException();
		}
		boolean result = root.remove(entry, true, getTreeHeight());
		if (result)
			numberOfEntries--;
		return result;
	}

	/**
	 * Removes all entries with the given tree having the given key
	 * 
	 * @param key Key of the entries to remove
	 * @return true if the entry (entries) has been found and erased, false is there
	 *         has been no such entry with a given key in a tree
	 */
	public boolean remove(K key) {
		if (!opened) {
			throw new ManipulationWithClosedTreeException();
		}
		List<O> list = getListForKey(key);
		if (list == null)
			return false;
		for (O entry : list) {
			remove(entry);
		}
		return true;
	}

	/**
	 * Returns the tree height
	 * 
	 * @return tree height
	 */
	public int getTreeHeight() {
		if (!opened) {
			throw new ManipulationWithClosedTreeException();
		}
		return treeHeight < 0 ? root.getTreeHeight() : treeHeight;
	}

	/**
	 * Returns true if this index contains a given entry.
	 * 
	 * @param entry Entry to be found
	 * @return true if this index contains a given entry.
	 */
	public boolean isInTree(O entry) {
		if (!opened) {
			throw new ManipulationWithClosedTreeException();
		}
		BPLeafNode<K, O> leaf = root.findLeft(entry);
		return Arrays.binarySearch(leaf.entries, 0, leaf.numberOfEntries, entry) >= 0;
	}

	/**
	 * Returns one entry for a given key. This method can be used, if the data in
	 * B+tree are unique.
	 * 
	 * @param key Key of the entry to return.
	 * @return one entry with a given key or <code>null</code> if there is no such
	 *         entry.
	 */
	public O get(K key) {
		if (!opened) {
			throw new ManipulationWithClosedTreeException();
		}
		BPLeafNode<K, O> leaf = root.findLeafLeft(key);
		int position = leaf.binarySearch(key);
		return position >= 0 ? leaf.entries[position] : null;
	}

	/**
	 * Returns a {@link List} of entries for a given key. This method can be used,
	 * if the data in B+tree are not unique. List is organized in ascending order.
	 * 
	 * @param key Key value of the entries to return.
	 * @return {@link List} of entries with a given key or <code>null</code> if
	 *         there is no such entry.
	 */
	public List<O> getListForKey(K key) {
		if (!opened) {
			throw new ManipulationWithClosedTreeException();
		}
		Iterator<O> it = getIteratorForKey(key);
		if (!it.hasNext())
			return null;
		LinkedList<O> list = new LinkedList<O>();
		while (it.hasNext()) {
			list.add(it.next());
		}
		return list;
	}

	/**
	 * Returns an {@link Iterator} of entries for a given key. This method can be
	 * used, if the data in B+tree are not unique. Iterator returns entries in
	 * ascending order.
	 * 
	 * @param key Key value of the entries to return.
	 * @return {@link Iterator} of entries with a given key.
	 */
	public Iterator<O> getIteratorForKey(K key) {
		if (!opened) {
			throw new ManipulationWithClosedTreeException();
		}
		return new ItrForKey(key);
	}

	private class ItrForKey implements Iterator<O> {
		BPLeafNode<K, O> leaf;
		int cursor; // index of next element to return
		K key;

		public ItrForKey(K key) {
			this.key = key;
			leaf = root.findLeafLeft(key);
			cursor = leaf.getLeftObjectPosition(key);
			if (cursor < 0)
				leaf = null;
		}

		public boolean hasNext() {
			return (leaf != null) && (leaf.entries[cursor].getKey().equals(key));
		}

		public O next() {
			O result = leaf.entries[cursor++];
			if (cursor == leaf.numberOfEntries) {
				leaf = leaf.getRightNode();
				cursor = 0;
			}
			return result;
		}

		public void remove() {
			throw new RuntimeException("Cannot remove from B+tree during iteration.");
		}
	}

	/**
	 * Returns minimal key.
	 * 
	 * @return minimal key.
	 */
	public K getMin() {
		if (min == null && numberOfEntries > 0) {
			BPLeafNode<K, O> leftLeaf = root.findLeafLeft();
			min = leftLeaf.entries[0].getKey();
		}
		return min;
	}

	/**
	 * Returns maximal key.
	 * 
	 * @return maximal key.
	 */
	public K getMax() {
		if (max == null && numberOfEntries > 0) {
			BPLeafNode<K, O> rightLeaf = root.findLeafRight();
			max = rightLeaf.entries[rightLeaf.numberOfEntries - 1].getKey();
		}
		return max;
	}

	/**
	 * resets the number of Input/output operations to the disk
	 */
	public void resetCountIOs() {
		countIOs = 0;
	}

	/**
	 * Returns the number of Input/output operations to the disk since the creation
	 * the instance of the BPTree of since the call of the
	 * <code>resetCountIOs</code> function.
	 * 
	 * @return the number of Input/output operations to the disk
	 */
	public int getCountIOs() {
		return countIOs;
	}

	/**
	 * Returns the number of entries in the index.
	 * 
	 * @return the number of entries in teh index.
	 */
	public int getNumberOfEntries() {
		return numberOfEntries;
	}

	@Override
	public String toString() {
		if (!opened) {
			throw new ManipulationWithClosedTreeException();
		}
		String vystup = "<<< BPTree >>>\r\n";
		vystup += root.toString("");
		return vystup;
	}

	/**
	 * Returns an iterator over the elements in this BPTree in proper sequence.
	 *
	 * @return an iterator over the elements in this BPTree in proper sequence
	 */
	public Iterator<O> iterator() {
		return new Itr();
	}

	private class Itr implements Iterator<O> {
		BPLeafNode<K, O> leaf = root.findLeafLeft();
		int cursor = 0; // index of next element to return

		public boolean hasNext() {
			return (leaf != null);
		}

		public O next() {
			O result = leaf.entries[cursor++];
			if (cursor == leaf.numberOfEntries) {
				leaf = leaf.getRightNode();
				cursor = 0;
			}
			return result;
		}

		public void remove() {
			throw new RuntimeException("Cannot remove from B+tree during iteration.");
		}
	}

	/**
	 * Returns an iterator over the elements in this BPTree in inverse sequence.
	 *
	 * @return an iterator over the elements in this BPTree in inverse sequence.
	 */
	public Iterator<O> inverseIterator() {
		return new InverseItr();
	}

	private class InverseItr implements Iterator<O> {
		BPLeafNode<K, O> leaf = root.findLeafRight();
		int cursor = leaf.numberOfEntries - 1; // index of next element to return

		public boolean hasNext() {
			return (leaf != null);
		}

		public O next() {
			O result = leaf.entries[cursor--];
			if (cursor == -1) {
				leaf = leaf.getLeftNode();
				cursor = leaf.numberOfEntries - 1;
			}
			return result;
		}

		public void remove() {
			throw new RuntimeException("Cannot remove from B+tree during iteration.");
		}
	}

	public List<O> intervalQuery(K low, K high) {
		if (!opened) {
			throw new ManipulationWithClosedTreeException();
		}
		BPLeafNode<K, O> leaf = root.findLeafLeft(low);
		int position = leaf.binarySearch(low);
		if (position < 0) {
			position = -1 - position;
		}
		List<O> result = new LinkedList<>();
		while (true) {
			if (position == leaf.numberOfEntries) {
				leaf = leaf.getRightNode();
				position = 0;
			}
			if (leaf == null) {
				break;
			}
			O obj = leaf.entries[position++];
			if(0 >= obj.getKey().compareTo(high) ) {
				result.add(obj);
			} else {
				break;
			}
		}
		return result;
	}
}
