package AdityaBhati_16343086.InformationalRetrieval;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.AfterEffectB;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.BasicModelIn;
import org.apache.lucene.search.similarities.DFRSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.MultiSimilarity;
import org.apache.lucene.search.similarities.NormalizationH1;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class IndexFiles {
	String indPath;
	String crandocpath;
	String cranDocQueryPath;
	String cranDocRelPath;
	String splitPath;
	Analyzer analyzer;
	Directory index;
	IndexReader indexReader;
	IndexSearcher indexSearcher;
	
	public IndexFiles(String indPath, String crandocpath, String cranDocQueryPath, String cranDocRelPath,
			String splitPath, Analyzer analyzer)
	{
		super();
		this.indPath = indPath;
		this.crandocpath = crandocpath;
		this.cranDocQueryPath = cranDocQueryPath;
		this.cranDocRelPath = cranDocRelPath;
		this.splitPath = splitPath;
		this.analyzer = analyzer;
	}
	public void cranFileParser() {

		BufferedReader bufferedReader = null;
		String file_line;
		boolean abstractStart = false;
		String fileOutput = "";
		Integer counter = 1;

		try {
			bufferedReader = new BufferedReader(new FileReader(new File(crandocpath)));
			bufferedReader.readLine();

			while ((file_line = bufferedReader.readLine()) != null) {
				
				if (file_line.startsWith(".W")) {
					abstractStart = true;
				} else if (file_line.startsWith(".I") && abstractStart) {

					writeOutput(splitPath + counter.toString() + ".txt", fileOutput);
					counter++;
					fileOutput = "";
					abstractStart = false;
				} else if (abstractStart) {
					fileOutput = fileOutput + file_line + System.getProperty("line.separator");
				}
			}
			writeOutput(splitPath + counter.toString() + ".txt", fileOutput);
			bufferedReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (bufferedReader != null)
				try {
					bufferedReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}

	}

	public Map<Integer, String> queryParser() throws Exception {

		BufferedReader buffer = new BufferedReader(new FileReader(new File(cranDocQueryPath)));
		String line = buffer.readLine();
		StringBuilder sb = new StringBuilder();
		Map<Integer, String> map = new HashMap<>();
		try {

			while (line != null) {
				int queryId = 0;
				String queryText = "";
				if (line.startsWith(".I")) {
					sb.setLength(0);
					queryId = Integer.parseInt(line.substring(3));
					line = buffer.readLine();
				}
				if (line.startsWith(".W")) {
					line = buffer.readLine();
					while (!line.startsWith(".I")) {
						sb.append(line);
						line = buffer.readLine();
						if (line == null) {
							queryText = sb.toString();
							break;
						}
					}
					queryText = sb.toString();
					sb.setLength(0);
				}
				map.put(queryId, queryText);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			buffer.close();

		}
		return map;
	}

	
	
	public void writeOutput(String outputFileName, String output) throws IOException {
		final File outputDirectory = new File(outputFileName);

		if (!outputDirectory.exists()) {
			outputDirectory.createNewFile();
		}

		Writer fileWriter = new FileWriter(outputDirectory);
		BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
		bufferedWriter.write(stemmingAnalyzePreprocessing(output));
		bufferedWriter.close();
		fileWriter.close();
	}

	public String stemmingAnalyzePreprocessing(String text) throws IOException {
		String term;
		String tempString = "";
		TokenStream result = analyzer.tokenStream(null, text);
		result = new PorterStemFilter(result);
		result = new StopFilter(result, StopAnalyzer.ENGLISH_STOP_WORDS_SET);
		CharTermAttribute resultAttr = result.addAttribute(CharTermAttribute.class);
		result.reset();
		while (result.incrementToken()) {
			term = resultAttr.toString();
			term = term + ' ';
			tempString = tempString + term;
		}
		result.end();
		result.close();
		return tempString;
	}
	
	public void indexFiles() {
		boolean create = true;

		final Path docDir = Paths.get(crandocpath);
		if (!Files.isReadable(docDir)) {
			System.out.println("Document directory '" + docDir.toAbsolutePath()
					+ "' does not exist or is not readable, please check the path");
			System.exit(1);
		}
		
		Date start = new Date();
		try {
			System.out.println("Indexing to directory '" + indPath + "'...");

			index = FSDirectory.open(Paths.get(indPath));
			IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

			if (create) {
				iwc.setOpenMode(OpenMode.CREATE);
			} else {
				// Add new documents to an existing index:
				iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
			}

			IndexWriter writer = new IndexWriter(index, iwc);
			indexDocs(writer, docDir);

			writer.close();

			Date end = new Date();
			System.out.println(end.getTime() - start.getTime() + " total milliseconds");

		} catch (IOException e) {
			System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
		}
	}
	
	
	static void indexDoc(IndexWriter writer, Path file, long lastrevise) throws IOException {

		Scanner sc = new Scanner(file);

		sc.useDelimiter(".I");

		InputStream stream = Files.newInputStream(file);

    try {

    	while(sc.hasNext()) {

    		

    		String contents = sc.next();

    		String[] result = contents.split(".A|.T|.B|.W");

    		

        Document doc = new Document();

        doc.add(new StringField("DocNumber",result[0].trim(),Field.Store.YES));

        doc.add(new TextField("title", result[1],Field.Store.YES));

        doc.add(new TextField("Author", result[2], Field.Store.YES));

        doc.add(new TextField("bib", result[3], Field.Store.YES));

        doc.add(new TextField("contents", result[4], Field.Store.YES));



        if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {

            System.out.println("adding " + file);

            writer.addDocument(doc);

        } else {

            System.out.println("updating " + file);

            writer.updateDocument(new Term("path", file.toString()), doc);

        }

    	}

    }catch (Exception e) {

		// TODO: handle exception

	}finally {

		stream.close();

	}

}
	
	static void indexDocs(final IndexWriter writer, Path track) throws IOException {

        if (Files.isDirectory(track)) {

            Files.walkFileTree(track, new SimpleFileVisitor<Path>() {

                @Override

                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                    try {

                        indexDoc(writer, file, attrs.lastModifiedTime().toMillis());

                    } catch (IOException ignore) {

                    }

                    return FileVisitResult.CONTINUE;

                }

            });

        } else {

            indexDoc(writer, track, Files.getLastModifiedTime(track).toMillis());

        }

    }
	
public void scorer(Map<Integer, String> map) throws NumberFormatException, IOException, ParseException {
		
		int q_counter =1;
		
		indexReader = DirectoryReader.open(index);

		indexSearcher = new IndexSearcher(indexReader);
		
		BufferedWriter writer = new BufferedWriter(new FileWriter("crandocs/results.txt"));
		
		for (Map.Entry<Integer, String> entry : map.entrySet())
		{
			
			// MultiSimilarity Array

    		Similarity similarity[] = { new BM25Similarity(2, (float) 0.89),

    				new DFRSimilarity(new BasicModelIn(), new AfterEffectB(), new NormalizationH1()),

    				new LMDirichletSimilarity(1500) };

    		// Set the similarty metrics to searcher

    		indexSearcher.setSimilarity(new MultiSimilarity(similarity));
			
			
			TopScoreDocCollector topScoreDocCollector = TopScoreDocCollector.create(1000);

			Query q = new QueryParser("contents", analyzer).parse(entry.getValue());

			indexSearcher.search(q, topScoreDocCollector);

			ScoreDoc[] hits = topScoreDocCollector.topDocs().scoreDocs;
			
			int hitCounter=1;

			for (ScoreDoc hit : hits) {

				int docId = hit.doc;

				Document doc = indexSearcher.doc(docId);

				writer.write(q_counter + " 0 " + doc.get("DocNumber") + " " + hitCounter + " " + hit.score + " exp_0" + "\n");

				hitCounter++;

			}			q_counter++;
		}
		
		writer.close();
		
}
	
	public void relParser() {

		try (BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(cranDocRelPath)));
				BufferedWriter writer = new BufferedWriter(new FileWriter("crandocs/c_cran.txt"));) {

			String line;

			String arr[];

			while ((line = bufferedReader.readLine()) != null) {

				arr = line.split(" ");

				writer.write(Integer.parseInt(arr[0]) + " 0 " + Integer.parseInt(arr[1]) + " "
						+ Integer.parseInt(arr[2]) + "\n");

			}

		} catch (FileNotFoundException e) {

			e.printStackTrace();

		} catch (IOException e) {

			e.printStackTrace();

		}

	}
	

}



