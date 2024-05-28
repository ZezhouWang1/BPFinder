package one;

import java.io.File;
import java.nio.file.Paths;
import java.util.function.Predicate;

public class Constants {
    public static void initConstants(String[] args) {
        TAG = args[0];
        APK_APK = args[1];
        ADB = args[2];
        APKSIGNER = args[3];
        KEYSTORE = args[4];
        KEYSTORE_PASSWORD = args[5];
        androidJar = args[6];
        AAPT = args[7];
        KA = args[8];
        OUTPUT_DIR = args[9];

        initOutputDir();

        TARGET_PACKAGE = MethodInstrument.getTargetPackageName();
        APK_DONE = new File(APK_APK).getParent()+"//done//"+TAG+"//"+new File(APK_APK).getName();
        APK_SOOT_OUTPUT = Paths.get("").toAbsolutePath().toString()+"\\sootOutput"+"\\"+new File(APK_APK).getName();
        METHOD_NAME_FILE = String.format("%s/method-names/method-names-%s.txt", OUTPUT_DIR, new File(APK_SOOT_OUTPUT).getName());
        CALLGRAPH_FILE = String.format("%s/callgraph-txt/callgraph-%s.txt", OUTPUT_DIR, new File(APK_SOOT_OUTPUT).getName());
        CALLGRAPH_DOT = String.format("%s/callgraph-dot/callgraph-%s.dot", OUTPUT_DIR, new File(APK_SOOT_OUTPUT).getName());
        DFS_FILE = String.format("%s/dfs/dfs-%s.txt", OUTPUT_DIR, new File(APK_SOOT_OUTPUT).getName());
        OUTPUT_FILE = String.format("%s/output/output-%s.txt", OUTPUT_DIR, new File(APK_SOOT_OUTPUT).getName());
        ANDROGUARD_GML = String.format("%s/gml/%s.gml", OUTPUT_DIR, new File(APK_SOOT_OUTPUT).getName());
    }

    private static void initOutputDir(){
        //createDir(new File(APK_APK).getParent()+"//done//"+TAG);
        createDir(OUTPUT_DIR+"//method-names");
        createDir(OUTPUT_DIR+"//callgraph-txt");
        createDir(OUTPUT_DIR+"//callgraph-dot");
        createDir(OUTPUT_DIR+"//dfs");
        createDir(OUTPUT_DIR+"//gml");
        createDir(OUTPUT_DIR+"//output");
    }

    private static void createDir(String directoryPath){
        File directory = new File(directoryPath);
        if(!directory.exists()){
            directory.mkdir();
        }
    }


    public static String TAG;
    public static String APK_APK;
    public static String APK_DONE;
    public static String APK_SOOT_OUTPUT;
    public static String ADB;
    public static String APKSIGNER;
    public static String KEYSTORE;
    public static String KEYSTORE_PASSWORD;
    public static String androidJar;
    public static String AAPT;
    public static String KA = "KA";
    public static String TARGET_PACKAGE;
    public static final String DELIMITER = "--->>>";
    public static final String BREAKING_EDGE = "--/-->";
    public static String OUTPUT_DIR;
    public static String METHOD_NAME_FILE;
    public static String CALLGRAPH_FILE;
    public static String CALLGRAPH_DOT;
    public static String DFS_FILE;
    public static String OUTPUT_FILE;
    public static String ANDROGUARD_GML;
    public static final Predicate<String> NOT_TARGET_PACKAGE =
            e -> !e.contains(TARGET_PACKAGE) && !e.contains("edu.wayne")
                    && !e.contains("de.ecspride") && !e.contains("com.hjq.permissions");
}
