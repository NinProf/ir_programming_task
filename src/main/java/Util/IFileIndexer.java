package Util;

import org.apache.lucene.document.Document;

import java.io.File;
import java.io.IOException;

public interface IFileIndexer {
    Document index(File file) throws IOException;
}
