package dcd.el.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import dcd.el.ELConsts;
import dcd.el.io.IOUtils;

public class TupleFileTools {
	public static final int NUM_CHARS_LIM = 1024 * 1024 * 256;
	public static final int NUM_LINE_LIM = 10000000;
	private static final DecimalFormat DEC_FORMAT = new DecimalFormat("0000");
	
	public static class SingleFieldComparator implements Comparator<String> {
		public SingleFieldComparator(int fieldIdx) {
			this.fieldIdx = fieldIdx;
		}
		
		public SingleFieldComparator(int fieldIdx, Comparator<String> fieldComparator) {
			this.fieldIdx = fieldIdx;
			this.fieldComparator = fieldComparator;
		}

		@Override
		public int compare(String linel, String liner) {
			String fieldl = CommonUtils.getFieldFromLine(linel, fieldIdx), fieldr = CommonUtils
					.getFieldFromLine(liner, fieldIdx);
			
			if (fieldComparator != null)
				return fieldComparator.compare(fieldl, fieldr);
			return fieldl.compareTo(fieldr);
		}

		int fieldIdx = 0;
		Comparator<String> fieldComparator = null;
	}
	
	public static class MultiStringFieldComparator implements Comparator<String> {
		public MultiStringFieldComparator(int[] fieldIdxes) {
			this.fieldIdxes = fieldIdxes;
		}

		@Override
		public int compare(String linel, String liner) {
			for (int fieldIdx : fieldIdxes) {
				String fieldl = CommonUtils.getFieldFromLine(linel, fieldIdx), fieldr = CommonUtils
						.getFieldFromLine(liner, fieldIdx);
				int cmp = fieldl.compareTo(fieldr);
				if (cmp != 0)
					return cmp;
			}
			return 0;
		}
		
		int[] fieldIdxes = null;
	}
	
	public static class SwapTransformer implements StringTransformer {
		public SwapTransformer(int idx0, int idx1) {
			this.idx0 = idx0;
			this.idx1 = idx1;
		}

		@Override
		public String transform(String str) {
			String[] vals = str.split("\t");
			String tmp = vals[idx0];
			vals[idx0] = vals[idx1];
			vals[idx1] = tmp;
			
			StringBuilder stringBuilder = new StringBuilder();
			boolean isFirst = true;
			for (String val : vals) {
				if (isFirst)
					isFirst = false;
				else
					stringBuilder.append("\t");
				stringBuilder.append(val);
			}
			
			return new String(stringBuilder);
		}
		
		int idx0, idx1;
	}
	
	public static void swap(String fileName, int idx0, int idx1, String dstFileName) {
		SwapTransformer swapTransformer = new SwapTransformer(idx0, idx1);
		transformLines(fileName, swapTransformer, dstFileName);
	}

