package org.example.datastructure;

import com.ibm.wala.cast.ipa.callgraph.AstCallGraph;
import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.*;
import com.ibm.wala.cast.tree.impl.AbstractSourcePosition;
import com.ibm.wala.cast.tree.impl.LineNumberPosition;
import com.ibm.wala.cast.tree.impl.RangePosition;
import com.ibm.wala.cast.util.SourceBuffer;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.shrike.shrikeBT.Instruction;
import com.ibm.wala.shrike.shrikeBT.InvokeInstruction;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.*;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.IntSet;
import org.example.datastructure.CatchBlockInfo;
import org.example.datastructure.TaintedSinkInfo;
import org.example.pa.WalaAnalysis;
import org.example.util.FileUtil;
import org.example.util.Print;
import polyglot.ast.Catch;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class ExtendedBlock {
    private CGNode node;
    private List<ISSABasicBlock> blocks;
    private int numberOfBasicBlocks;
    private int firstLineNumber;
    private int lastLineNumber;
    private int colorNum;
    private int methodFirstLine;
    private int methodLastLine;
    private boolean isFirstLineValid = true;
    private ArrayList<String> ccb = new ArrayList<>();
    // private ArrayList<String> methodSrcCode;

    public ExtendedBlock(CGNode node, List<ISSABasicBlock> blocks, int numberOfBasicBlocks, int firstLineNumber,
            int lastLineNumber, int colorNum, int methodFirstLine, int methodLastLine) {
        this.node = node;
        this.blocks = blocks;
        this.numberOfBasicBlocks = blocks.size();
        this.firstLineNumber = firstLineNumber;
        this.lastLineNumber = lastLineNumber;
        this.colorNum = colorNum;
        this.methodFirstLine = methodFirstLine;
        this.methodLastLine = methodLastLine;
    }

    public void setCCB(ArrayList<String> confCheckBlock) {
        this.ccb = confCheckBlock;
    }

    public ExtendedBlock(CGNode node, List<ISSABasicBlock> blocks, int colorNum) {
        this.node = node;
        this.blocks = blocks;
        this.colorNum = colorNum;

    }

    // public void setMethodSrcCode() {

    // }

    public void setMethodFirstLine(int i) {
        this.methodFirstLine = i;
    }

    public void setMethodLastLine(int i) {
        this.methodLastLine = i;
    }

    public int getMethodFirstLine() {
        return this.methodFirstLine;
    }

    public int getMethodLastLine() {
        return this.methodLastLine;
    }

    public int getColorNum() {
        return this.colorNum;
    }

    // Getters 和 Setters
    public CGNode getNode() {
        return node;
    }

    public void setNode(CGNode node) {
        this.node = node;
    }

    public List<ISSABasicBlock> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<ISSABasicBlock> blocks) {
        this.blocks = blocks;
        this.numberOfBasicBlocks = blocks.size();
    }

    public void addBlock(ISSABasicBlock bb) {
        if (this.blocks != null) {
            this.blocks.add(bb);
        } else {
            this.blocks = new ArrayList<ISSABasicBlock>();
            this.blocks.add(bb);
        }
        this.numberOfBasicBlocks = this.blocks.size();
    }

    public int getNumberOfBasicBlocks() {
        return numberOfBasicBlocks;
    }

    public void setNumberOfBasicBlocks(int numberOfBasicBlocks) {
        this.numberOfBasicBlocks = numberOfBasicBlocks;
    }

    public int getFirstLineNumber() {
        return firstLineNumber;
    }

    public void setFirstLineNumber(int firstLineNumber) {
        this.firstLineNumber = firstLineNumber;
    }

    public int getLastLineNumber() {
        return lastLineNumber;
    }

    public void setLastLineNumber(int lastLineNumber) {
        this.lastLineNumber = lastLineNumber;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ExtendedBlock{" +
                "node=" + node +
                ", blocks=" + blocks +
                ", numberOfBasicBlocks=" + numberOfBasicBlocks +
                ", firstLineNumber=" + firstLineNumber +
                ", lastLineNumber=" + lastLineNumber +
                ", firstMehotdNumber=" + methodFirstLine +
                ", lastMethodNumber=" + methodLastLine +
                ", colorNum=" + colorNum +
                ", isFirstLineValid=" + isFirstLineValid +
                '}');
        builder.append("\nExtended-confCheckCodeBlocks:\n");
        for (String l : this.ccb) {
            builder.append(l + "\n");
        }
        return builder.toString();

    }

    public int hashCode() {
        return node.hashCode() + colorNum;
    }

    public void setFirstLineInvalid() {
        this.isFirstLineValid = false;
    }

    public boolean getisFirstLineValid() {
        return this.isFirstLineValid;
    }
}