package edu.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Regex {

    public static String getSubUtilSimple(String soap, String rgex) {
        Pattern pattern = Pattern.compile(rgex);// 匹配的模式
        Matcher m = pattern.matcher(soap);
        while (m.find()) {
            return m.group(1);
        }
        return "";
    }

    public static String getLastSubUtilSimple(String soap, String rgex) {
        Pattern pattern = Pattern.compile(rgex,Pattern.DOTALL);// 匹配的模式
        Matcher m = pattern.matcher(soap);
        if (m.find()) {
            return m.group(m.groupCount());
        }
        return "";
    }

    public static List<String> getSubUtilSimpleList(String soap, String rgex) {
        List<String> res = new ArrayList<>();

        Pattern pattern = Pattern.compile(rgex);// 匹配的模式
        Matcher m = pattern.matcher(soap);
        while (m.find()) {
            res.add(m.group(1));
        }
        return res;
    }

    public static Set<String> getSubUtilSimpleSet(String soap, String rgex) {
        Set<String> res = new HashSet<>();

        Pattern pattern = Pattern.compile(rgex);// 匹配的模式
        Matcher m = pattern.matcher(soap);
        while (m.find()) {
            res.add(m.group(1));
        }
        return res;
    }
}
