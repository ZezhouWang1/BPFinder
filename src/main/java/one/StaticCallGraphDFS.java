package one;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import static one.CallGraphDef.CallGraphNode;
import static one.Constants.*;

public class StaticCallGraphDFS {

    private static final CallGraphDef staticCallGraph = new CallGraphDef();

    public static void runDFS() {
        new File(DFS_FILE).delete();
        staticCallGraph.loadCallGraph(CALLGRAPH_FILE);
        List<CallGraphNode> sources = getSource();
        for (CallGraphNode source: sources){
            List<CallGraphNode> sequence = new ArrayList<>();
            sequence.add(source);
            source.setAccessed(true);
            dfs(source, sequence);
        }

    }

    private static List<CallGraphNode> getSource(){
        List<CallGraphNode> sources = new ArrayList<>();
        for (CallGraphNode callGraphNode: staticCallGraph.name_node_map().values()){
            if(callGraphNode.inNeighbors.isEmpty()){
                sources.add(callGraphNode);
            }
        }
        return sources;
    }

    private static void dfs(CallGraphNode node, List<CallGraphNode> accessSequence){
        if(node.outNeighbors.isEmpty()){
            saveSequence(accessSequence);
            return;
        }

        for (CallGraphNode out: node.outNeighbors){
            if(out.isAccessed()){
                continue;
            }
            out.setAccessed(true);
            accessSequence.add(out);
            dfs(out, accessSequence);
            accessSequence.remove(out);
            out.setAccessed(false);
        }
    }

    private static void saveSequence(List<CallGraphNode> accessSequence){
        try(PrintWriter writer = new PrintWriter(new FileWriter(Constants.DFS_FILE, true))){
            List<String> accessSequenceName = accessSequence.stream()
                    .map(CallGraphNode::toString).collect(Collectors.toList());
            writer.println(String.join(DELIMITER, accessSequenceName));
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
