package Util;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;

import java.io.IOException;

/**
 * Storage for custom Analyzer
 */
public class TextAnalyzer {
    /**
     * Uses Lucene StandardTokenizer
     * Lowercase
     * Standard stopwords
     * and Porter stemming
     *
     * @return Analyzer build using above mentioned Tokenizer and Filter
     * @throws IOException "for some reason..."
     * @author Michael Mario Kubicki
     */
    public static Analyzer GetAnalyzer() throws IOException {
        return CustomAnalyzer.builder()
                .withTokenizer(StandardTokenizerFactory.class)
                .addTokenFilter(LowerCaseFilterFactory.class)
                .addTokenFilter(StopFilterFactory.class)
                .addTokenFilter(PorterStemFilterFactory.class)
                .build();
    }
}
