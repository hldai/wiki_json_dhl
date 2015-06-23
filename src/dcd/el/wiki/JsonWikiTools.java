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
import dcd.el.utils.TupleFileTools;

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

	public static class AnchorTextCntLineComparator implements
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

	public static void genEntityAliasCountFile(
			String articleAnchorTextFileName, String titleToWidFileName,
			String dstAnchorTextListFileName, String dstAnchorCountFileName,
			String dstNumTotalAnchorsFileName, String maxRefCntFileName) {
		String tmpAnchorTextListFileName = Paths.get(ELConsts.TMP_FILE_PATH,
				"anchor_list.txt").toString();
		genTmpAnchorTextListFile(articleAnchorTextFileName, titleToWidFileName,
				tmpAnchorTextListFileName);

		String tmpSortedAnchorTextListFileName = Paths.get(
				ELConsts.TMP_FILE_PATH, "anchor_list_sorted.txt").toString();
		TupleFileTools.sort(tmpAnchorTextListFileName,
				tmpSortedAnchorTextListFileName,
				new AnchorTextCntLineComparator());

		BufferedWriter dstAnchorListWriter = IOUtils
				.getUTF8BufWriter(dstAnchorTextListFileName, true), dstAnchorCountWriter = IOUtils
				.getUTF8BufWriter(dstAnchorCountFileName, true);
		BufferedReader reader = IOUtils
				.getUTF8BufReader(tmpSortedAnchorTextListFileName);
		try {
			int curWid = -1;
			int curAnchorCnt = 0, curWidAnchorCnt = 0, allAnchorCnt = 0;
			int maxRefCnt = 0;
			String curAnchorDesc = null;
			String line = null;
			while ((line = reader.readLine()) != null) {
				String[] vals = line.split("\t");
				int wid = Integer.valueOf(vals[0]), anchorCnt = Integer
						.valueOf(vals[2]);
				allAnchorCnt += anchorCnt;
				if (wid == curWid) {
					curWidAnchorCnt += anchorCnt;
					if (vals[1].equals(curAnchorDesc)) {
						curAnchorCnt += anchorCnt;
					} else {
						dstAnchorListWriter.write(curWid + "\t" + curAnchorDesc
								+ "\t" + curAnchorCnt + "\n");

						curAnchorDesc = vals[1];
						curAnchorCnt = anchorCnt;
					}
				} else {
					if (curWid != -1) {
						dstAnchorListWriter.write(curWid + "\t" + curAnchorDesc
								+ "\t" + curAnchorCnt + "\n");
						dstAnchorCountWriter.write(curWid + "\t"
								+ curWidAnchorCnt + "\n");
						if (curWidAnchorCnt > maxRefCnt)
							maxRefCnt = curWidAnchorCnt;
					}

					curWid = wid;
					curAnchorDesc = vals[1];
					curAnchorCnt = anchorCnt;
					curWidAnchorCnt = anchorCnt;
				}
			}
			dstAnchorListWriter.write(curWid + "\t" + curAnchorDesc
					+ "\t" + curAnchorCnt + "\n");
			dstAnchorCountWriter.write(curWid + "\t"
					+ curWidAnchorCnt + "\n");
			if (curWidAnchorCnt > maxRefCnt)
				maxRefCnt = curWidAnchorCnt;

			dstAnchorListWriter.close();
			dstAnchorCountWriter.close();
			reader.close();
			
			IOUtils.writeIntValueFile(dstNumTotalAnchorsFileName, allAnchorCnt);
			IOUtils.writeIntValueFile(maxRefCntFileName, maxRefCnt);
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
				Link preLink = null;
				int linkCnt = 0;
				for (Link link : links) {
					if (link.getId().contains("\n")
							|| link.getId().contains("\t")
							|| link.getDescription().contains("\n")
							|| link.getDescription().contains("\t")) {
						continue;
					}

					if (preLink != null && LinkEqual(preLink, link)) {
						++linkCnt;
					} else {
						if (preLink != null) {
							linkListBuilder.append(preLink.getId() + "\t"
									+ preLink.getDescription() + "\t" + linkCnt
									+ "\n");
						}
						linkCnt = 1;
					}

					preLink = link;
				}
				if (preLink != null)
					linkListBuilder.append(preLink.getId() + "\t"
							+ preLink.getDescription() + "\t" + linkCnt);

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
		System.out.println(Article.getTitleInWikistyle("m&m's"));
		// RecordReader<Article> reader = new
		// RecordReader<Article>(jsonFilePath,
		// new JsonRecordParser<Article>(Article.class));
		//
		// BufferedWriter writer = IOUtils.getUTF8BufWriter(
		// "d:/data/el/tmp_files/tmp_str.txt", false);
		//
		// try {
		// int cnt = 0;
		// for (Article a : reader) {
		// System.out.println(a.getWikiTitle());
		// writer.write(a.toString() + "\n");
		//
		// ++cnt;
		// if (cnt == 10)
		// break;
		// }
		//
		// writer.close();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
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
		Item anchorItem = null;
		BufferedWriter anchorTextListWriter = IOUtils.getUTF8BufWriter(
				dstFileName, false);
		int cnt = 0, lineCnt = 0;
		try {
			while (itemReader.readNextItem() != null) {
				itemReader.readNextItem();
				anchorItem = itemReader.readNextItem();

				String[] lines = anchorItem.value.split("\n");
				for (String line : lines) {
					String[] vals = line.split("\t");
					Integer wid = getWid(vals[0], mapper);
					if (wid != null) {
						anchorTextListWriter.write(wid + "\t" + vals[1].toLowerCase() + "\t"
								+ vals[2] + "\n");
						++lineCnt;
					}
				}

				++cnt;
//				if (cnt == 10)
//					break;
				if (cnt % 1000000 == 0)
					System.out.println(cnt);
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

	private static boolean LinkEqual(Link ll, Link lr) {
		return ll.getId().equals(lr.getId())
				&& ll.getDescription().equals(lr.getDescription());
	}
}
