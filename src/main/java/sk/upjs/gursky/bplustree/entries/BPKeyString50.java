package sk.upjs.gursky.bplustree.entries;

import java.io.Serializable;

import sk.upjs.gursky.bplustree.BPKeyString;

/**
 * This is an example of the body (for anonymous class - the best way to set the maximal length of the strings) 
 * that represents a String like a VARCHAR(50) in databases
 * methods getSize() and getMaxLength() must return a constant (final) value i.e. not key.length
 * return value for getSize() method is computed as getMaxLength() * 2 + 4.
 */
public class BPKeyString50 extends BPKeyString implements Serializable {
	
	private static final long serialVersionUID = -2887094654804099617L;

	public BPKeyString50() {}
	
	public BPKeyString50(String key) {
		super(key);
	}
	
	public int getSize() {
		return 104;
	}

	/**
	 * Returns maximal number of characters in a string. Method must return a constant (final static) value for all instances.
	 * It must hold that <code>getSize() == getMaxLength() * 2 + 4</code>
	 * 
	 * @return maximal number of characters in a string
	 */
	protected int getMaxLength() {	
		return 50;
	}
}
