package sk.upjs.gursky.pdb;

import java.nio.ByteBuffer;

import sk.upjs.gursky.bplustree.BPObject;

public class SalaryOffsetEntry implements BPObject<SalaryKey, SalaryOffsetEntry> {

	private static final long serialVersionUID = -4574151963333928340L;
	
	int salary; 
	long offset;
	
	public SalaryOffsetEntry() {
		
	}	

	public SalaryOffsetEntry(int salary, long offset) {
		super();
		this.salary = salary;
		this.offset = offset;
	}


	@Override
	public int compareTo(SalaryOffsetEntry s) {
		if (this.salary < s.salary) 
			return -1;
		if (this.salary > s.salary)
			return 1;		
		return 0;
	}

	@Override
	public void load(ByteBuffer bb) {		
		salary = bb.getInt();
		offset = bb.getLong();
	}

	@Override
	public void save(ByteBuffer bb) {
		bb.putInt(salary);
		bb.putLong(offset);

	}

	@Override
	public int getSize() {
		return 12;
	}

	@Override
	public SalaryKey getKey() {
		return new SalaryKey(salary);
	}
	
	public int getSalary() {
		return salary;
	}
	
	public long getOffset() {
		return offset;
	}

	@Override
	public String toString() {
		return "SalaryAndOffsetEntry [salary=" + salary + ", offset=" + offset + "]";
	}

}
