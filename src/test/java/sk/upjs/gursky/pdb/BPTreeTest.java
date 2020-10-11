package sk.upjs.gursky.pdb;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import sk.upjs.gursky.bplustree.BPTree;
import sk.upjs.gursky.bplustree.entries.BPKeyInt;
import sk.upjs.gursky.bplustree.entries.BPObjectIntDouble;

public class BPTreeTest {

	private static final File INDEX_FILE = new File("TESTindexBP.idx");
	private BPTree<BPKeyInt, BPObjectIntDouble> tree;
	
	@Before
	public void setUp() throws Exception {
		tree =  new BPTree<BPKeyInt, BPObjectIntDouble>(BPObjectIntDouble.class, INDEX_FILE);
	}

	@After
	public void tearDown() throws Exception {
        tree.close();
        INDEX_FILE.delete();
	}

	@Test
	public void test() throws Exception {
        tree.setNodeSize(8192);         //default is 4096
        tree.setCacheCapacity(100);     //default is 10
        tree.openNewFile();
        for (int i = 0; i < 100; i++) {
            tree.add(new BPObjectIntDouble((int)(Math.random()*1000000000),Math.random()));
        }
        BPObjectIntDouble prev = null;
        for (BPObjectIntDouble entry : tree) {
            System.out.println(entry);
            if (prev != null) {
            	assertTrue(prev.getKey().getKeyInt() < entry.getKey().getKeyInt());
            }
            prev = entry;
        }
	}

}
