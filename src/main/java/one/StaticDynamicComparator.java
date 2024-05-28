package one;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static one.Constants.*;

public class StaticDynamicComparator {
    private final CallGraphDef staticCallGraph = new CallGraphDef();
    private final List<DynamicAccessSequence> das = new ArrayList<>();

    private void loadGraphs() {
        staticCallGraph.loadCallGraph(CALLGRAPH_FILE);
    }

    private void loadDynamicSequences() {
        try (BufferedReader br = new BufferedReader(new FileReader(OUTPUT_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                das.add(new DynamicAccessSequence(line));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void findMismatch() {
        if(staticCallGraph.size() == 0){
            for (DynamicAccessSequence sequence : das) {
                matchAnySequence(sequence);
            }
            return;
        }

        for (DynamicAccessSequence sequence : das) {
            match(sequence);
        }

    }

    private void matchAnySequence(DynamicAccessSequence sequence){
        sequence.accessSequence.removeIf(NOT_TARGET_PACKAGE);
        if(sequence.accessSequence.size() >= 2){
            for (int i = 0; i < sequence.accessSequence.size() - 1; i++) {
                String current = sequence.accessSequence.get(i);
                String next = sequence.accessSequence.get(i + 1);
                breakPointMessage(current, next);
            }
        }
    }

    private void match(DynamicAccessSequence sequence) {
        if (sequence.accessSequence.size() <= 1) {
            return;
        }
        for (int i = 0; i < sequence.accessSequence.size() - 1; i++) {
            String current = sequence.accessSequence.get(i);
            String next = sequence.accessSequence.get(i + 1);
            if(current.equals(next)){
                continue;
            }
            if(current.contains(TARGET_PACKAGE) && next.contains(TARGET_PACKAGE)) {
                if (!staticCallGraph.containsNode(current) || !staticCallGraph.containsNode(next)) {
                    breakPointMessage(current, next);
                }
            }

            CallGraphDef.CallGraphNode src = staticCallGraph.getNode(current);
            CallGraphDef.CallGraphNode tgt = staticCallGraph.getNode(next);

            if (!src.outNeighbors.contains(tgt)
                    && src.name.contains(TARGET_PACKAGE)
                    && tgt.name.contains(TARGET_PACKAGE)) {
                breakPointMessage(current, next);
            }

        }
    }

    public static void compare() {
        StaticDynamicComparator c = new StaticDynamicComparator();
        c.loadGraphs();
        c.loadDynamicSequences();
        c.das.removeIf(sequence -> sequence.accessSequence.isEmpty());
        c.findMismatch();
    }

    private void breakPointMessage(String src, String tgt) {
        System.out.printf("Breaking point found! %s %s %s%n", src, BREAKING_EDGE, tgt);
    }
}
