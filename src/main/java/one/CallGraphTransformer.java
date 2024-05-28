package one;

import edu.src.GlobalRef;
import edu.util.ApplicationClassFilter;
import edu.util.EntryPointHelper;
import org.xmlpull.v1.XmlPullParserException;
import soot.*;
import soot.dexpler.DalvikThrowAnalysis;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.ExceptionalUnitGraphFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;

import static one.Constants.*;

public class CallGraphTransformer {

    public static void callGraph2DirectedGraph() throws Exception {

        long startTime = System.currentTimeMillis();
        System.out.println("==>START TIME:" + startTime);

        //calculate EntryPoint to generate dummyMainMethod
        EntryPointHelper entryPointHelper = calculateEntryPoint(APK_APK, androidJar);

        JimpleBasedInterproceduralCFG baseICFG = new JimpleBasedInterproceduralCFG(true, true) {
            protected DirectedGraph<Unit> makeGraph(Body body) {
                return enableExceptions ? ExceptionalUnitGraphFactory.createExceptionalUnitGraph(body, DalvikThrowAnalysis.interproc(), true)
                        : new BriefUnitGraph(body);
            }
        };
        CallGraph largestCallGraph = Scene.v().getCallGraph();

        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            if (ApplicationClassFilter.isClassInSystemPackage(sootClass.getName())) {
                continue;
            }

            for (SootMethod sootMethod: sootClass.getMethods()){
                Scene.v().setEntryPoints(Collections.singletonList(sootMethod));
                CallGraph cg = Scene.v().getCallGraph();
                if(cg.size() > largestCallGraph.size()){
                    largestCallGraph = cg;
                }
            }
        }

        saveCallGraph2DirectedGraph(largestCallGraph, CALLGRAPH_FILE);
        saveCallGraph2Dot(largestCallGraph, CALLGRAPH_DOT);
        dot2png(CALLGRAPH_DOT);

        long afterEntryPoint = System.currentTimeMillis();
        System.out.println("==>after EntryPoint TIME:" + afterEntryPoint);
    }

    private static void getMinTargetSDKVersion(String apkPath) {
        try {
            ProcessManifest manifest = new ProcessManifest(apkPath);
            GlobalRef.minSDKVersion = manifest.getMinSdkVersion();
            GlobalRef.targetSDKVersion = manifest.getTargetSdkVersion();
            System.out.println("====APK minSDKVersion====" + GlobalRef.minSDKVersion);
            System.out.println("====APK targetSDKVersion====" + GlobalRef.targetSDKVersion);
        } catch (Exception e) {
            GlobalRef.minSDKVersion = 30;
        }
    }

    private static EntryPointHelper calculateEntryPoint(String apkPath, String androidJarPath) throws XmlPullParserException, IOException {
        EntryPointHelper entryPointHelper = new EntryPointHelper();
        entryPointHelper.calculateEntryPoint(apkPath, androidJarPath);
        return entryPointHelper;
    }

    private static boolean containsUnnecessaryClasses(Edge edge){
        if(ApplicationClassFilter.isClassInSystemPackage
                (String.valueOf(edge.getSrc().method().getDeclaringClass())) ||
                ApplicationClassFilter.isClassInSystemPackage
                        (String.valueOf(edge.getTgt().method().getDeclaringClass()))){
            return true;
        }

        if(String.valueOf(edge.getSrc().method().getDeclaringClass()).contains("dummy") ||
                String.valueOf(edge.getTgt().method().getDeclaringClass()).contains("dummy")){
            return true;
        }

        return false;
    }

    private static void saveCallGraph2DirectedGraph(CallGraph callGraph, String fileName){
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            for (Edge edge : callGraph) {
                // Add the edge to the DOT file
                if(containsUnnecessaryClasses(edge)){
                    continue;
                }
                //String src = edge.getSrc().toString(), tgt = edge.getTgt().toString();

                String src = edge.getSrc().method().getDeclaringClass()+"."+edge.getSrc().method().getName();
                String tgt = edge.getTgt().method().getDeclaringClass()+"."+edge.getTgt().method().getName();
                writer.println(src + DELIMITER + tgt);
            }

            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveCallGraph2Dot(CallGraph callGraph, String fileName){
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            writer.println("digraph CallGraph {");

            for (Edge edge : callGraph) {
                // Add the edge to the DOT file
                if(containsUnnecessaryClasses(edge)){
                    continue;
                }

                String src = edge.getSrc().method().getDeclaringClass()+"."+edge.getSrc().method();
                String tgt = edge.getTgt().method().getDeclaringClass()+"."+edge.getTgt().method();
                writer.println("  \"" + src + "\" -> \"" + tgt + "\";");
            }

            writer.println("}");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void dot2png(String dotFile){
        String cmd = String.format("dot -Tpng -O %s", dotFile);
        try{
            System.out.println(cmd);
            Runtime.getRuntime().exec(cmd);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

}
