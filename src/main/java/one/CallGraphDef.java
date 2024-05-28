package one;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static one.Constants.DELIMITER;

public class CallGraphDef{
    private final Map<String, CallGraphNode> NAME_NODE_MAP = new HashMap<>();
    public static class CallGraphNode {
        boolean accessed = false;

        String name;
        List<CallGraphNode> outNeighbors = new ArrayList<>();
        List<CallGraphNode> inNeighbors = new ArrayList<>();

        CallGraphNode(String name) {
            this.name = name;
        }

        void addInNeighbor(CallGraphNode in) {
            inNeighbors.add(in);
        }

        void addOutNeighbor(CallGraphNode out) {
            outNeighbors.add(out);
        }

        public void setAccessed(boolean accessed) {
            this.accessed = accessed;
        }

        public boolean isAccessed() {
            return accessed;
        }

        @Override
        public String toString() {
            return this.name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CallGraphNode that = (CallGraphNode) o;
            return Objects.equals(this.name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.name);
        }

    }

    public int size(){
        return NAME_NODE_MAP.size();
    }

    public Map<String, CallGraphNode> name_node_map() {
        return NAME_NODE_MAP;
    }



    public boolean containsNode(String name){
        return NAME_NODE_MAP.containsKey(name);
    }
    public CallGraphNode getNode(String name){
        if(!containsNode(name)){
            NAME_NODE_MAP.put(name, new CallGraphNode(name));
        }

        return NAME_NODE_MAP.get(name);
    }

    public void loadCallGraph(String callGraphFile){
        try(BufferedReader br = new BufferedReader(new FileReader(callGraphFile))) {
            String line;
            while ((line = br.readLine())!= null){
                loadEdge(line);
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void loadEdge(String line){
        CallGraphNode src = getNode(line.split(DELIMITER)[0]);
        CallGraphNode tgt = getNode(line.split(DELIMITER)[1]);
        if(src.equals(tgt)){
            return;
        }

        if(src.outNeighbors.contains(tgt)){
            return;
        }

        if(tgt.inNeighbors.contains(src)){
            return;
        }

        src.addOutNeighbor(tgt);
        tgt.addInNeighbor(src);
    }
}
