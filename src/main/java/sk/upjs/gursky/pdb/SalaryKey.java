package sk.upjs.gursky.pdb;

import java.nio.ByteBuffer;

import sk.upjs.gursky.bplustree.BPKey;

public class SalaryKey implements BPKey<SalaryKey> {

	private static final long serialVersionUID = -1062718514927725257L;
	private int key;
	
	public SalaryKey() {
		
	}
	
	public SalaryKey(int key) {
		this.key = key;
	}
	
	public int getSize() {
		return 4;
	}
	
	public void load(ByteBuffer bb) {
		key = bb.getInt();
	}
	
	public void save(ByteBuffer bb) {
		bb.putInt(key);
	}
	
	public int compareTo(SalaryKey salaryKey) {
		return Integer.compare(this.key, salaryKey.key); 
	}
}
