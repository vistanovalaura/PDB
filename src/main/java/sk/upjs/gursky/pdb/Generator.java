package sk.upjs.gursky.pdb;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Random;

public class Generator {
	
	public static final int NUMBER_OF_PAGES = 10000;
	public static final File GENERATED_FILE = new File("person.tab"); 
	
	public static void main(String[] args) throws Exception {
		generateFile(NUMBER_OF_PAGES, GENERATED_FILE);
	}
	
	/**
	 * Vytvori subor so zadanym menom/umiestnenim a poctom stranok velkosti 4096B naplnenych nahodnymi datami.
	 * Na jednu stranku sa vojde 85 zaznamov, menej zaznamov (ale nahodne) bude vzdy obsahovat posledna stranka.
	 * Subor obsahuje zaznamy pre tabulku podla tejto definicie
	 * 
	 * create table clovek (
	 * 		meno char(10),
	 *  	priezvisko char(10),
	 *  	vek int,
	 *  	plat int
	 * )
	 * 
	 * Jeden zaznam ma teda 48B a v subore su vramci kazdej stranky radene bezprostredne za sebou s tym, 
	 * ze kazda stranka ma na prvych styroch bytoch pocet zaznamov, ktore obsahuje. Stranky su tiez radene 
	 * bezprostredne za sebou.
	 * 
	 * @param numberOfPages - kolko stranok naplnenych nahodnymi datami sa ma vytvorit
	 * @param f - subor, ktory sa ma vytvorit
	 * @throws Exception
	 */
	public static void generateFile(int numberOfPages, File f) throws Exception {
		
		Random r = new Random();
		
		if (!f.exists()) {
			f.createNewFile();
		}
		RandomAccessFile raf = new RandomAccessFile(f, "rw");
		
		FileChannel channel = raf.getChannel();
		ByteBuffer  buffer = ByteBuffer.allocateDirect(4096);
		
		for (int i = 0; i < numberOfPages; i++) {
			int z = 85;
			if (i == numberOfPages - 1) {
				z = (int) (Math.random() * 84 + 1);
			}
			buffer.clear();
			buffer.putInt(z);
			for (int j = 0; j < z; j++) {
				String str = Long.toString(Math.abs(r.nextLong()), 36);
				
				for (int k = 0; k < 10; k++) {
					try {
						buffer.putChar(str.charAt(k));
					} catch (StringIndexOutOfBoundsException e) {
						buffer.putChar(' ');
					}
				}
				str = Long.toString(Math.abs(r.nextLong()), 36);
				for (int k = 0; k < 10; k++) {
					try {
						buffer.putChar(str.charAt(k));
					} catch (StringIndexOutOfBoundsException e) {
						buffer.putChar(' ');
					}
				}
				buffer.putInt((int) (Math.random() * 50 + 18));
				buffer.putInt((int) (Math.random() * 2000 + 300));
			}
			buffer.rewind();
			channel.write(buffer, i * 4096);
		}
		
		channel.close();
		raf.close();
	}
}