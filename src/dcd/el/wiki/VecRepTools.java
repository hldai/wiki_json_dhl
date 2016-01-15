// author: DHL brnpoem@gmail.com

package dcd.el.wiki;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import dcd.el.io.IOUtils;
import dcd.word2vec.WordVectors;
import it.cnr.isti.hpc.io.reader.JsonRecordParser;
import it.cnr.isti.hpc.io.reader.RecordReader;
import it.cnr.isti.hpc.wikipedia.article.Article;
import it.cnr.isti.hpc.wikipedia.article.Link;

// vector representation
public class VecRepTools {
	public static void genEntityCategoryRep(String jsonFilePath, 
			String wordVectorFileName, String dstFilePath) {
//		WordVectors wordVectors = new WordVectors(wordVectorFileName);
//		
//		float[] vec = wordVectors.getVector("not");
//		for (float v : vec) {
//			System.out.print(v + " ");
//		}
//		System.out.println();
		RecordReader<Article> reader = new RecordReader<Article>(jsonFilePath,
				new JsonRecordParser<Article>(Article.class));
		
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFilePath, false);
		
		try {
			int cnt = 0;
			for (Article a : reader) {
				System.out.println(a.getTitle());
				System.out.println(a.getInfobox().getName());
//				writer.write(a.getWid() + "\n");
//				List<Link> categories = a.getCategories();
//				
//				writer.write(categories.size() + "\n");
//				for (Link category : categories) {
//					writer.write(category.getDescription() + "\n");
//				}
				
				++cnt;
				if (cnt == 10)
					break;
			}
			
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
