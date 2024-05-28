package one;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static one.Constants.*;

public class CoverageCalculator {
    public static double coverageInStaticCallGraph(){
        Set<String> dynamicResult = loadDynamicExecuteResult();
        CallGraphDef staticCG = loadStaticCallGraph();
        return dynamicResult.size() * 1.0 / staticCG.size();
    }

    public static double coverageInAllMethods(){
        Set<String> dynamicResult = loadDynamicExecuteResult();
        Set<String> allMethods = loadAllMethods();
        return dynamicResult.size() * 1.0 / allMethods.size();
    }



    private static CallGraphDef loadStaticCallGraph(){
        CallGraphDef cg = new CallGraphDef();
        cg.loadCallGraph(CALLGRAPH_FILE);
        cg.name_node_map().keySet().removeIf(NOT_TARGET_PACKAGE);
        return cg;

    }

    private static Set<String> loadDynamicExecuteResult(){
        Set<String> result = new HashSet<>();
        try(BufferedReader br = new BufferedReader(new FileReader(OUTPUT_FILE))){
            String line;
            while ((line = br.readLine())!= null){
                result.addAll(Arrays.asList(line.split(DELIMITER)));
            }

            result.removeIf(NOT_TARGET_PACKAGE);
        }catch (IOException e){
            e.printStackTrace();
        }

        return result;
    }

    private static Set<String> loadAllMethods(){
        Set<String> result = new HashSet<>();
        try(BufferedReader br = new BufferedReader(new FileReader(METHOD_NAME_FILE))){
            String line;
            while ((line = br.readLine()) != null){
                result.add(line);
            }
        }catch (IOException e){
            e.printStackTrace();
        }

        result.removeIf(NOT_TARGET_PACKAGE);

        return result;
    }
}
