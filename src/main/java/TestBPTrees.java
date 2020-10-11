import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import sk.upjs.gursky.bplustree.BPTree;
import sk.upjs.gursky.bplustree.entries.BPKeyInt;
import sk.upjs.gursky.bplustree.entries.BPObjectIntDouble;

public class TestBPTrees {

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		int numberOfEntries = 50000;
		String indexFile1 = "/var/tmp/indexBP.idx";
		String indexFile2 = "/var/tmp/indexBP2.idx";
		String treeObjectFile = "/var/tmp/BPtree.obj";
		//generating entries
		ArrayList<BPObjectIntDouble> values = new ArrayList<BPObjectIntDouble>(numberOfEntries);
		for (int i = 0; i < numberOfEntries; i++) {
			values.add(new BPObjectIntDouble((int)(Math.random()*1000000000),Math.random()));
		}
		//creating first BPTree with entries added one-by-one
		BPTree<BPKeyInt, BPObjectIntDouble> tree =  new BPTree<BPKeyInt, BPObjectIntDouble>(BPObjectIntDouble.class,new File(indexFile1));
//		tree.setNodeSize(1024);
		tree.setCacheCapacity(100);
		tree.openNewFile();
		long time1 =  System.currentTimeMillis();
		for (int i = 0; i < values.size(); i++) {
			tree.add(values.get(i));
		}
		time1 = System.currentTimeMillis() - time1;
		System.out.println("Time of creation 1: "+time1 + " ms");

		//creating second BPTree with entries added by batch update
		BPTree<BPKeyInt, BPObjectIntDouble> tree2 =  new BPTree<BPKeyInt, BPObjectIntDouble>(BPObjectIntDouble.class,new File(indexFile2));
//		tree2.setNodeSize(1024);
		tree2.setCacheCapacity(100);
		Collections.sort(values);
		long time2 =  System.currentTimeMillis();
		tree2.openAndBatchUpdate(values.iterator(), values.size());
		time2 = System.currentTimeMillis() - time2;
		System.out.println("Time of creation 2: "+time2 + " ms");
		
		//we can store the tree for a future use
		tree.close();
		tree.store(new File(treeObjectFile));
		tree = null;
		
		//later (in different program/process) we can load it back 
		tree = new BPTree<BPKeyInt, BPObjectIntDouble>(BPObjectIntDouble.class,new File(indexFile1),new File(treeObjectFile));;
		tree.open();
		//follows the test that both trees has the same content as the source ArrayList
		Iterator<BPObjectIntDouble> it1 = tree.iterator();
		Iterator<BPObjectIntDouble> it2 = tree2.iterator();
		boolean ok = true;
		int i=0;
		while (it2.hasNext()) {
			BPObjectIntDouble o1 = it1.next();
			BPObjectIntDouble o2 = it2.next();
			BPObjectIntDouble o3 = values.get(i++);			
			if (! o1.equals(o2)) {
				System.out.println(i + ":  " + o1 + "  " + o2 + "  " + o3);
				System.out.println("!!!");
				ok = false;
			}
		}
		System.out.println(ok);
		
		//now we are going to remove the second half of data from the trees
		long time3 = System.currentTimeMillis();
		for (i = numberOfEntries/2; i < numberOfEntries;i++) {
			if (! tree.remove(values.get(i))) System.out.println(values.get(i)+" entry " + values.get(i)+ " not found in tree 1");
			if (! tree2.remove(values.get(i))) System.out.println(values.get(i)+" entry " + values.get(i)+ " not found in tree 2");
		}
		time3 = System.currentTimeMillis() - time3;
		System.out.println("Time of deletion one half of both trees: "+time3 + " ms");
		
		//follows the test that both trees has the same content as first half of the source ArrayList
		it1 = tree.iterator();
		it2 = tree2.iterator();
		for (i = 0; i < numberOfEntries/2; i++) {
			BPObjectIntDouble o1 = it1.next();
			BPObjectIntDouble o2 = it2.next();
			BPObjectIntDouble o3 = values.get(i);			
			if (! (o1.equals(o2) && o2.equals(o3))) {
				System.out.println(i + ":  " + o1 + "  " + o2 + "  " + o3);
				System.out.println("!!!");
				ok = false;
			}
		}
		System.out.println(ok);
		
		tree.close();
		tree2.close();
	}
}

