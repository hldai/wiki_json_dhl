package dcd.el.io;

import java.io.BufferedReader;
import java.io.IOException;


public class ItemReader {
	public ItemReader(String fileName, boolean isGZip) {
		if (isGZip) {
			reader = IOUtils.getGZIPBufReader(fileName);
		} else {
			reader = IOUtils.getUTF8BufReader(fileName);
		}
	}
	
	public void open(String fileName, boolean isGZip) {
		if (isGZip) {
			reader = IOUtils.getGZIPBufReader(fileName);
		} else {
			reader = IOUtils.getUTF8BufReader(fileName);
		}
	}
	
	public Item readNextItem() {
		String[] pair = null;
		String line = null;
		try {
			line = reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (line == null)
			return null;
		
//		System.out.println(line);
		Item item = new Item();
		pair = line.split(" ");
		item.key = pair[0];
		item.numLines = Integer.valueOf(pair[1]);
		item.value = IOUtils.readLines(reader, item.numLines);
//		System.out.println(value);
		
		return item;
	}
	
	public void close() {
		try {
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	BufferedReader reader = null;
}
