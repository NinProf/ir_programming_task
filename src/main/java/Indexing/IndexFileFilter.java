package Indexing;

import java.io.File;
import java.io.FileFilter;
import java.util.Set;

/**
 * Only accepts files ending with given extension
 * Expects given extensions to be lowerCase
 *
 * @author Michael Mario Kubicki
 */
class IndexFileFilter implements FileFilter {

    private Set<String> fileTypes;

    IndexFileFilter(Set<String> fileTypes) {
        this.fileTypes = fileTypes;
    }

    @Override
    public boolean accept(File f) {
        String fileName = f.getName().toLowerCase();
        return fileTypes.stream().anyMatch(fileName::endsWith);
    }
}