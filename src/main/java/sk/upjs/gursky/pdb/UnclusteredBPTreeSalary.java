package sk.upjs.gursky.pdb; 

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import sk.upjs.gursky.bplustree.BPTree;

public class UnclusteredBPTreeSalary  extends BPTree<SalaryKey, SalaryOffsetEntry> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3406487597457912466L;
	
	public static final int PAGE_SIZE = 4096;
	
	private File personsFile;

	public UnclusteredBPTreeSalary(File indexFile, File personsFile) {
		super(SalaryOffsetEntry.class, indexFile);
		this.personsFile = personsFile;
	}
	
	public static UnclusteredBPTreeSalary newTreeBulkLoading(File personsFile, File indexFile) throws IOException {
		UnclusteredBPTreeSalary tree = new UnclusteredBPTreeSalary(indexFile, personsFile);
		tree.setNodeSize(PAGE_SIZE);
		RandomAccessFile raf = new RandomAccessFile(personsFile, "r");

		FileChannel channel = raf.getChannel();
		ByteBuffer buffer = ByteBuffer.allocateDirect(PAGE_SIZE);

		List<SalaryOffsetEntry> pairs = new ArrayList<>();
		long fileSize = personsFile.length();
		long pagesCount = fileSize / PAGE_SIZE;
		for (int offset = 0; offset < fileSize; offset += PAGE_SIZE) {
			// System.out.println("Citam " + (offset / PAGE_SIZE) + ". stranku");
			buffer.clear();
			channel.read(buffer, offset);
			buffer.rewind();
			int personsCount = buffer.getInt();
			for (int i = 0; i < personsCount; i++) {
				PersonEntry personEntry = new PersonEntry();
				personEntry.load(buffer);
				pairs.add(new SalaryOffsetEntry(personEntry.salary, offset + 4 + (i * personEntry.getSize())));
			}
		}
		channel.close();
		raf.close();
		Collections.sort(pairs);
		tree.openAndBatchUpdate(pairs.iterator(), pairs.size());
		return tree;
	}
	
	public List<PersonEntry> intervalQueryEntries(SalaryKey low, SalaryKey high) throws IOException {
		List<SalaryOffsetEntry> pairs = super.intervalQuery(low, high);
		RandomAccessFile raf = new RandomAccessFile(personsFile, "r");
		FileChannel channel = raf.getChannel();
		ByteBuffer buffer = ByteBuffer.allocateDirect(PAGE_SIZE);
		List<PersonEntry> entries = new LinkedList<PersonEntry>();
		for (SalaryOffsetEntry pair : pairs) {
			buffer.clear();
			long pageOffset = (pair.getOffset() / PAGE_SIZE) * PAGE_SIZE;
			int bufferOffset = (int) (pair.getOffset() - pageOffset);
			channel.read(buffer, pageOffset);
			buffer.position(bufferOffset);
			PersonEntry personEntry = new PersonEntry();
			personEntry.load(buffer);
			entries.add(personEntry);
		}
		channel.close();
		raf.close();
		return entries;
	}

}
