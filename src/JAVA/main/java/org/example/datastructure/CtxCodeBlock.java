package org.example.datastructure;

import java.util.ArrayList;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.model.java.lang.reflect.Array;
import com.ibm.wala.ssa.ISSABasicBlock;

import fj.data.List;
import soot.dexpler.instructions.ArrayLengthInstruction;

public class CtxCodeBlock {
    private CGNode node;
    private ArrayList<ISSABasicBlock> blocks;
    private int numberOfBasicBlocks;
    private int firstLineNumber;
    private int lastLineNumber;
    private int colorNum;
    private int methodFirstLine;
    private int methodLastLine;
    private ArrayList<String> methodSrcCode;
    private boolean isfirstLineValid = true;

    public CtxCodeBlock(CGNode node, ArrayList<ISSABasicBlock> blocks, int numberOfBasicBlocks, int firstLineNumber,
            int lastLineNumber, int colorNum, int methodFirstLine, int methodLastLine) {
        this.node = node;
        this.blocks = blocks;
        this.firstLineNumber = firstLineNumber;
        this.lastLineNumber = lastLineNumber;
        this.methodFirstLine = methodFirstLine;
        this.methodLastLine = methodLastLine;
    }

    public CtxCodeBlock(CGNode node, ArrayList<ISSABasicBlock> blocks) {
        this.node = node;
        this.blocks = blocks;
    }

    public CtxCodeBlock(CGNode node) {
        this.node = node;
    }

    public void setMethodSrcCode(ArrayList<String> mrs) {
        this.methodSrcCode = mrs;
    }

    public ArrayList<String> getMethodSrcCode() {
        return this.methodSrcCode;
    }

    public void setNewBlocks(ISSABasicBlock bb) {
        if (this.blocks == null) {
            ArrayList<ISSABasicBlock> newList = new ArrayList<>();
            newList.add(bb);
            this.blocks = newList;
        } else {
            this.blocks.add(bb);
        }
        this.numberOfBasicBlocks = this.blocks.size();
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

    public ArrayList<ISSABasicBlock> getBlocks() {
        return blocks;
    }

    public void setBlocks(ArrayList<ISSABasicBlock> blocks) {
        this.blocks = blocks;
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

    // toString 方法
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CtxCodeBlock{")
                .append("node=").append(node)
                .append(", blocks=").append(blocks)
                .append(", numberOfBasicBlocks=").append(numberOfBasicBlocks)
                .append(", firstLineNumber=").append(firstLineNumber)
                .append(", lastLineNumber=").append(lastLineNumber)
                .append(", firstMethodNumber=").append(methodFirstLine)
                .append(", lastMethodNumber=").append(methodLastLine)
                .append(", isFirstLineValid=").append(isfirstLineValid)
                .append(", methodSrcCode=\n");

        if (methodSrcCode != null) {
            for (String cd : methodSrcCode) {
                builder.append(cd).append("\n");
            }
        }

        builder.append("}");
        return builder.toString();

    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + lastLineNumber;
        result = 31 * result + firstLineNumber;

        for (String exceptionType : methodSrcCode) {
            result = 31 * result + (exceptionType != null ? exceptionType.hashCode() : 0);
        }
        return result;

    }

    @Override
    public boolean equals(Object o) {
        CtxCodeBlock another = (CtxCodeBlock) o;
        if (another.getNode().equals(this.node))
            return true;
        return false;
    }

    public void setFirstInvalid() {
        this.isfirstLineValid = false;
    }

    public boolean isfirstLineValid() {
        return this.isfirstLineValid;
    }
}
