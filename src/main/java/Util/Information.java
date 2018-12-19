package Util;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Class to easily get Information concerning input and settings
 *
 * @author Michael Mario Kubicki
 */
public class Information {

    //Additional flag
    public static boolean VERBOSE;
    //Information taken from commandline
    public File DocumentDirectory;
    public File IndexDirectory;
    public RankingModel Ranking;
    public String Query;
    //Information from own settings and user-settings
    public int ResultCount;
    public Set<String> FileTypes;

    /**
     * empty constructor for own setup
     *
     * @author Michael Mario Kubicki
     */
    public Information() {
        VERBOSE = false;
        FileTypes = new TreeSet<>();
    }

    /**
     * Take information from commandline
     *
     * @param args commandline input
     * @return Information object containing the settings
     * @author Michael Mario Kubicki
     */
    public static Information ApplyArgs(String[] args) {

        ArrayList<String> Arg = new ArrayList<>(Arrays.asList(args));
        Information information = new Information();

        //Check for help flag
        if (Arg.contains("-h")) {
            System.out.println("USAGE: ");
            System.out.println("ir_programming_task.jar [path_to_document_folder] [path_to_index_folder] [VS/OK] [\"query\"] [optional: path_to_settings.xml]");
            System.exit(0);
        }
        //Check for verbose flag
        if (Arg.contains("-v")) {
            VERBOSE = true;
            Arg.remove("-v");
        }

        //After removal of additional flags
        //commandline should only have at least 4 parts
        //5 if you use additional settings
        //Check for sanity
        if (Arg.size() < 4 || Arg.size() > 5) {
            errorUsage();
        }

        //First command = document directory
        //Check if exists, as it is primary input
        information.DocumentDirectory = getDirectory(args[0]);

        //Second command = index directory
        //Could exist or not
        //only surface check for validity and create if needed
        information.IndexDirectory = new File(args[1]);
        if (!information.IndexDirectory.exists())
            information.IndexDirectory.mkdirs();

        //Third command = Ranking Model
        //Select case
        switch (args[2].toUpperCase()) {
            case "VS":
                information.Ranking = RankingModel.VectorSpace;
                break;
            case "OK":
                information.Ranking = RankingModel.Okapi;
                break;
            default:
                System.out.println("Wrong Ranking Model selection!");
                errorUsage();
                break;
        }

        //Fourth command = Query
        information.Query = args[3];

        //Load Settings from internal storage
        //Basic number of results and used file extensions
        try {
            information.loadInternalSettings();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //If additional Settings load them
        if (args.length == 5) {
            try {
                //Fifth command = path to settings
                //Read and load
                File inputFile = new File(args[4]);
                InputStream is = new FileInputStream(inputFile);
                information.loadSettings(is);

            } catch (ParserConfigurationException | IOException | SAXException e) {
                System.out.println("Error loading additional settings");
                e.printStackTrace(System.out);
                System.exit(-1);
            }
        }

        return information;
    }

    /**
     * Display correct usage
     * if used wrongly
     *
     * @author Michael Mario Kubicki
     */
    private static void errorUsage() {
        System.out.println("Wrong usage:");
        System.out.println("ir_programming_task.jar [path_to_document_folder] [path_to_index_folder] [VS/OK] [query] [optional: path_to_settings.xml]");
        System.out.println("or ir_programming_task.jar -h for more information");
        System.exit(-1);
    }

    /**
     * Check for validity of directory
     * and return it if ok
     *
     * @param path string path to directory
     * @return Directory as File
     */
    private static File getDirectory(String path) {
        File file = new File(path);

        if (!file.isDirectory() || !file.exists()) {
            System.out.println("Error with: " + path);
            errorUsage();
        }

        return file;
    }

    private static String getText(NodeList nl) {
        if (nl.item(0) == null)
            return "";
        return nl.item(0).getTextContent();
    }

    /**
     * Load internal Settings
     *
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     * @author Michael Mario Kubicki
     */
    private void loadInternalSettings() throws ParserConfigurationException, IOException, SAXException {
        InputStream settingsIn = this.getClass().getClassLoader().getResourceAsStream("settings.xml");
        loadSettings(settingsIn);
    }

    /**
     * Load XML from inputStream
     * <p>
     * Settings = XML
     * Two elements (both optional)
     * - number_results
     * - file_types (multiple delimited by ';'
     * nested in "settings" tag
     *
     * @param inputStream
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     * @author Michael Mario Kubicki
     */
    private void loadSettings(InputStream inputStream) throws ParserConfigurationException, IOException, SAXException {

        //Read XML
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(inputStream);

        //Try Parsing number_results
        String result_count = getText(document.getElementsByTagName("number_results"));
        try {
            this.ResultCount = Integer.parseInt(result_count);
        } catch (Exception e) {
            //Ignore. just dont update
        }

        //Try Parsing file_types
        //Next time just use same tag multiple times...
        String[] types = getText(document.getElementsByTagName("file_types")).split(";");

        this.FileTypes.addAll(Arrays.stream(types)
                .filter(str -> !str.equals(""))
                .map(String::toLowerCase)
                .collect(Collectors.toList()));
    }
}