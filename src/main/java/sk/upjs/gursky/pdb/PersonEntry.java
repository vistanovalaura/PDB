package sk.upjs.gursky.pdb;

import java.nio.ByteBuffer;

import sk.upjs.gursky.bplustree.BPObject;

public class PersonEntry implements BPObject<PersonStringKey, PersonEntry> {

	private static final long serialVersionUID = -3849588860748391617L;

	String name, surname;
	int age, salary;

	public PersonEntry() {
	}

	public PersonStringKey getKey() {
		return new PersonStringKey(surname);
	}

	public SalaryKey getSalaryKey() {
		return new SalaryKey(salary);
	}

	public int getSize() {
		return 48;
	}

	public void save(ByteBuffer bb) {

		for (int k = 0; k < 10; k++) {
			bb.putChar(name.charAt(k));
		}
		for (int k = 0; k < 10; k++) {
			bb.putChar(surname.charAt(k));
		}
		bb.putInt(age);
		bb.putInt(salary);
	}

	public void load(ByteBuffer bb) {

		char[] data = new char[10];

		for (int i = 0; i < 10; i++) {
			data[i] = bb.getChar();
		}
		name = new String(data);
		for (int i = 0; i < 10; i++) {
			data[i] = bb.getChar();
		}
		surname = new String(data);
		age = bb.getInt();
		salary = bb.getInt();
	}

	@Override
	public String toString() {

		return name + " " + surname + " " + age + " " + salary + System.getProperty("line.separator");
	}

	public int compareTo(PersonEntry zaznam) {
		return Integer.compare(this.salary, zaznam.salary);

	}
}