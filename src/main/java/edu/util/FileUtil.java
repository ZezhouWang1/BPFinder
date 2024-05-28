package edu.util;

import com.google.common.collect.Lists;
import edu.model.sourcefile.ResourceLeakAPIStmt;
import edu.model.sourcefile.ResourceLeakRule;
import edu.model.sourcefile.TypeStateAPIStmt;
import edu.model.sourcefile.TypeStateRule;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileUtil {

    public static List<String> getAPIsFromFile(String absoluteFilePath) {
        List<String> res = new ArrayList<>();
        File file = new File(absoluteFilePath);
        if (!file.exists()) {
            return res;
        }

        List<String> fileContent = null;
        try {
            fileContent = new ArrayList<>(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (String fileLine : fileContent) {
            res.addAll(Regex.getSubUtilSimpleList(fileLine, "(<.*?>)"));
        }

        return res;
    }

    public static List<ResourceLeakRule> getResourceLeakRulesFromSourceFile(String absoluteFilePath){
        List<ResourceLeakRule> res = new ArrayList<>();
        File file = new File(absoluteFilePath);
        if (!file.exists()) {
            return res;
        }

        List<String> fileContent = null;
        try {
            fileContent = new ArrayList<>(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (String fileLine : fileContent) {
            ResourceLeakRule resourceLeakRule = new ResourceLeakRule();
            List<String> lineList = Lists.newArrayList(fileLine.split(";"));
            resourceLeakRule.beforeAPI = new ResourceLeakAPIStmt(lineList.get(0));
            resourceLeakRule.afterAPI = new ResourceLeakAPIStmt(lineList.get(1));
            res.add(resourceLeakRule);
        }
        return res;
    }

    public static List<TypeStateRule> getRulesFromSourceFile(String absoluteFilePath) {
        List<TypeStateRule> res = new ArrayList<>();
        File file = new File(absoluteFilePath);
        if (!file.exists()) {
            return res;
        }

        List<String> fileContent = null;
        try {
            fileContent = new ArrayList<>(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (String fileLine : fileContent) {
            TypeStateRule typeStateRule = new TypeStateRule();
            List<String> lineList = Lists.newArrayList(fileLine.split(";"));
            if (lineList.contains("IllegalStateException")) {
                typeStateRule.isCorrect = false;
                lineList.remove("IllegalStateException");
            }

            boolean ifContainsSDKVersion = false;
            for (String sdkStr : lineList) {
                if (sdkStr.contains("SDKVersion")) {
                    String minSDKVersion = Regex.getSubUtilSimple(sdkStr, "(SDKVersion>=.*?,)").replace("SDKVersion>=", "").replace(",", "");
                    typeStateRule.minSdkVersion = minSDKVersion;
                    String maxSDKVersion = Regex.getSubUtilSimple(sdkStr, "(SDKVersion<=.*?\\])").replace("SDKVersion<=", "").replace("]", "");
                    typeStateRule.maxSdkVersion = maxSDKVersion;
                    ifContainsSDKVersion = true;
                }
            }
            if (ifContainsSDKVersion) {
                lineList.remove(0);
            }

            if (lineList.size() == 1) {
                typeStateRule.beforeAPIs.add(new TypeStateAPIStmt(lineList.get(0)));
            } else {
                typeStateRule.afterAPI = new TypeStateAPIStmt(lineList.get(lineList.size() - 1));
                for (int i = 0; i <= lineList.size() - 2 && lineList.size() - 2 >= 0; i++) {
                    String api = lineList.get(i);
                    if (api.contains("|")) {
                        List<String> beforeAPIList = Arrays.asList(api.split("\\|"));
                        for (String beforeAPI : beforeAPIList) {
                            typeStateRule.beforeAPIs.add(new TypeStateAPIStmt(beforeAPI));
                        }
                    } else {
                        typeStateRule.beforeAPIs.add(new TypeStateAPIStmt(api));
                    }
                }
            }
            res.add(typeStateRule);
        }
        return res;
    }

    public static List<String> getAllAPIListFromSourceFile(String absoluteFilePath) {
        List<String> res = new ArrayList<>();
        File file = new File(absoluteFilePath);
        if (!file.exists()) {
            return res;
        }

        List<String> fileContent = null;
        try {
            fileContent = new ArrayList<>(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fileContent;
    }

}
