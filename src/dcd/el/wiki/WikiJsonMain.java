package dcd.el.wiki;

import dcd.config.IniFile;
import dcd.config.IniFile.Section;

public class WikiJsonMain {

	public static void run() {
		IniFile config = new IniFile();
		config.Load("d:/data/el/config/json_wiki.ini");
		String job = config.getValue("main", "job");

		System.out.println("Job: " + job);

		if (job.equals("test")) {
			Section sect = config.getSection("test");
			String jsonFilePath = sect.getValue("json_file");
			JsonWikiTools.test(jsonFilePath);
		} else if (job.equals("separate_articles")) {
			Section sect = config.getSection("separate_articles");
			String jsonFilePath = sect.getValue("json_file"), mainFilePath = sect
					.getValue("main_file"), disambFilePath = sect
					.getValue("disambiguation_file"), redirectFilePath = sect
					.getValue("redirect_file");
			JsonWikiTools.separateArticles(jsonFilePath, mainFilePath,
					redirectFilePath, disambFilePath);
		} else if (job.equals("gen_redirect_list")) {
			Section sect = config.getSection("gen_redirect_list");
			String jsonFilePath = sect.getValue("json_file"), dstFilePath = sect
					.getValue("dst_file");
			JsonWikiTools.genRedirectList(jsonFilePath, dstFilePath);
		} else if (job.equals("gen_rtitle_wid_list")) {
			Section sect = config.getSection("gen_rtitle_wid_list");
			String jsonFilePath = sect.getValue("json_file"), dstFilePath = sect
					.getValue("dst_file");
			JsonWikiTools.genRawTitleToWidList(jsonFilePath, dstFilePath);
		} else if (job.equals("gen_article_anchor_list")) {
			Section sect = config.getSection("gen_article_anchor_list");
			String jsonFilePath = sect.getValue("json_file"), dstFilePath = sect
					.getValue("dst_file");
			JsonWikiTools.genArticleAnchorText(jsonFilePath, dstFilePath);
		} else if (job.equals("gen_entity_alias_cnt_files")) {
			Section sect = config.getSection("gen_entity_alias_cnt_files");
			String articleAnchorTextFileName = sect
					.getValue("article_anchor_file"), titleToWidFileName = sect
					.getValue("title_wid_file"), dstAnchorTextListFileName = sect
					.getValue("anchor_text_list_file"), dstAnchorCountFileName = sect
					.getValue("anchor_cnt_file"), dstNumTotalAnchorsFileName = sect
					.getValue("num_anchors_file"), maxRefCntFileName = sect.getValue("max_ref_cnt_file");
			JsonWikiTools.genEntityAliasCountFiles(articleAnchorTextFileName,
					titleToWidFileName, dstAnchorTextListFileName,
					dstAnchorCountFileName, dstNumTotalAnchorsFileName, maxRefCntFileName);
		} else if (job.equals("rtitle_to_wid")) {
			Section sect = config.getSection(job);
			String fileName = sect.getValue("file"), dstFileName = sect.getValue("dst_file"),
					titleToWidFileName = sect.getValue("title_wid_file");
			int idx = sect.getIntValue("idx");
			JsonWikiTools.rtitleToWidInFile(fileName, idx, titleToWidFileName, dstFileName);
		} else if (job.equals("gen_dict_wiki_basis")) {
			Section sect = config.getSection(job);
			String anchorTextListFileName = sect.getValue("anchor_text_list_file"),
					disambAliasWidFileName = sect.getValue("disamb_alias_wid_file"),
					titleWidWithRedirectFileName = sect.getValue("redirect_title_wid_file"),
					dstFileName = sect.getValue("dst_file");
			
			JsonWikiTools.genDictWikiBasis(anchorTextListFileName,
					disambAliasWidFileName, titleWidWithRedirectFileName, dstFileName);
		} else if (job.equals("gen_article_categories")) {
			Section sect = config.getSection(job);
			String jsonFilePath = sect.getValue("json_file"), wordVectorFileName = sect.getValue("word_vec_file"),
					dstFilePath = sect.getValue("dst_file");
			VecRepTools.genEntityCategoryRep(jsonFilePath, wordVectorFileName, dstFilePath);
		}
	}

	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();

		run();

		long endTime = System.currentTimeMillis();
		System.out.println((endTime - startTime) / 1000.0 + " seconds used.");
	}
}
