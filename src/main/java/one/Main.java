package one;

import java.util.Objects;
import java.util.concurrent.Callable;

import static one.Constants.*;

public class Main {
    public static void main(String[] args) throws Exception {
        Constants.initConstants(args);
        MethodInstrument.instrument();
        CallGraphTransformer.callGraph2DirectedGraph();

        /*StaticCallGraphDFS.runDFS();
        DynamicTracer.dynamicExecute();*/
    }
}
