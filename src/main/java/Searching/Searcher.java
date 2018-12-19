package Searching;

import Util.RankingModel;
import Util.TextAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;

/**
 * Class for querying constructed index
 * Setup everything by using the constructor with arguments or calling setup
 * use Search(...) to index a directory
 * after calling close needs to be setup again
 *
 * @author Michael Mario Kubicki
 * @see Searcher#SetUp(File, RankingModel, String, String...)
 * @see Searcher#Close()
 * @see Searcher#Search(String, int)
 */
public class Searcher {

    private IndexReader indexReader;
    private IndexSearcher indexSearcher;
    private MultiFieldQueryParser queryParser;
    private boolean setup = false;


    /**
     * Empty constructor
     * need to manually call SetUp
     *
     * @author Michael Mario Kubicki
     * @see Searcher#SetUp(File, RankingModel, String, String...)
     */
    public Searcher() {

    }

    /**
     * Construct and setup
     *
     * @param index_dir    Directory of index
     * @param model        Ranking Model
     * @param field        Field to search/compare
     * @param other_fields Additional fields
     * @throws IOException Exception concerning accessing index files
     * @author Michael Mario Kubicki
     */
    public Searcher(File index_dir, RankingModel model, String field, String... other_fields) throws IOException {
        this();
        this.SetUp(index_dir, model, field, other_fields);
    }

    /**
     * Setup searcher to search query
     *
     * @param index_dir    Directory of index
     * @param model        Ranking Model
     * @param field        Field to search/compare
     * @param other_fields Additional fields
     * @throws IOException Exception concerning accessing index files
     * @author Michael Mario Kubicki
     * @see Searcher#Close()
     */
    public void SetUp(File index_dir, RankingModel model, String field, String... other_fields) throws IOException {

        //Open index in reader
        Directory index = FSDirectory.open(index_dir.toPath());
        indexReader = DirectoryReader.open(index);

        //Create searcher
        indexSearcher = new IndexSearcher(indexReader);

        switch (model) {
            case VectorSpace:
                indexSearcher.setSimilarity(new ClassicSimilarity());
                break;
            case Okapi:
                indexSearcher.setSimilarity(new BM25Similarity());
                break;
        }

        //Construct Query parser
        //Using Multiple fields
        String[] fieldsConcat = new String[1 + other_fields.length];
        fieldsConcat[0] = field;
        for (int i = 1; i < fieldsConcat.length; ++i)
            fieldsConcat[i] = other_fields[i - 1];

        queryParser = new MultiFieldQueryParser(
                fieldsConcat,
                TextAnalyzer.GetAnalyzer()
        );

        setup = true;
    }

    /**
     * Close searcher to release index
     *
     * @throws IOException
     * @author Michael Mario Kubicki
     * @see Searcher#SetUp(File, RankingModel, String, String...)
     */
    public void Close() throws IOException {
        indexReader.close();
    }

    /**
     * Search for given Query in indexed Documents
     *
     * @param query        Query to be searched
     * @param result_count Number of results expected
     * @return scoreDocs
     * @throws IllegalStateException Searcher wasn't setup correctly
     * @throws ParseException        Exception while parsing
     * @throws IOException           Exception while accessing index
     * @author Michael Mario Kubicki
     */
    public ScoreDoc[] Search(String query, int result_count) throws IllegalStateException, ParseException, IOException {
        if (!setup) throw new IllegalStateException("Indexer was not set up");

        TopDocs td = indexSearcher.search(queryParser.parse(query), result_count);

        return td.scoreDocs;
    }

    public Document GetDoc(int doc) throws IOException {
        return indexSearcher.doc(doc);
    }
}
