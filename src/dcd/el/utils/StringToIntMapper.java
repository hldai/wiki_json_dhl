package dcd.el.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;

import dcd.el.io.IOUtils;

public class StringToIntMapper {
	public StringToIntMapper(String mapFileName) {
		BufferedReader reader = IOUtils.getUTF8BufReader(mapFileName);
		int numLines = IOUtils.getNumLinesFor(mapFileName);
		keys = new String[numLines];
		values = new int[numLines];
		try {
			for (int i = 0; i < numLines; ++i) {
				String line = reader.readLine();
				String[] parts = line.split("\t");
				keys[i] = parts[0];
				values[i] = Integer.valueOf(parts[1]);
			}
			
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Integer getValue(String key) {
		int pos = Arrays.binarySearch(keys, key);
		if (pos < 0)
			return null;
		return values[pos];
	}
	
	private String[] keys = null;
	private int[] values = null;
}
