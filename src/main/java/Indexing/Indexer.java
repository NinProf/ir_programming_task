package Indexing;

import Util.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Class for Indexing Documents
 * Setup everything by using the constructor with arguments or calling setup
 * use Index(...) to index a directory
 * after calling close needs to be setup again
 *
 * @author Michael Mario Kubicki
 * @see Indexer#SetUp(File, RankingModel, IFileIndexer, CheckedList)
 * @see Indexer#Close()
 * @see Indexer#Index(File, Set)
 */
public class Indexer {

    private IndexWriter indexWriter;
    private CheckedList checkedList;

    private int indexedFileCounter;

    private boolean setup = false;

    private Map<String, IFileIndexer> fileTypeIndexer;
    private IFileIndexer backUpIndexer;

    private ArrayList<IStringListener> onIndex;
    private ArrayList<IStringListener> onFinish;

    /**
     * empty constructor
     * You still need to manually call SetUp(...)
     *
     * @author Michael Mario Kubicki
     * @see Indexer
     * @see Indexer#SetUp(File, RankingModel, IFileIndexer, CheckedList)
     */
    public Indexer() {
        this.indexedFileCounter = 0;

        onFinish = new ArrayList<>();
        onIndex = new ArrayList<>();
        fileTypeIndexer = new HashMap<>();
    }

    /**
     * normal constructor
     * automatically sets everything up so it can immediately used
     *
     * @param index_dir     Directory in which the index is/should be found
     * @param model         Scoring model to be used
     * @param backUpIndexer Indexer for generic File
     * @param checkedList   CheckedList object containing Info about all already checked files
     * @throws IOException Exception concerning Access to index directory
     * @author Michael Mario Kubicki
     * @see Indexer#SetUp(File, RankingModel, IFileIndexer, CheckedList)
     */
    public Indexer(File index_dir, RankingModel model, IFileIndexer backUpIndexer, CheckedList checkedList) throws IOException {
        this();
        this.SetUp(index_dir, model, backUpIndexer, checkedList);
    }

    /**
     * Setup Indexer to be used
     *
     * @param index_dir     Directory in which the index is/should be found
     * @param model         Scoring model to be used
     * @param backUpIndexer Indexer for generic File
     * @param checkedList   CheckedList object containing Info about all already checked files
     * @throws IOException Exception concerning Access to index directory
     * @author Michael Mario Kubicki
     * @see Indexer#Close()
     */
    public void SetUp(File index_dir, RankingModel model, IFileIndexer backUpIndexer, CheckedList checkedList) throws IOException {

        //List of already checked files
        this.checkedList = checkedList;

        //Indexer for generic File
        this.backUpIndexer = backUpIndexer;

        //Select Directory for index
        Directory indexDirectory = FSDirectory.open(index_dir.toPath());

        //Get Customized Analyzer
        Analyzer analyzer = TextAnalyzer.GetAnalyzer();

        //Setup Configuration
        IndexWriterConfig writerConfig = new IndexWriterConfig(analyzer);

        //Create or append index
        writerConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

        //Autocomit on closing writer
        writerConfig.setCommitOnClose(true);

        //Alot of information if Verbose
        if (Information.VERBOSE)
            writerConfig.setInfoStream(System.out);

        //Select used Scoring
        switch (model) {
            case VectorSpace:
                writerConfig.setSimilarity(new ClassicSimilarity());
                break;
            case Okapi:
                writerConfig.setSimilarity(new BM25Similarity());
        }

        //Create writer for Index
        indexWriter = new IndexWriter(indexDirectory, writerConfig);

        setup = true;
    }

    /**
     * Close the indexer to initiate committing of changes
     * and finish indexing
     * indexer needs to be setup again before further usage
     *
     * @throws IOException Exception concerning Access to index directory
     * @author Michael Mario Kubicki
     * @see Indexer#SetUp(File, RankingModel, IFileIndexer, CheckedList)
     */
    public void Close() throws IOException {
        indexWriter.close();
        setup = false;
    }

    /**
     * Index the documents in given directory
     * only lookout for specified file types
     * After indexing new/updating check for deletion
     * Call OnFinish listener after being done
     *
     * @param documents_dir Directory of the documents. Will be searched recursively
     * @param fileTypes     File types to index
     * @throws IOException           Exception concerning Access to documents
     * @throws IllegalStateException Indexer wasn't setup correctly
     * @author Michael Mario Kubicki
     * @see Indexer#SetUp(File, RankingModel, IFileIndexer, CheckedList)
     * @see Indexer#onFinish
     */
    public void Index(File documents_dir, Set<String> fileTypes) throws IOException, IllegalStateException {

        if (!setup) throw new IllegalStateException("Indexer was not set up");

        FileFilter filter = new IndexFileFilter(fileTypes);

        indexDir(documents_dir, filter);

        //Check for removed
        Set<String> removed = checkedList.GetDifference();
        for (String s : removed) {
            //Remove files with path s from index
            indexWriter.deleteDocuments(new Term("path", s));
            checkedList.checkedFiles.remove(s);
            CallOnIndex(s + " REMOVED");
        }

        CallOnFinish("" + indexedFileCounter);
        indexedFileCounter = 0;
    }

