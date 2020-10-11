package sk.upjs.gursky.bplustree.entries;

import java.io.Serializable;
import java.nio.ByteBuffer;

import sk.upjs.gursky.bplustree.BPKey;


public class BPKeyLong implements BPKey<BPKeyLong>, Serializable
{

	private static final long serialVersionUID = 809951016755954693L;
	private long key;
	
	public BPKeyLong() {}
	
	public BPKeyLong(long key)
	{
		this.key = key;
	}
	
	public void load(ByteBuffer bb)
	{
		key = bb.getLong();
	}
	
	public void save(ByteBuffer bb)
	{
		bb.putLong(key);
	}
	
	public int getSize()
	{
		return 8;
	}

	@Override
	public String toString()
	{
		return new Long(key).toString();
	}
	
	/**
	 * Compares this object with the specified object for order. 
	 * Returns a negative integer, zero, or a positive integer as this object is less than, 
	 * equal to, or greater than the specified object.
	 * 
	 * @param o The object to be compared. 
	 * @return A negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object. 
	 */
	public int compareTo(BPKeyLong o){
		if (key < o.key) return -1;
		if (key > o.key) return 1;
		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (! (obj instanceof BPKeyLong)) return false;
		return key == ((BPKeyLong)obj).key;
	}

	public long getKeyLong() {
		return key;
	}

}
