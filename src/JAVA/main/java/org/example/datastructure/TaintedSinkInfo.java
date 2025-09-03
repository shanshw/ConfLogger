package org.example.datastructure;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAInstruction;

import java.util.HashSet;
import java.util.List;

public class TaintedSinkInfo {
    private String fileName = "";

    private String srcPath = "";

    private CGNode node = null;

    private String MethodLines = ""; // format FirstLine:LastLine

    private String blockLines = ""; // format blockFirstLine:-1 -1 means don't know

    private SSAInstruction inst = null;

    private String methodDescriptor = "";

    private HashSet<String> paraSet = new HashSet<>();

    private Statement s;

    private Statement targetedSrc;

    private List<Statement> taintedPath;

    public TaintedSinkInfo() {
    }

    public TaintedSinkInfo(String fn, CGNode n, String ml, String bl, SSAInstruction ins, String mds) {
        this.fileName = fn;
        this.node = n;
        this.MethodLines = ml;
        this.blockLines = bl;
        this.inst = ins;
        this.methodDescriptor = mds;
    }

    public TaintedSinkInfo(String fn, CGNode n, String ml, String bl, SSAInstruction ins, String mds, String srcPat) {
        this.fileName = fn;
        this.node = n;
        this.MethodLines = ml;
        this.blockLines = bl;
        this.inst = ins;
        this.methodDescriptor = mds;
        this.srcPath = srcPat;
    }

    public void setTaintedPath(List<Statement> path) {
        this.taintedPath = path;
    }

    public List<Statement> getTaintedPath() {
        return this.taintedPath;
    }

    public Statement getSrc() {
        return this.targetedSrc;
    }

    public void setSrc(Statement src) {
        this.targetedSrc = src;
    }

    public Statement getStats() {
        return this.s;
    }

    public void setStats(Statement stat) {
        this.s = stat;
    }

    public HashSet<String> getParaSet() {
        return paraSet;
    }

    public boolean isParaSetEmpty() {
        return paraSet.isEmpty();
    }

    public boolean setParaSet(String newPara) {
        if (paraSet.contains(newPara))
            return false;
        else {
            paraSet.add(newPara);
            return true;
        }
    }

    public boolean inParaSet(String para) {
        return paraSet.contains(para);
    }

    public void setSrcPath(String srcPath) {
        this.srcPath = srcPath;
    }

    public void setBlockLines(String blockLines) {
        this.blockLines = blockLines;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("-------------TaintedSinkInfo----------\n");
        // builder.append("ins:\t").append(this.inst).append("\n");
        builder.append("fileName:\t").append(this.fileName).append("\tmethodSinagture:\t").append(this.methodDescriptor)
                .append("\tmethodLines:\t").append(this.MethodLines).append("\n");
        builder.append("blockLines:\t").append(this.blockLines).append("\n");
        builder.append("paras:\t");
        for (String para : this.paraSet) {
            builder.append(para + "\n");
        }
        builder.append("TaintedStat:\t").append(this.s).append("\n");
        builder.append("Source:\t").append(this.targetedSrc).append("\n");
        builder.append("Tainted Path:\t");
        for (Statement s : this.taintedPath) {
            builder.append(s).append("\n");
        }

        return builder.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        else if (obj instanceof TaintedSinkInfo) {
            TaintedSinkInfo anotherTs = (TaintedSinkInfo) obj;
            if (this.fileName.equals(anotherTs.getFileName()) && this.blockLines.equals(anotherTs.getBlockLines())
                    && this.MethodLines.equals(anotherTs.getMethodLines()) // this.node.equals(anotherTs.getNode()) &&
                    && this.srcPath.equals(srcPath) && this.methodDescriptor.equals(anotherTs.getMethodDescriptor())) // &&
                                                                                                                      // this.inst.equals(anotherTs.getInst())
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int stringHash = fileName.hashCode() * blockLines.hashCode() * MethodLines.hashCode() * srcPath.hashCode()
                * methodDescriptor.hashCode();
        return stringHash;
    }

    public String getFileName() {
        return fileName;
    }

    public String getBlockLines() {
        return blockLines;
    }

    public CGNode getNode() {
        return node;
    }

    public String getMethodLines() {
        return MethodLines;
    }

    public String getSrcPath() {
        return srcPath;
    }

    public SSAInstruction getInst() {
        return inst;
    }

    public String getMethodDescriptor() {
        return methodDescriptor;
    }
}