	public static void transformLines(String fileName, StringTransformer lineTransformer, String dstFileName) {
		BufferedReader reader = IOUtils.getUTF8BufReader(fileName);
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName, false);
		if (writer == null)
			return ;
		
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				String transformed = lineTransformer.transform(line);
				if (transformed != null) {
					writer.write(lineTransformer.transform(line));
					writer.write("\n");
				}
			}
			
			reader.close();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void join(String fileName0, String fileName1,
			int idxCmp0, int idxCmp1, String dstFileName, Comparator<String> fieldComparator) {
		BufferedReader reader0 = IOUtils.getUTF8BufReader(fileName0), reader1 = IOUtils
				.getUTF8BufReader(fileName1);
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName);

		String line0 = null, line1 = null;
		try {
			line1 = reader1.readLine();
			String[] vals1 = line1.split("\t");
			int cnt = 0;
			while ((line0 = reader0.readLine()) != null && line1 != null) {
				String[] vals0 = line0.split("\t");

				int cmpVal = -1;
				while (line1 != null) {
					if (fieldComparator == null) {
						cmpVal = vals0[idxCmp0].compareTo(vals1[idxCmp1]);
					} else {
						cmpVal = fieldComparator.compare(vals0[idxCmp0], vals1[idxCmp1]);
					}
					if (cmpVal <= 0)
						break;
					
					line1 = reader1.readLine();
					// ++rcnt;
					if (line1 != null)
						vals1 = line1.split("\t");
				}

				if (cmpVal == 0) {
					writeValsWithoutOneField(writer, vals0, idxCmp0);
					writer.write("\t");
					writeValsWithoutOneField(writer, vals1, idxCmp1);
					writer.write("\n");
				}

				++cnt;

				// if (cnt == 100) break;
			}

			reader0.close();
			reader1.close();
			writer.close();

			System.out.println(cnt + " lines processed.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void sortAndRemoveDuplicates(String fileName, Comparator<String> tupleLineComparator,
			String dstFileName) {
		String tmpFileName = Paths.get(ELConsts.TMP_FILE_PATH, "sort_rm_dup.txt").toString();
		sort(fileName, tmpFileName, tupleLineComparator);
		removeDuplicates(tmpFileName, dstFileName);
	}

	public static void sort(String srcFileName, String dstFileName,
			Comparator<String> tupleLineComparator) {
		BufferedReader reader = IOUtils.getUTF8BufReader(srcFileName);
		int lineCnt = 0, tmpFileCnt = 0;
		String line = null;
		LinkedList<String> lines = new LinkedList<String>();
		String tmpFileName = null;
		boolean flgExceeded = true, needTmpFileSort = false;
		try {
			while (flgExceeded) {
				flgExceeded = false;

				System.out.println("Reading " + srcFileName);
				int curLineCnt = 0, charCnt = 0;
				while (!flgExceeded && (line = reader.readLine()) != null) {
					++curLineCnt;
					++lineCnt;
					charCnt += line.length();

					lines.add(line);
					if (curLineCnt % 1000000 == 0)
						System.out.println(curLineCnt + "\t" + charCnt);

					flgExceeded = charCnt > NUM_CHARS_LIM
							|| curLineCnt > NUM_LINE_LIM;
				}

				System.out.println("Sorting " + tmpFileCnt);
				Collections.sort(lines, tupleLineComparator);
				System.out.println("Done.");

				if (flgExceeded || tmpFileCnt != 0) {
					needTmpFileSort = true;
					tmpFileName = Paths.get(ELConsts.TMP_FILE_PATH,
							"s" + DEC_FORMAT.format(tmpFileCnt)).toString();
					writeLinesToFile(lines, tmpFileName);

					++tmpFileCnt;
					lines.clear();
				}
			}

			reader.close();
		} catch (IOException e) {
			System.out.println(lineCnt);
			e.printStackTrace();
		}

		if (needTmpFileSort) {
			// sort temp files
			System.out.println("Sorting temp files...");
			sortTempFiles(tmpFileCnt, dstFileName, tupleLineComparator);
			System.out.println("Done.");
		} else {
			// direct sort
			System.out.println("Doing direct sort.");
			writeLinesToFile(lines, dstFileName);
			System.out.println("Done.");
		}
	}
	
	public static void removeDuplicates(String fileName, String dstFileName) {
		BufferedReader reader = IOUtils.getUTF8BufReader(fileName);
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName, false);

		try {
			String line = null, preLine = null;
			while ((line = reader.readLine()) != null) {
				if (preLine == null || !line.equals(preLine)) {
					writer.write(line + "\n");
				}

				preLine = line;
			}

			reader.close();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void merge(String[] srcFileNames,
			String dstFileName) {
		BufferedReader reader = null;
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName);

		try {
			for (String fileName : srcFileNames) {
				System.out.println("processing " + fileName);

				reader = IOUtils.getUTF8BufReader(fileName);

				String line = null;
				while ((line = reader.readLine()) != null) {
					writer.write(line);
					writer.write("\n");
				}

				reader.close();
			}

			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static boolean checkTupleFileOrder(String fileName, Comparator<String> comparator) {
		BufferedReader reader = IOUtils.getUTF8BufReader(fileName);
		
		try {
			int cnt = 0;
			String line = null, preLine = null;
			boolean flg = true;
			while (flg && (line = reader.readLine()) != null) {
				if (preLine != null && comparator.compare(preLine, line) > 0) {
					System.out.println("line " + cnt + ": Not properly ordered.");
					reader.close();
					flg = false;
					break;
				}
				preLine = line;
				
				++cnt;
			}
			
			reader.close();
			System.out.println("File properly ordered.");
			return flg;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return false;
	}

	private static void sortTempFiles(int numTmpFiles, String dstFileName,
			Comparator<String> tupleLineComparator) {
		BufferedReader[] readers = new BufferedReader[numTmpFiles];

		String tmpFileName = null;
		for (int i = 0; i < numTmpFiles; ++i) {
			tmpFileName = Paths.get(ELConsts.TMP_FILE_PATH,
					"s" + DEC_FORMAT.format(i)).toString();

			readers[i] = IOUtils.getUTF8BufReader(tmpFileName);
		}

		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName, false);

		try {
			String[] lines = new String[numTmpFiles];
			for (int i = 0; i < numTmpFiles; ++i) {
				lines[i] = readers[i].readLine();
			}

			int minPos = 0;
			while (minPos > -1) {
				minPos = getMinIdx(lines, tupleLineComparator);

				if (minPos > -1) {
					writer.write(lines[minPos] + "\n");

					lines[minPos] = readers[minPos].readLine();
				}
			}

			for (int i = 0; i < numTmpFiles; ++i) {
				readers[i].close();
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static int getMinIdx(String[] lines,
			Comparator<String> tupleLineComparator) {
		int pos = -1;
		String minLine = null;
		for (int i = 0; i < lines.length; ++i) {
			if (lines[i] != null) {
				if (pos == -1) {
					minLine = lines[i];
					pos = i;
				} else if (tupleLineComparator.compare(minLine, lines[i]) > 0) {
					pos = i;
					minLine = lines[i];
				}
			}
		}

		return pos;
	}

	private static void writeLinesToFile(LinkedList<String> lines,
			String fileName) {
		BufferedWriter writer = IOUtils.getUTF8BufWriter(fileName, false);
		try {
			for (String line : lines) {
				writer.write(line + "\n");
			}

			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void writeValsWithoutOneField(BufferedWriter writer,
			String[] vals, int idx) {
		boolean isFirst = true;
		try {
			for (int i = 0; i < vals.length; ++i) {
				if (i != idx) {
					if (isFirst) {
						isFirst = false;
					} else {
						writer.write("\t");
					}
					writer.write(vals[i]);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
