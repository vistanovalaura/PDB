package sk.upjs.gursky.bplustree.entries;

import java.io.Serializable;
import java.nio.ByteBuffer;

import sk.upjs.gursky.bplustree.BPObject;


public class BPObjectIntDouble implements BPObject<BPKeyInt,BPObjectIntDouble>, Serializable
{
	private static final long serialVersionUID = 7740691381498020504L;
	private int        id;
	private double     value;
	
	public BPObjectIntDouble() {}
	
	public BPObjectIntDouble(int id, double value)
	{
		this.id = id;
		this.value = value;
	}
	
	public void load(ByteBuffer bb)
	{
		id = bb.getInt();
		value = bb.getDouble();
	}
	
	public void save(ByteBuffer bb)
	{
		bb.putInt(id);
		bb.putDouble(value);
	}
	
	public int getSize()
	{
		return 12;
	}
	
	@Override
	public BPKeyInt getKey() {
		return new BPKeyInt(id);
	}

	@Override
	public String toString()
	{
		return "[" + id + "," + value + "]";
	}
	
	/**
	 * Compares this object with the specified object for order. 
	 * Returns a negative integer, zero, or a positive integer as this object is less than, 
	 * equal to, or greater than the specified object.
	 * 
	 * @param o The object to be compared. 
	 * @return A negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object. 
	 */
	@Override
	public int compareTo(BPObjectIntDouble o){
		if (id < o.id) return -1;
		if (id > o.id) return 1;
		if (value < o.value) return -1;
		if (value > o.value) return 1;
		return 0;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (! (obj instanceof BPObjectIntDouble)) return false;
		return id == ((BPObjectIntDouble)obj).id && value == ((BPObjectIntDouble)obj).value ? true : false;
	}

	public double getValueDouble() {
		return value;
	}
}
