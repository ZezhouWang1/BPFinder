package one;

import java.io.File;

import static one.Constants.APK_APK;

public class DataAnalyze {
    public static void main(String[] args) {
        Constants.initConstants(args);
        StaticDynamicComparator.compare();
        System.out.println(CoverageCalculator.coverageInStaticCallGraph());
        System.out.println(CoverageCalculator.coverageInAllMethods());
    }
}
