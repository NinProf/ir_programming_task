package Util;

/**
 * Enum for simple selection of Model
 */
public enum RankingModel {
    VectorSpace("Vector Space"),
    Okapi("Okapi BM25");

    private String name;

    RankingModel(String stringVal) {
        name = stringVal;
    }

    public String toString() {
        return name;
    }
}