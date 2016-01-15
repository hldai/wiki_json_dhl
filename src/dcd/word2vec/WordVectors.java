package dcd.word2vec;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import dcd.el.io.IOUtils;
import dcd.el.objects.ByteArrayString;
import dcd.el.utils.CommonUtils;

public class WordVectors {
	public static class WordVectorPair implements Comparable<WordVectorPair> {
		public ByteArrayString word;
		public float[] vector;
		
		@Override
		public int compareTo(WordVectorPair wvpr) {
			return this.word.compareTo(wvpr.word);
		}
	}
	
	public WordVectors(String fileName) {
		try {
			BufferedWriter writer = IOUtils.getUTF8BufWriter("e:/el/word2vec/dict.txt", false);
//			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("e:/el/word2vec/dict.txt"));
			
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fileName));
			byte[] intBytes = new byte[Integer.BYTES];
			bis.read(intBytes);
			int numWords = CommonUtils.getLittleEndianInt(intBytes);
			wordVectorPairs = new WordVectorPair[(int)numWords];
			System.out.println(numWords);
			
			bis.read(intBytes);
			int vecLen = CommonUtils.getLittleEndianInt(intBytes);
			System.out.println(vecLen);
			
			for (int i = 0; i < numWords; ++i) {
				int len = bis.read();
				ByteArrayString word = new ByteArrayString();
				word.bytes = new byte[len];
				bis.read(word.bytes);
//				word.toUTF8();
//				System.out.println(word.toString());
				writer.write(word.toString() + "\n");
//				bos.write(word.bytes);
//				bos.write(10);
				
				byte[] vecBytes = new byte[Float.BYTES * vecLen];
				bis.read(vecBytes);
				float[] vec = CommonUtils.getLittleEndianFloatArray(vecBytes);
				
				wordVectorPairs[i] = new WordVectorPair();
				wordVectorPairs[i].word = word;
				wordVectorPairs[i].vector = vec;

//				if (i == 10) break;
//				System.out.println(i);
				if (i % 100000 == 0)
					System.out.println(i);
			}

//			bos.close();
			writer.close();
			bis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Arrays.sort(wordVectorPairs);
	}
	
	public float[] getVector(String word) {
		WordVectorPair wordVectorPair = new WordVectorPair();
		wordVectorPair.word = new ByteArrayString(word);
		
		int pos = Arrays.binarySearch(wordVectorPairs, wordVectorPair);
		if (pos < 0)
			return null;
		
		return wordVectorPairs[pos].vector;
	}
	
	WordVectorPair[] wordVectorPairs = null;
}
