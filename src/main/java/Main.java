import Indexing.Indexer;
import Searching.Searcher;
import Util.*;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.jsoup.Jsoup;

import java.io.*;

public class Main {

    /**
     * Startingpoint for Commandline program
     *
     * @param args Commandline args
     * @author Michael Mario Kubicki
     */
    public static void main(String[] args) {

        //Read commandline
        Information information = Information.ApplyArgs(args);

        //Show user settings
        System.out.println("== Information ==");
        System.out.println("Document-directory: " + information.DocumentDirectory);
        System.out.println("Index-directory: " + information.IndexDirectory);
        System.out.println("Ranking Model: " + information.Ranking);
        System.out.println("Query: " + information.Query);
        System.out.println("Number of Results Shown: " + information.ResultCount);
        System.out.println("Indexed File types: " + information.FileTypes + "\n");

        //Load known Files
        CheckedList checkedList = new CheckedList();
        File checkedListFile = new File(information.IndexDirectory + "/checked.dat");
        if (checkedListFile.exists() && checkedListFile.isFile() && checkedListFile.canRead()) {
            try {
                InputStream in = new FileInputStream(checkedListFile);
                ObjectInputStream objIN = new ObjectInputStream(in);
                checkedList = (CheckedList) objIN.readObject();
                objIN.close();
                in.close();

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        //Index
        try {
            Indexer indexer = new Indexer(
                    information.IndexDirectory,
                    information.Ranking,
                    new PlainIndexer(),
                    checkedList
            );

            //A bit more info whats going on
            indexer.AddOnIndex(new IndexedFile());
            indexer.AddOnFinish(new FinishedIndexing());

            IFileIndexer html = new HTMLIndexer();
            indexer.AddFileIndexer(".htm", html);
            indexer.AddFileIndexer(".html", html);

            indexer.Index(information.DocumentDirectory, information.FileTypes);

            indexer.Close();
        } catch (IOException e) {
            System.out.println("Cannot index ...");
            System.exit(-1);
        }

        //Search
        Searcher searcher = new Searcher();
        ScoreDoc[] result = new ScoreDoc[0];
        try {
            searcher = new Searcher(information.IndexDirectory, information.Ranking, "content", "title");

            result = searcher.Search(information.Query, information.ResultCount);

        } catch (IOException e) {
            System.out.println("Cannot read some files ...");
            e.printStackTrace(System.out);
        } catch (ParseException e) {
            System.out.println("Error while parsing ...");
            e.printStackTrace(System.out);
        }


        try {
            System.out.println("\n=== RESULTS ===\n");

            if (result.length == 0) {
                System.out.println("No relevant found");
            } else {
                for (int i = 1; i <= result.length; ++i) {
                    ScoreDoc scDocument = result[i - 1];
                    Document document = searcher.GetDoc(scDocument.doc);
                    File file = new File(document.getField("path").stringValue());


                    System.out.println("Rank " + i);
                    System.out.println("Score: " + scDocument.score);
                    System.out.println("File: " + file.getName());
                    if (file.getName().toLowerCase().endsWith(".html") ||
                            file.getName().toLowerCase().endsWith(".htm"))
                        System.out.println("Title: " + document.getField("title").stringValue());
                    System.out.println("Path: " + file.getPath() + "\n");
                }
            }


            searcher.Close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace(System.out);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Write checked List

        try {
            FileOutputStream out = new FileOutputStream(checkedListFile);
            ObjectOutputStream objOUT = new ObjectOutputStream(out);
            objOUT.writeObject(checkedList);
            objOUT.close();
            out.close();
        } catch (IOException ex) {
            ex.printStackTrace(System.out);
            System.exit(-1);
        }
    }
}

/**
 * EventListener for current File indexed - show on console
 *
 * @author Michael Mario Kubicki
 */
class IndexedFile implements IStringListener {
    @Override
    public void event(String input) {
        System.out.println("Indexed: " + input);
    }
}

/**
 * EventListener for finished indexing / count of indexed docs - show on console
 *
 * @author Michael Mario Kubicki
 */
class FinishedIndexing implements IStringListener {
    @Override
    public void event(String input) {
        System.out.println("Indexed " + input + " files");
    }
}

class HTMLIndexer implements IFileIndexer {

    @Override
    public Document index(File file) throws IOException {
        Document doc = new Document();

        InputStream is = new FileInputStream(file);
        org.jsoup.nodes.Document website = Jsoup.parse(is, null, file.getParent());
        doc.add(new TextField("content", website.body().text(), Field.Store.NO));
        doc.add(new TextField("title", website.title(), Field.Store.YES));
        doc.add(new StringField("path", file.getPath(), Field.Store.YES));

        return doc;
    }
}

class PlainIndexer implements IFileIndexer {

    @Override
    public Document index(File file) throws IOException {
        Document doc = new Document();

        FileReader reader = new FileReader(file);
        doc.add(new TextField("content", reader));
        doc.add(new TextField("title", file.getName(), Field.Store.YES));
        doc.add(new StringField("path", file.getPath(), Field.Store.YES));

        return doc;
    }
}