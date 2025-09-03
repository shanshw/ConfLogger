package org.example.pa;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.util.graph.dominators.Dominators;

import java.util.*;

public class TaintedBlockMerger<T> {

    private Map<T, Integer> blockIdMap;
    private int[] parent;
    private int[] rank;

    public TaintedBlockMerger() {
        this.blockIdMap = new HashMap<>();
    }

    public Set<Set<T>> mergeBlocks(Set<T> taintedSinks, IR ir, ControlFlowGraph cfg) {

        initUnionFind(taintedSinks);

        for (T sinkA : taintedSinks) {
            NormalStatement statement = (NormalStatement) sinkA;
            ISSABasicBlock blockA = statement.getNode().getIR().getBasicBlockForInstruction(statement.getInstruction()); // cfg.getBlockForInstruction
            // System.out.println(statement.getNode().getIR());
            if (blockA == null) {
                continue;
            }

            List<ISSABasicBlock> successors = getSuccessors(blockA, cfg);

            for (ISSABasicBlock succ : successors) {
                // if (succ.isEntryBlock() || succ.isExitBlock())
                // continue;
                T sinkB = getTaintedSinkFromBlock(succ, taintedSinks);

                if (sinkB != null && !sinkA.equals(sinkB)) {

                    union(sinkA, sinkB);
                }
            }
        }

        return buildResult(taintedSinks);
    }

    private void initUnionFind(Set<T> taintedSinks) {
        int index = 0;
        for (T sink : taintedSinks) {
            blockIdMap.put(sink, index++);
        }
        parent = new int[index];
        rank = new int[index];
        for (int i = 0; i < index; i++) {
            parent[i] = i;
            rank[i] = 1;
        }
    }

    private T getTaintedSinkFromBlock(ISSABasicBlock block, Set<T> taintedSinks) {
        for (T sink : taintedSinks) {

            NormalStatement statement = (NormalStatement) sink;
            ISSABasicBlock blockB = statement.getNode().getIR().getBasicBlockForInstruction(statement.getInstruction());
            if (block.equals(blockB)) {
                return sink;
            }
        }
        return null;
    }

    private List<ISSABasicBlock> getSuccessors(ISSABasicBlock block, ControlFlowGraph cfg) {
        List<ISSABasicBlock> successors = new ArrayList<>();
        for (Iterator<? extends ISSABasicBlock> it = cfg.getSuccNodes(block); it.hasNext();) {
            successors.add(it.next());
        }
        return successors;
    }

    private int find(int i) {
        if (parent[i] != i) {
            parent[i] = find(parent[i]); // Path compression
        }
        return parent[i];
    }

    private void union(T a, T b) {
        int rootA = find(blockIdMap.get(a));
        int rootB = find(blockIdMap.get(b));

        if (rootA != rootB) {
            // Union by rank
            if (rank[rootA] > rank[rootB]) {
                parent[rootB] = rootA;
            } else if (rank[rootA] < rank[rootB]) {
                parent[rootA] = rootB;
            } else {
                parent[rootB] = rootA;
                rank[rootA]++;
            }
        }
    }

    private Set<Set<T>> buildResult(Set<T> taintedSinks) {
        Map<Integer, Set<T>> resultMap = new HashMap<>();
        for (T sink : taintedSinks) {
            int root = find(blockIdMap.get(sink));
            resultMap.computeIfAbsent(root, k -> new HashSet<>()).add(sink);
        }
        return new HashSet<>(resultMap.values());
    }
}
