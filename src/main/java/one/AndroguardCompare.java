package one;

import edu.util.EntryPointHelper;
import org.xmlpull.v1.XmlPullParserException;
import soot.*;
import soot.dexpler.DalvikThrowAnalysis;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.ExceptionalUnitGraphFactory;

import java.io.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static one.Constants.*;

public class AndroguardCompare {
    private static int androguardNodeSize;
    private static int androguardEdgeSize;
    private static int sootCGNodeSize;
    private static int sootCGEdgeSize;

    public static void main(String[] args) throws XmlPullParserException, IOException {
        generateAndroguardGraph();
        loadAndroguardGraph();
        loadCallGraph();
        System.out.println("androguardNodeSize = " + androguardNodeSize);
        System.out.println("androguardEdgeSize = " + androguardEdgeSize);
        System.out.println("sootCGNodeSize = " + sootCGNodeSize);
        System.out.println("sootCGEdgeSize = " + sootCGEdgeSize);
    }

    private static void generateAndroguardGraph() {
        try {
            String cmd = String.format("androguard cg --output %s --output-type %s --no-isolated %s",
                    ANDROGUARD_GML, "gml", APK_APK);
            Runtime.getRuntime().exec(cmd);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadAndroguardGraph() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("python3", "gml_reader.py", ANDROGUARD_GML, TARGET_PACKAGE);
            Process process = processBuilder.start();

            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Nodes Size: ")) {
                    androguardNodeSize = Integer.parseInt(line.substring("Nodes Size: ".length()));
                } else if (line.contains("Edges Size: ")) {
                    androguardEdgeSize = Integer.parseInt(line.substring("Edges Size: ".length()));
                } else {
                    System.out.println(line);
                }
            }
            int exitCode = process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void loadCallGraph() throws XmlPullParserException, IOException {
        EntryPointHelper entryPointHelper = calculateEntryPoint(APK_APK, androidJar);

        JimpleBasedInterproceduralCFG baseICFG = new JimpleBasedInterproceduralCFG(true, true) {
            protected DirectedGraph<Unit> makeGraph(Body body) {
                return enableExceptions ? ExceptionalUnitGraphFactory.createExceptionalUnitGraph(body, DalvikThrowAnalysis.interproc(), true)
                        : new BriefUnitGraph(body);
            }
        };
        CallGraph largestCallGraph = Scene.v().getCallGraph();

        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            for (SootMethod sootMethod : sootClass.getMethods()) {
                Scene.v().setEntryPoints(Collections.singletonList(sootMethod));
                CallGraph cg = Scene.v().getCallGraph();
                if (cg.size() > largestCallGraph.size()) {
                    largestCallGraph = cg;
                }
            }
        }
        sootCGEdgeSize = largestCallGraph.size();
        sootCGNodeSize = 0;
        Set<String> nodes = new HashSet<>();
        for(Edge e: largestCallGraph){
            nodes.add(e.src().getDeclaringClass()+"."+e.src().getName());
            nodes.add(e.tgt().getDeclaringClass()+"."+e.tgt().getName());
        }
        sootCGNodeSize = nodes.size();
    }

    private static EntryPointHelper calculateEntryPoint(String apkPath, String androidJarPath) throws XmlPullParserException, IOException {
        EntryPointHelper entryPointHelper = new EntryPointHelper();
        entryPointHelper.calculateEntryPoint(apkPath, androidJarPath);
        return entryPointHelper;
    }
}