    /**
     * Index everything in the directory
     *
     * @param dir    directory containing files to be indexed
     * @param filter Filter for file types
     * @throws IOException Exception concerning Access to documents
     * @author Michael Mario Kubicki
     */
    private void indexDir(File dir, FileFilter filter) throws IOException {

        /*
        Take all the Files in the directory
        and for each:
        - if directory: use indexDir recursively
        - else: check if accessible and of correct type
            then index it
         */

        File[] files = dir.listFiles();

        assert files != null;

        for (File file : files) {
            if (file.isDirectory())
                indexDir(file, filter);
            else if (file.exists() && file.canRead() && filter.accept(file))
                indexFile(file);
        }
    }

    /**
     * Index the given file using the IndexWriter
     * First check using checkedList if file is already known, new or needs an update
     * Act accordingly
     *
     * @param file File to be indexed
     * @throws IOException Exception concerning file-accessing
     * @author Michael Mario Kubicki
     * @see IndexWriter
     * @see Indexer#onIndex
     */
    private void indexFile(File file) throws IOException {

        //Check if File is already on List or needs an update
        CheckedList.FileState stateFile = CheckedList.FileState.New;
        try {
            stateFile = this.checkedList.CheckFile(file);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(-1);
        }

        switch (stateFile) {
            case Known:
                //Do nothing
                break;
            case New:
                //Add File to index
                indexFileAdd(file);
                break;
            case Update:
                //Update File on index
                indexFileUpdate(file);
                break;
        }
    }

    /**
     * Add File as new Document to index
     *
     * @param file File
     * @throws IOException
     * @author Michael Mario Kubicki
     */
    private void indexFileAdd(File file) throws IOException {
        CallOnIndex(file.getPath() + " NEW");
        indexedFileCounter++;

        //Try finding document writing by extention
        String filename = file.getName().toLowerCase();
        for (String s : fileTypeIndexer.keySet()) {
            if (filename.endsWith(s)) {
                Document doc = fileTypeIndexer.get(s).index(file);
                indexWriter.addDocument(doc);
                return;
            }
        }

        //Fallback
        Document doc = backUpIndexer.index(file);
        indexWriter.addDocument(doc);
    }

    /**
     * Update File in Index
     *
     * @param file File
     * @throws IOException
     */
    private void indexFileUpdate(File file) throws IOException {
        CallOnIndex(file.getPath() + " UPDATE");
        indexedFileCounter++;

        //Try finding document writing by extention
        String filename = file.getName().toLowerCase();
        for (String s : fileTypeIndexer.keySet()) {
            if (filename.endsWith(s)) {
                Document doc = fileTypeIndexer.get(s).index(file);
                indexWriter.updateDocument(new Term("path", file.getPath()), doc);
                return;
            }
        }

        //Fallback
        Document doc = backUpIndexer.index(file);
        indexWriter.updateDocument(new Term("path", file.getPath()), doc);
    }

    /**
     * Add listener for indexing new file
     * Listener receives path of file
     *
     * @param listener To be add
     * @author Michael Mario Kubicki
     */
    public void AddOnIndex(IStringListener listener) {
        onIndex.add(listener);
    }

    /**
     * Add listener for finish of indexing call
     * Listener receives number of indexed files as string
     *
     * @param listener To be add
     * @author Michael Mario Kubicki
     */
    public void AddOnFinish(IStringListener listener) {
        onFinish.add(listener);
    }

    /**
     * Remove listener for indexing new file
     * Listener receives path of file
     *
     * @param listener To be removed
     * @author Michael Mario Kubicki
     */
    public void RemoveOnIndex(IStringListener listener) {
        onIndex.remove(listener);
    }

    /**
     * Remove listener for finish of indexing call
     * Listener receives number of indexed files as string
     *
     * @param listener To be removed
     * @author Michael Mario Kubicki
     */
    public void RemoveOnFinish(IStringListener listener) {
        onFinish.remove(listener);
    }

    /**
     * Call listener OnIndex
     *
     * @param document document currently being indexed
     * @author Michael Mario Kubicki
     */
    protected void CallOnIndex(String document) {
        onIndex.forEach(l -> l.event(document));
    }

    /**
     * Call listener OnFinish
     *
     * @param count number of documents indexed
     * @author Michael Mario Kubicki
     */
    protected void CallOnFinish(String count) {
        onFinish.forEach(l -> l.event(count));
    }

    /**
     * Add Indexer to extension
     *
     * @param extension file extension
     * @param indexer   Indexer
     * @author Michael Mario Kubicki
     */
    public void AddFileIndexer(String extension, IFileIndexer indexer) {
        this.fileTypeIndexer.put(extension.toLowerCase(), indexer);
    }
}
