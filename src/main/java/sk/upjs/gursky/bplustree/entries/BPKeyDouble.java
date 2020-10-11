package sk.upjs.gursky.bplustree.entries;

import java.io.Serializable;
import java.nio.ByteBuffer;

import sk.upjs.gursky.bplustree.BPKey;


public class BPKeyDouble implements BPKey<BPKeyDouble>, Serializable
{

	private static final long serialVersionUID = -6511749774122198926L;
	private double key;
	
	public BPKeyDouble() {}
	
	public BPKeyDouble(double key)
	{
		this.key = key;
	}
	
	public void load(ByteBuffer bb)
	{
		key = bb.getDouble();
	}
	
	public void save(ByteBuffer bb)
	{
		bb.putDouble(key);
	}
	
	public int getSize()
	{
		return 8;
	}

	@Override
	public String toString()
	{
		return new Double(key).toString();
	}
	
	/**
	 * Compares this object with the specified object for order. 
	 * Returns a negative integer, zero, or a positive integer as this object is less than, 
	 * equal to, or greater than the specified object.
	 * 
	 * @param o The object to be compared. 
	 * @return A negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object. 
	 */
	public int compareTo(BPKeyDouble o){
		if (key < o.key) return -1;
		if (key > o.key) return 1;
		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (! (obj instanceof BPKeyDouble)) return false;
		return key == ((BPKeyDouble)obj).key;
	}

	public double getKeyLong() {
		return key;
	}

}
