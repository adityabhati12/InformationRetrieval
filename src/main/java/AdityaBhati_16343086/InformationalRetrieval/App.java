package AdityaBhati_16343086.InformationalRetrieval;

import java.util.Map;

import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

public class App 
{
    public static void main( String[] args ) throws Exception
    {
    	String user_home = System.getProperty("user.dir");
    	IndexFiles indexer = new IndexFiles(user_home+"/indexfiles/", user_home+"/crandocs/cran.all.1400", user_home+"/crandocs/cran.qry", user_home+"/crandocs/cranqrel", user_home+"/splitedfiles/", new StandardAnalyzer());
    	indexer.cranFileParser();
    	indexer.indexFiles();
		Map<Integer, String> qmap = indexer.queryParser();
		System.out.println(qmap.size());
		indexer.relParser();
		indexer.scorer(qmap);
    }
}

