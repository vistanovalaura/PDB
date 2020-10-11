package sk.upjs.gursky.pdb;

import java.nio.ByteBuffer;

import sk.upjs.gursky.bplustree.BPKey;

public class PersonStringKey implements BPKey<PersonStringKey> {

	private static final long serialVersionUID = 2784314044852195831L;
	private String key;
	
	public PersonStringKey() {}
	
	public PersonStringKey(String key) {
		
		this.key = key;
	}
	
	public int getSize() {
		
		return 20;
	}
	
	public void load(ByteBuffer bb) {
		
		char[] data = new char[10];
		
		for (int i = 0; i < 10; i++) {
			data[i] = bb.getChar();
		}
		key = new String(data);
	}
	
	public void save(ByteBuffer bb) {
		
		for (int k = 0; k < 10; k++) {
			bb.putChar(key.charAt(k));
		}
	}
	
	public int compareTo(PersonStringKey personStringKey) {
		
		return key.compareTo(personStringKey.key);
	}
}
