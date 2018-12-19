package Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

/**
 * Serializable object to check for Updates/New/Deletion/...
 *
 * @author Michael Mario Kubicki
 */
public class CheckedList implements java.io.Serializable {
    //Serialized part
    //Map from Path -> Hash
    public HashMap<String, String> checkedFiles;
    //Not serialized
    //all files we checked
    //difference between this and checkedFiles == deleted
    public transient TreeSet<String> controllList;

    public CheckedList() {
        checkedFiles = new HashMap<>();
        controllList = new TreeSet<>();
    }

    /**
     * Get the Difference from checkedFiles and controllList
     * basically the deleted files
     *
     * @return path of deleted Files
     * @author Michael Mario Kubicki
     */
    public Set<String> GetDifference() {
        //Try making copy of keySet from HashMap
        //to modify with removeAll with controllList
        HashMap<String, String> test = (HashMap<String, String>) checkedFiles.clone();
        Set<String> testSet = test.keySet();
        testSet.removeAll(controllList);

        return testSet;
    }

    /**
     * Check if the file is new/needs update/or nothing
     *
     * @param file File to check
     * @return State what to do
     * @throws NoSuchAlgorithmException shouldn't be thrown at all (except your java has no MD5)
     * @author Michael Mario Kubicki
     */
    public FileState CheckFile(File file) throws NoSuchAlgorithmException {

        if (controllList == null)
            controllList = new TreeSet<>();

        controllList.add(file.getPath());

        //Calculate MD5 hash of File to see change

        //See https://stackoverflow.com/a/304350
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (InputStream is = new FileInputStream(file);
             DigestInputStream dis = new DigestInputStream(is, md)) {
            //Read complete File
            while (dis.available() > 0) {
                dis.read();
            }
        } catch (IOException ex) {
            ex.printStackTrace(System.out);
            System.exit(-1);
        }

        byte[] hash_bytes = md.digest();
        StringBuilder hash = new StringBuilder();
        for (byte b : hash_bytes) {
            hash.append(Integer.toString(b, 16));
        }

        //Result in hash.toString()

        //Check if we know this file
        //Don't know -> new
        //Know, but changed hash -> update
        //Know and same hash -> known (do nothing)
        if (checkedFiles.containsKey(file.getPath())) {
            if (checkedFiles.get(file.getPath()).equals(hash.toString()))
                return FileState.Known;
            else {
                checkedFiles.replace(file.getPath(), hash.toString());
                return FileState.Update;
            }
        } else {
            checkedFiles.put(file.getPath(), hash.toString());
            return FileState.New;
        }
    }

    public enum FileState {
        New,
        Update,
        Known
    }

}

