package dcd.el.wiki;

import it.cnr.isti.hpc.io.reader.JsonRecordParser;
import it.cnr.isti.hpc.io.reader.RecordReader;
import it.cnr.isti.hpc.wikipedia.article.Article;
import it.cnr.isti.hpc.wikipedia.article.Link;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import dcd.el.ELConsts;
import dcd.el.io.IOUtils;
import dcd.el.io.Item;
import dcd.el.io.ItemReader;
import dcd.el.io.ItemWriter;
import dcd.el.utils.StringToIntMapper;
import dcd.el.utils.StringTransformer;
import dcd.el.utils.TupleFileTools;
import dcd.el.utils.TupleFileTools.SwapTransformer;

public class JsonWikiTools {
	public static class LinkComparator implements Comparator<Link> {

		@Override
		public int compare(Link ll, Link lr) {
			int idCmp = ll.getId().compareTo(lr.getId());
			if (idCmp != 0)
				return idCmp;

			return ll.getDescription().compareTo(lr.getDescription());
		}
	}

	public static class AliasWidCntLineComparator implements
			Comparator<String> {
		@Override
		public int compare(String linel, String liner) {
			String[] valsl = linel.split("\t"), valsr = liner.split("\t");
			int widl = Integer.valueOf(valsl[0]), widr = Integer
					.valueOf(valsr[0]);
			if (widl != widr)
				return widl - widr;

			return valsl[1].compareTo(valsr[1]);
		}
	}
	
	private static class TitleToWidTransformer implements StringTransformer {
		public TitleToWidTransformer(int idx, StringToIntMapper titleToWid) {
			this.idx = idx;
			this.titleToWid = titleToWid;
		}
		
		@Override
		public String transform(String str) {
			String[] vals = str.split("\t");
			Integer wid = titleToWid.getValue(vals[idx]);
			if (wid == null)
				return null;
			
			vals[idx] = String.valueOf(wid);
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
		
		int idx;
		StringToIntMapper titleToWid = null;
	}
	
