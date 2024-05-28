package one;

import java.nio.file.Paths;

public class Main4CGConstruction {
    public static void main(String[] args) throws Exception {
        Constants.initConstants(args);
        MethodInstrument.instrument();
        CallGraphTransformer.callGraph2DirectedGraph();
        //StaticCallGraphDFS.runDFS();
    }
}
