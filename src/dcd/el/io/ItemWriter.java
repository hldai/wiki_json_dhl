package dcd.el.io;

import java.io.BufferedWriter;
import java.io.IOException;

public class ItemWriter {
	public ItemWriter(String fileName) {
		writer = IOUtils.getUTF8BufWriter(fileName);
	}
	
	public ItemWriter() {
		
	}
	
	public void open(String fileName) {
		writer = IOUtils.getUTF8BufWriter(fileName);
	}
	
	public void writeItem(Item item) {
		try {
			item.fixNumLines();
			writer.write(item.key + " " + item.numLines + "\n");
			writer.write(item.value);
			if (item.numLines > 0)
				writer.write("\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void close() {
		if (writer != null) {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	BufferedWriter writer = null;
}