	public static void genDictWikiBasis(String anchorTextListFileName, String disambAliasWidFileName, 
			String titleWidWithRedirectFileName,
			String dstFileName) {
		String tmpFileName0 = Paths.get(ELConsts.TMP_FILE_PATH, "disamb_wid_alias.txt").toString(),
				tmpFileName1 = Paths.get(ELConsts.TMP_FILE_PATH, "wid_title_with_redirect.txt").toString();
		System.out.println("swaping " + disambAliasWidFileName);
		TupleFileTools.swap(disambAliasWidFileName, 0, 1, tmpFileName0);
		System.out.println("swaping " + titleWidWithRedirectFileName);
		TupleFileTools.swap(titleWidWithRedirectFileName, 0, 1, tmpFileName1);
		System.out.println("done");

		System.out.println("merging...");
		String tmpFileName2 = Paths.get(ELConsts.TMP_FILE_PATH, "merged_wiki_basis.txt").toString();
		BufferedWriter writer = IOUtils.getUTF8BufWriter(tmpFileName2, false);
		try {
			BufferedReader reader = null;
			String[] fileNames = { tmpFileName0, tmpFileName1 };
			String line = null;
			for (String fileName : fileNames) {
				reader = IOUtils.getUTF8BufReader(fileName);
				
				while ((line = reader.readLine()) != null) {
					writer.write(line + "\t1\n");
				}
				
				reader.close();
			}
			
			reader = IOUtils.getUTF8BufReader(anchorTextListFileName);
			while ((line = reader.readLine()) != null) {
				writer.write(line + "\n");
			}
			reader.close();
			
			
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("done");
		
		genMergedWidAliasCntFile(tmpFileName2, dstFileName);
	}
	
	public static void rtitleToWidInFile(String fileName, int idx, String titleToWidFileName, String dstFileName) {
		System.out.println("reading title to wid file.");
		StringToIntMapper titleToWid = new StringToIntMapper(titleToWidFileName);
		System.out.println("done.");
		TitleToWidTransformer transformer = new TitleToWidTransformer(idx, titleToWid);
		TupleFileTools.transformLines(fileName, transformer, dstFileName);
	}

	public static void genEntityAliasCountFiles(
			String articleAnchorTextFileName, String titleToWidFileName,
			String dstAnchorTextListFileName, String dstAnchorCountFileName,
			String dstNumTotalAnchorsFileName, String maxRefCntFileName) {
		String tmpAnchorTextListFileName = Paths.get(ELConsts.TMP_FILE_PATH,
				"anchor_list.txt").toString();
		genTmpAnchorTextListFile(articleAnchorTextFileName, titleToWidFileName,
				tmpAnchorTextListFileName);

		genMergedWidAliasCntFile(tmpAnchorTextListFileName, dstAnchorTextListFileName);

		BufferedWriter anchorCountWriter = IOUtils.getUTF8BufWriter(dstAnchorCountFileName, false);
		BufferedReader reader = IOUtils.getUTF8BufReader(dstAnchorTextListFileName);
		try {
			String line = null;
			int curWid = -1;
			int curAnchorCnt = 0, allAnchorCnt = 0, maxRefCnt = 0;
			while ((line = reader.readLine()) != null) {
				String[] vals = line.split("\t");
				int wid = Integer.valueOf(vals[0]), anchorCnt = Integer
						.valueOf(vals[2]);
				allAnchorCnt += anchorCnt;
				if (wid == curWid) {
					curAnchorCnt += anchorCnt;
				} else {
					if (curWid != -1) {
						anchorCountWriter.write(curWid + "\t"
								+ curAnchorCnt + "\n");
						if (curAnchorCnt > maxRefCnt)
							maxRefCnt = curAnchorCnt;
					}
					
					curWid = wid;
					curAnchorCnt = anchorCnt;
				}
			}
			anchorCountWriter.write(curWid + "\t"
					+ curAnchorCnt + "\n");
			if (curAnchorCnt > maxRefCnt)
				maxRefCnt = curAnchorCnt;
			
			anchorCountWriter.close();
			reader.close();
			
			IOUtils.writeIntValueFile(dstNumTotalAnchorsFileName, allAnchorCnt);
			IOUtils.writeIntValueFile(maxRefCntFileName, maxRefCnt);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void genMergedWidAliasCntFile(String fileName, String dstFileName) {
		String tmpFileName = Paths.get(ELConsts.TMP_FILE_PATH, "sorted_wid_alias_cnt.txt").toString();
		TupleFileTools.sort(fileName, tmpFileName,
				new AliasWidCntLineComparator());
		BufferedReader reader = IOUtils.getUTF8BufReader(tmpFileName);
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName, false);
		try {
			String line = null;
			String curAlias = null;
			int curWid = -1, curWidAliasCnt = 0;
			while ((line = reader.readLine()) != null) {
				String[] vals = line.split("\t");
				int wid = Integer.valueOf(vals[0]), widAliasCnt = Integer
						.valueOf(vals[2]);
				if (wid == curWid && vals[1].equals(curAlias)) {
					curWidAliasCnt += widAliasCnt;
				} else {
					if (curWid != -1)
						writer.write(curWid + "\t" + curAlias + "\t" + curWidAliasCnt + "\n");
					
					curWid = wid;
					curAlias = vals[1];
					curWidAliasCnt = widAliasCnt;
				}
			}
			writer.write(curWid + "\t" + curAlias + "\t" + curWidAliasCnt + "\n");
			
			reader.close();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void genArticleAnchorText(String jsonFilePath,
			String dstFilePath) {
		RecordReader<Article> reader = new RecordReader<Article>(jsonFilePath,
				new JsonRecordParser<Article>(Article.class));
		ItemWriter itemWriter = new ItemWriter(dstFilePath);

		Item idItem = new Item();
		idItem.key = ELConsts.WIKI_ID_ITEM_KEY;
		Item titleItem = new Item();
		titleItem.key = ELConsts.WIKI_TITLE_ITEM_KEY;
		Item anchorItem = new Item();
		anchorItem.key = ELConsts.WIKI_ANCHOR_ITEM_KEY;
		LinkComparator linkCmp = new LinkComparator();
		int cnt = 0;
		for (Article article : reader) {
			if (article.getWikiTitle().contains("\n")) {
				System.err.println("title has \\n");
			} else {
				idItem.value = String.valueOf(article.getWid());
				titleItem.value = article.getTitle();

				StringBuilder linkListBuilder = new StringBuilder();
				List<Link> links = article.getLinks();
				Collections.sort(links, linkCmp);
				String preLinkId = null, preLinkDescription = null;
				String linkId = null, linkDescription = null;
				int linkCnt = 0;
				for (Link link : links) {
					linkId = link.getId().trim();
					linkDescription = link.getDescription().trim();
					if (linkId.length() == 0 || linkId.contains("\n")
							|| linkId.contains("\t")
							|| linkDescription.length() == 0
							|| linkDescription.contains("\n")
							|| linkDescription.contains("\t")) {
						continue;
					}

					if (preLinkId != null && preLinkId.equals(linkId) && preLinkDescription.equals(linkDescription)) {
						++linkCnt;
					} else {
						if (preLinkId != null) {
							linkListBuilder.append(preLinkId + "\t"
									+ preLinkDescription + "\t" + linkCnt
									+ "\n");
						}
						linkCnt = 1;
					}

					preLinkId = linkId;
					preLinkDescription = linkDescription;
				}
				if (preLinkId != null)
					linkListBuilder.append(preLinkId + "\t"
							+ linkDescription + "\t" + linkCnt);

				anchorItem.value = new String(linkListBuilder);

				itemWriter.writeItem(idItem);
				itemWriter.writeItem(titleItem);
				itemWriter.writeItem(anchorItem);
			}

			++cnt;
			// if (cnt == 5) break;
			if (cnt % 1000000 == 0) {
				System.out.println(cnt);
			}
		}

		itemWriter.close();
	}

	public static void genRedirectList(String jsonFilePath, String dstFilePath) {
		RecordReader<Article> reader = new RecordReader<Article>(jsonFilePath,
				new JsonRecordParser<Article>(Article.class));

		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFilePath, true);

		try {
			int cnt = 0;
			for (Article a : reader) {
				if (a.getWikiTitle().contains("\n")
						|| a.getRedirect().contains("\n")
						|| a.getRedirect().equals("")) {
					System.out.println(a.getWikiTitle() + "$");
					System.out.println(a.getRedirect() + "$");
				} else {
					writer.write(a.getWikiTitle() + "\t" + a.getRedirect()
							+ "\n");
				}
				++cnt;

				// if (cnt == 10) break;
			}

			writer.close();

			System.out.println(cnt + " redirect pages processed.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void test(String jsonFilePath) {
		RecordReader<Article> reader = new RecordReader<Article>(jsonFilePath,
				new JsonRecordParser<Article>(Article.class));
		System.out.println(jsonFilePath);
		int cnt = 0;
		for (Article article : reader) {
			List<Link> categories = article.getCategories();
			for (Link category : categories) {
				System.out.println(category.getId());
			}
		}
	}

	public static void separateArticles(String jsonFilePath,
			String mainFilePath, String redirectFilePath, String disambFilePath) {
		RecordReader<Article> reader = new RecordReader<Article>(jsonFilePath,
				new JsonRecordParser<Article>(Article.class));

		BufferedWriter mainWriter = IOUtils.getUTF8BufWriter(mainFilePath), redWriter = IOUtils
				.getUTF8BufWriter(redirectFilePath), disambWriter = IOUtils
				.getUTF8BufWriter(disambFilePath);

		try {
			int cnt = 0;
			for (Article a : reader) {
				// System.out.println(a.getWikiTitle());
				if (a.isRedirect()) {
					// if (a.getLinks().isEmpty()) {
					// System.out.println("Redirect has no link.");
					// }
					redWriter.write(a.toJson() + "\n");
				} else if (a.isDisambiguation()) {
					disambWriter.write(a.toJson() + "\n");
				} else {
					mainWriter.write(a.toJson() + "\n");
				}

				++cnt;
				// if (cnt == 100)
				// break;
				if (cnt % 1000000 == 0)
					System.out.println(cnt);
			}

			mainWriter.close();
			redWriter.close();
			disambWriter.close();

			System.out.println(cnt + " articles processed.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void genRawTitleToWidList(String jsonFilePath,
			String dstFilePath) {
		RecordReader<Article> reader = new RecordReader<Article>(jsonFilePath,
				new JsonRecordParser<Article>(Article.class));
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFilePath);

		int cnt = 0;
		try {
			for (Article a : reader) {
				if (!a.isRedirect() && !a.isDisambiguation()) {
					writer.write(a.getWikiTitle() + "\t" + a.getWid() + "\n");
				}

				++cnt;

				if (cnt % 1000000 == 0) {
					System.out.println(cnt);
				}
			}

			writer.close();
			System.out.println(cnt + " articles.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void genDisambiguationList() {
		final String disambSuffix = " (disambiguation)";

		RecordReader<Article> reader = new RecordReader<Article>(
				"d:/data/el/wiki/wikipedia-dump.json.gz",
				new JsonRecordParser<Article>(Article.class));

		BufferedWriter rawTitleToWidWriter = IOUtils.getUTF8BufWriter(
				"d:/data/el/wiki/raw_title_to_wid.txt", true);
		BufferedWriter disambListWriter = IOUtils.getUTF8BufWriter(
				"d:/data/el/wiki/disamb_list.txt", true);

		int cnt = 0, disambCnt = 0;
		try {
			for (Article a : reader) {
				int wid = a.getWid(), wikiId = a.getWikiId();
				if (wid != wikiId) {
					System.out.println("wid and wikiId not equal. " + wid + " "
							+ wikiId);
				}

				// System.out.println(a.getWikiTitle());
				String wikiTitle = a.getWikiTitle();
				rawTitleToWidWriter.write(wikiTitle + "\t" + wid + "\n");

				if (a.isDisambiguation()) {
					String title = a.getTitle();
					if (title.endsWith(disambSuffix)) {
						title = title.substring(0, title.length()
								- disambSuffix.length());
					}

					List<Link> links = a.getLinks();
					for (Link l : links) {
						// System.out.println(l.getCleanId());
						disambListWriter.write(title + "\t" + l.getCleanId()
								+ "\n");
					}
					++disambCnt;
				}

				++cnt;
				// if (disambCnt == 10) break;

				if (cnt % 1000000 == 0) {
					System.out.println(cnt);
				}
			}

			rawTitleToWidWriter.close();
			disambListWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println(cnt + " pages.");
		System.out.println(disambCnt + " disambiguation pages.");
	}

	private static void genTmpAnchorTextListFile(
			String articleAnchorTextFileName, String titleToWidFileName,
			String dstFileName) {
		System.out.println("Loading title to wid file...");
		StringToIntMapper mapper = new StringToIntMapper(titleToWidFileName);
		System.out.println("Done.");
		ItemReader itemReader = new ItemReader(articleAnchorTextFileName, false);
		Item anchorItem = null, idItem = null;
		BufferedWriter anchorTextListWriter = IOUtils.getUTF8BufWriter(
				dstFileName, false);
		int cnt = 0, lineCnt = 0;
		try {
			while ((idItem = itemReader.readNextItem()) != null) {
				itemReader.readNextItem();
				anchorItem = itemReader.readNextItem();
				
				++cnt;
//				if (cnt == 10)
//					break;
				if (cnt % 1000000 == 0)
					System.out.println(cnt);

				if (anchorItem.numLines == 0)
					continue;
				
				String[] lines = anchorItem.value.split("\n");
				for (String line : lines) {
					String[] vals = line.split("\t");
					if (vals[0].length() == 0) {
						System.err.println(idItem.value + " length is 0");
						continue;
					}
					StringBuilder title = new StringBuilder();
					title.append(Character.toUpperCase(vals[0].charAt(0)));
					title.append(vals[0].substring(1, vals[0].length()));
					Integer wid = getWid(new String(title), mapper);
					if (wid != null) {
						anchorTextListWriter.write(wid + "\t" + vals[1].toLowerCase() + "\t"
								+ vals[2] + "\n");
						++lineCnt;
					}
				}
			}

			anchorTextListWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(lineCnt + " lines to " + dstFileName);
		// IOUtils.writeNumLinesFileFor(dstFileName, lineCnt);
	}

	private static Integer getWid(String title,
			StringToIntMapper titleToEidMapper) {
		if (title.length() < 1)
			return null;
		
		char firstChar = title.charAt(0);
		if (Character.isLowerCase(firstChar)) {
			StringBuilder sb = new StringBuilder();
			sb.append(Character.toUpperCase(firstChar));
			sb.append(title.substring(1, title.length()));
			return titleToEidMapper.getValue(new String(sb));
		}
		return titleToEidMapper.getValue(title);
	}
}
