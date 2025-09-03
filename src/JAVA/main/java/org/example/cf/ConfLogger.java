package org.example.cf;

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

public class ConfLogger {

    public final static Map<CGNode, HashMap<Integer, List<ISSABasicBlock>>> nodeToColoredBlocks = new HashMap<>();
    public static int currentColor = 0;

    public void extractExistLogs(String projectRoot, Set<List<Statement>> taintedPaths,
            HashMap<Statement, String> srcParaBook)
            throws IOException, InvalidClassFileException {
        HashMap<TaintedSinkInfo, HashMap<Statement, HashSet<String>>> tsSrcBook = new HashMap<>();
        for (List<Statement> ls : taintedPaths) {

            String para = "";
            if (srcParaBook.containsKey(ls.get(ls.size() - 1))) {
                para = srcParaBook.get(ls.get(ls.size() - 1));
            }

            propocessTaintedPath(tsSrcBook, ls, para);
        }
        for (TaintedSinkInfo key : tsSrcBook.keySet()) {
            System.out.println(key);
            HashMap<Statement, HashSet<String>> mapinside = tsSrcBook.get(key);

            String fileName = key.getFileName();
            String packagePath = key.getNode().getMethod().getDeclaringClass().getName().toString()
                    .split("L")[1]
                    .strip().replace(".", "\\").split("\\$")[0];

            String filePath = projectRoot + "\\" + packagePath + ".java";
            filePath = filePath.replace("/", "\\");

            String methodLines = key.getMethodLines();
            int methodFirstLine = Integer.parseInt(methodLines.split(":")[0]);
            int methodLastLine = Integer.parseInt(methodLines.split(":")[1]);

            ArrayList<String> methodSrcCode = getSrcMethodCode(filePath, methodFirstLine, methodLastLine);

            int confCheckBlockLine = Integer.parseInt(key.getBlockLines().split(":")[0]);

            int startLine = confCheckBlockLine - methodFirstLine;

            HashSet<CatchBlockInfo> checkInfos = checkCatchBlock(key.getNode());
            ArrayList<String> confCehckBlock = extractCodeBlock(methodSrcCode, startLine);

            HashSet<Integer> logLinesSet = trackLoggerUsage(key.getNode());
            ArrayList<String> extractedLogs = new ArrayList<>();

            System.out.println("--------------LogsNum-----------");
            for (int logLineNum : logLinesSet) {
                System.out.println(logLineNum);
                extractedLogs.addAll(getSrcMethodCode(filePath, logLineNum, logLineNum));
            }
            //
            System.out.println("--------------extractedLogs-----------");
            for (String extracted : extractedLogs) {
                System.out.println(extracted);
            }

        }

    }

    public String extractLine(ArrayList<String> lines, int lineNumber) {

        if (lineNumber < 0 || lineNumber >= lines.size()) {
            return "";
        }
        return lines.get(lineNumber).trim();
    }

    private HashSet<Integer> trackLoggerUsage(CGNode node) throws InvalidClassFileException {
        ShrikeCTMethod method = (ShrikeCTMethod) node.getMethod();
        IClass declaringClass = method.getDeclaringClass();
        HashSet<Integer> logSet = new HashSet<>();

        // Check if the class defines a logger of type org.slf4j.Logger
        boolean hasLogger = false;
        for (IField field : declaringClass.getAllFields()) {
            System.out.println(field.getFieldTypeReference().getName());
            if (field.getFieldTypeReference().getName().toString().equals("Lorg/slf4j/Logger")) {
                hasLogger = true;
                break;
            }
        }

        if (hasLogger) {
            System.out.println("Class " + declaringClass.getName() + " defines a logger of type org.slf4j.Logger.");

            // Track the logger usage in the method
            IR ir = node.getIR();
            if (ir != null) {
                for (SSAInstruction instruction : ir.getInstructions()) {
                    if (instruction instanceof SSAInvokeInstruction) {
                        SSAInvokeInstruction invokeInstruction = (SSAInvokeInstruction) instruction;
                        if (invokeInstruction.getDeclaredTarget().getDeclaringClass().getName().toString()
                                .contains("org/slf4j/Logger")) {
                            System.out.println("Logger usage found in method: " + method.getSignature());
                            int lineNnum = method.getLineNumber(method.getBytecodeIndex(instruction.iIndex()));
                            System.out.println("Instruction: " + instruction + "\t line:" + lineNnum);
                            logSet.add(lineNnum);
                        }
                    }
                }
            }
        } else {
            System.out.println(
                    "Class " + declaringClass.getName() + " does not define a logger of type org.slf4j.Logger.");
        }
        return logSet;
    }

    public static void colorBasicBlocks(Statement statement) {
        NormalStatement s = (NormalStatement) statement;
        CGNode node = s.getNode();
        IR ir = node.getIR();
        ShrikeCTMethod method = (ShrikeCTMethod) node.getMethod();
        SSACFG cfg = ir.getControlFlowGraph();
        ISSABasicBlock currentBlock = ir.getBasicBlockForInstruction(s.getInstruction());

        List<ISSABasicBlock> blocksToColor = new ArrayList<>();
        blocksToColor.add(currentBlock);
        Iterator<ISSABasicBlock> it = cfg.getPredNodes(currentBlock);
        while (it.hasNext()) {
            blocksToColor.add(it.next());
        }
        it = cfg.getSuccNodes(currentBlock);
        while (it.hasNext()) {
            blocksToColor.add(it.next());
        }

        HashMap<Integer, List<ISSABasicBlock>> colorMap = nodeToColoredBlocks.computeIfAbsent(node,
                k -> new HashMap<>());

        Integer color = null;
        for (ISSABasicBlock block : blocksToColor) {
            for (Map.Entry<Integer, List<ISSABasicBlock>> entry : colorMap.entrySet()) {
                if (entry.getValue().contains(block)) {
                    color = entry.getKey();
                    break;
                }
            }
            if (color != null) {
                break;
            }
        }
        if (color == null) {
            color = currentColor++;
        }

        colorMap.computeIfAbsent(color, k -> new ArrayList<>()).addAll(blocksToColor);

    }

    public static void printColored() {

        System.out.println("-------------colored-situations-------------");
        for (CGNode node : nodeToColoredBlocks.keySet()) {
            System.out.println("-------------" + node.getMethod().getName() + "-------------");
            for (int color : nodeToColoredBlocks.get(node).keySet()) {
                System.out.println("color - " + color);
                HashSet<Integer> mergedBlocks = new HashSet<>();
                for (ISSABasicBlock bb : nodeToColoredBlocks.get(node).get(color)) {
                    if (mergedBlocks.contains(bb.getNumber()))
                        continue;
                    else {
                        System.out.println(bb.getNumber() + "\t");
                        mergedBlocks.add(bb.getNumber());
                    }
                }
            }
            System.out.println("======================");
        }
    }

    public static List<ISSABasicBlock> getBlocksForNodeAndColor(CGNode node, Integer color) {

        return nodeToColoredBlocks.getOrDefault(node, new HashMap<>()).getOrDefault(color, Collections.emptyList());

    }

    public static Map<Integer, List<ISSABasicBlock>> getColorMapForNode(CGNode node) {
        Map<Integer, List<ISSABasicBlock>> results = nodeToColoredBlocks.get(node);
        return results;
    }

    public static int getColredNumber(CGNode node) {
        return 0;
        // return nodeToColoredBlocks.get(node).size();
    }

    public static ArrayList<String> extractCodeBlock(ArrayList<String> lines, int firstInMethodNumber) {
        ArrayList<String> codeBlock = new ArrayList<>();
        int braceCount = 0;
        boolean inBlock = false;

        // Adjust the startLine based on the methodFirstLineNumber
        int startLine = firstInMethodNumber;

        for (int i = startLine; i < lines.size(); i++) {
            String line = lines.get(i).trim();

            // Check for the start of the code block
            if (line.contains("{")) {
                braceCount++;
                inBlock = true;
            }

            // Add the current line to the code block
            if (inBlock) {
                codeBlock.add(lines.get(i));
            }

            // Check for the end of the code block
            if (line.contains("}")) {
                braceCount--;
                if (braceCount == 0) {
                    break;
                }
            }
        }

        return codeBlock;
    }

    public static ArrayList<String> getSrcMethodCode(ArrayList<String> classSrcCode, int methodFirstLine,
            int methodLastLine)
            throws IOException {
        ArrayList<String> mCodeSippets = new ArrayList<>();
        int currLine = 0;
        for (String line : classSrcCode) {
            // System.out.println(line);
            currLine++;
            if (currLine >= methodFirstLine && currLine <= methodLastLine) {
                mCodeSippets.add(line);
                // linesOfSrcCode+=line+"\n";
            }
        }
        return (ArrayList<String>) mCodeSippets;
    }

    public static ArrayList<String> getSrcMethodCode(String filePath, int methodFirstLine, int methodLastLine)
            throws IOException {
        ArrayList<String> mCodeSippets = new ArrayList<>();
        int currLine = 0;
        List<String> methodSrcCode = FileUtil.readFileLines(filePath);
        for (String line : methodSrcCode) {
            // System.out.println(line);
            currLine++;
            if (currLine >= methodFirstLine && currLine <= methodLastLine) {
                mCodeSippets.add(line);
                // linesOfSrcCode+=line+"\n";
            }
        }
        return (ArrayList<String>) mCodeSippets;
    }

    public HashSet<CatchBlockInfo> checkCatchBlock(CGNode node) throws InvalidClassFileException {
        AstCallGraph.AstCGNode cgnode = (AstCallGraph.AstCGNode) node;
        Iterator<ISSABasicBlock> iterators = cgnode.getIR().getBlocks();
        ShrikeCTMethod method = (ShrikeCTMethod) cgnode.getMethod();
        HashSet<CatchBlockInfo> catchSet = new HashSet<>();
        // System.out.println("--------CheckCatchBlock-----------");
        while (iterators.hasNext()) {
            ISSABasicBlock ehbb = iterators.next();
            if (ehbb instanceof SSACFG.ExceptionHandlerBasicBlock) {

                SSACFG.ExceptionHandlerBasicBlock basicBlco = (SSACFG.ExceptionHandlerBasicBlock) ehbb;
                Iterator<TypeReference> itt = basicBlco.getCaughtExceptionTypes();
                List<String> exceptionTypes = new ArrayList<>();
                String lLevel = "";
                String consontant = "";

                while (itt.hasNext()) {
                    // System.out.println(itt.next().getName());
                    exceptionTypes.add(itt.next().getName().toString());
                }
                for (SSAInstruction instd : basicBlco.getAllInstructions()) {
                    if (instd instanceof SSAInvokeInstruction) {
                        SSAInvokeInstruction invokeInstruction = (SSAInvokeInstruction) instd;
                        MethodReference callee = invokeInstruction.getCallSite().getDeclaredTarget();
                        if (callee.getDeclaringClass().toString().contains("org/slf4j/Logger")
                                && callee.getDeclaringClass().toString().contains("Application")) {
                            lLevel = String.valueOf(callee.getName());
                            SymbolTable irSym = cgnode.getIR().getSymbolTable();
                            int numOfUse = instd.getNumberOfUses();
                            for (int i = 0; i < numOfUse; i++) {
                                int useNum = instd.getUse(i);
                                if (irSym.isConstant(useNum)) {
                                    String constValue = irSym.getValueString(useNum)
                                            .split("#")[irSym.getValueString(useNum).split("#").length - 1].strip();
                                    System.out.println("it's a constValues:" + constValue);
                                    consontant = constValue;

                                }
                            }
                        }

                    }

                }

                int assistLocateLineNum = -1;
                boolean foundTryBlock = false;
                ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg = cgnode.getCFG();
                for (ISSABasicBlock pred : cfg.getNormalPredecessors(ehbb)) {
                    foundTryBlock = true;
                    System.out.println("Try block content (Normal Predecessor Basic Block): " + pred);
                    for (SSAInstruction instdd : pred) {
                        // System.out.println("Instruction: " + instdd);
                        if (instdd != null) {
                            assistLocateLineNum = method.getLineNumber(method.getBytecodeIndex(instdd.iIndex()));
                        }
                    }
                }

                if (!foundTryBlock) {
                    for (ISSABasicBlock pred : cfg.getExceptionalPredecessors(ehbb)) {
                        foundTryBlock = true;
                        System.out.println("Try block content (Exceptional Predecessor Basic Block): " + pred);
                        for (SSAInstruction inst2 : pred) {
                            // System.out.println("Instruction: " + inst2+"LineNumber"+);
                            if (inst2 != null) {
                                assistLocateLineNum = method.getLineNumber(method.getBytecodeIndex(inst2.iIndex()));
                            }
                        }
                    }
                }

                if (!foundTryBlock) {
                    for (Iterator<ISSABasicBlock> it = cfg.getPredNodes(ehbb); it.hasNext();) {
                        ISSABasicBlock pred = it.next();
                        System.out.println("Try block content (Any Predecessor Basic Block): " + pred);
                        for (SSAInstruction inst3 : pred) {
                            // System.out.println("Instruction: " + inst3);
                            if (inst3 != null) {
                                assistLocateLineNum = method.getLineNumber(method.getBytecodeIndex(inst3.iIndex()));
                            }
                        }
                    }
                }

                CatchBlockInfo ctbInfo = new CatchBlockInfo(assistLocateLineNum, lLevel, consontant, exceptionTypes);
                catchSet.add(ctbInfo);
            }

        }

        return catchSet;

    }

    public void inspectExistLogs(ArrayList<String> extractedLogs) throws InvalidClassFileException {
        String user_prompt = "";
        ArrayList<Object> propocessedLogs = new ArrayList<>();
        int i = 1;
        for (String log : extractedLogs) {
            String trimmedLog = log.trim();
            propocessedLogs.add(trimmedLog);
            String tmpLog = String.format("<l%d>'%s'</l%d>\n", i, trimmedLog, i);
            String tmpctx = String.format("<ctx%d>'null'</ctx%d>\n", i, i);
            user_prompt += tmpLog + tmpctx;
            i++;
        }
        System.out.println(user_prompt);

    }

    public void genConfLog() {

    }

    public static void interactWithLLM(String up, String file_Path) {
        try {

            String pythonInterpreter = "python3";

            String scriptPath = "PythonCode/without_inspection_llm_interatction.py";

            // 构建命令和参数
            List<String> command = new ArrayList<>();
            command.add(pythonInterpreter);
            command.add(scriptPath);
            command.add("-p");
            command.add(up);
            command.add("-d");
            command.add(file_Path);

            // 创建ProcessBuilder对象
            ProcessBuilder processBuilder = new ProcessBuilder(command);

            // 启动进程
            Process process = processBuilder.start();

            // 获取脚本输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            // 获取脚本错误输出
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                System.err.println(line);
            }

            // 等待脚本执行完成
            int exitCode = process.waitFor();
            System.out.println("Exited with code: " + exitCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void IdentifyConfLogPoint() {

    }

    private void insertConfLog() {

    }

    public static <T> TaintedSinkInfo propocessTaintedPath(Statement stat) throws InvalidClassFileException {
        boolean isSink = true;
        TaintedSinkInfo ts = new TaintedSinkInfo();
        if (stat instanceof Statement) {
            Statement s = (Statement) stat;
            switch (s.getKind()) {
                case NORMAL: {
                    if (isSink) {
                        isSink = false;

                        IMethod m = s.getNode().getMethod();
                        NormalStatement statement = (NormalStatement) s;
                        IMethod.SourcePosition p = m.getSourcePosition(statement.getInstructionIndex());
                        IR ir = statement.getNode().getIR();

                        ShrikeBTMethod method = ((ShrikeBTMethod) m);
                        SSAInstruction[] inss = ir.getInstructions();
                        int lengthofInss = inss.length;
                        int lst = lengthofInss - 1;

                        CGNode TSNode = statement.getNode();
                        String fileName = p.toString().split("\\(")[0];
                        int methodFirstLineNumber = m.getLineNumber(method.getBytecodeIndex(0)) - 1;
                        int methodLastLineNumber = m.getLineNumber(method.getBytecodeIndex(lst));// method last line
                        int confCheckBlockFirstLineNumber = m
                                .getLineNumber(
                                        method.getBytecodeIndex(((NormalStatement) s).getInstructionIndex()));

                        ts = new TaintedSinkInfo(fileName,
                                TSNode,
                                methodFirstLineNumber + ":" + methodLastLineNumber,
                                String.valueOf(confCheckBlockFirstLineNumber) + ":-1",
                                statement.getInstruction(),
                                m.getSignature());

                    }
                    break;
                }
                case PARAM_CALLER:
                case NORMAL_RET_CALLER: {
                    break;
                } // source falls into this kind
                default: {
                    // System.out.println("Not Set type for it.");
                    break;
                }
            }
        }

        return ts;
    }

    private void propocessTaintedPath(HashMap<TaintedSinkInfo, HashMap<Statement, HashSet<String>>> tsSrcBook,
            List<Statement> stList, String accordinglyPara) throws IOException, InvalidClassFileException {
        boolean isSink = true;
        for (Statement s : stList) {
            switch (s.getKind()) {
                case NORMAL: {
                    if (isSink) {
                        isSink = false;

                        IMethod m = s.getNode().getMethod();
                        NormalStatement statement = (NormalStatement) s;
                        IMethod.SourcePosition p = m.getSourcePosition(statement.getInstructionIndex());
                        IR ir = statement.getNode().getIR();

                        ShrikeBTMethod method = ((ShrikeBTMethod) m);
                        SSAInstruction[] inss = ir.getInstructions();
                        int lengthofInss = inss.length;
                        int lst = lengthofInss - 1;

                        CGNode TSNode = statement.getNode();
                        String fileName = p.toString().split("\\(")[0];
                        int methodFirstLineNumber = m.getLineNumber(method.getBytecodeIndex(0)) - 1;
                        int methodLastLineNumber = m.getLineNumber(method.getBytecodeIndex(lst));
                        int confCheckBlockFirstLineNumber = m
                                .getLineNumber(method.getBytecodeIndex(((NormalStatement) s).getInstructionIndex()));

                        System.out.println("---------------Tainted Sink------------");
                        System.out.println(s);
                        System.out.println("ins:" + ((NormalStatement) s).getInstruction() + "\t fileName" + fileName);
                        System.out.println(
                                m.getSignature() + "<" + methodFirstLineNumber + ":" + methodLastLineNumber + ">");
                        System.out.println("block<" + confCheckBlockFirstLineNumber + "?>");

                        TaintedSinkInfo ts = new TaintedSinkInfo(fileName,
                                TSNode,
                                methodFirstLineNumber + ":" + methodLastLineNumber,
                                String.valueOf(confCheckBlockFirstLineNumber) + ":-1",
                                statement.getInstruction(),
                                m.getSignature());
                        if (!accordinglyPara.isEmpty()) {
                            ts.setParaSet(accordinglyPara);
                        }
                        // System.out.println(ts);

                        if (tsSrcBook.containsKey(ts)) {
                            HashMap<Statement, HashSet<String>> oldMap = tsSrcBook.get(ts);

                            oldMap.put(s, new HashSet<>());
                            tsSrcBook.put(ts, oldMap);
                        } else {
                            HashMap<Statement, HashSet<String>> hm = new HashMap<>();
                            hm.put(s, new HashSet<>());
                            tsSrcBook.put(ts, hm);
                        }
                    }
                    break;
                }
                case PARAM_CALLER:
                case NORMAL_RET_CALLER: {
                    break;
                } // source falls into this kind
                default: {
                    // System.out.println("Not Set type for it.");
                }
            }
        }

    }

    private static Position getPosition(Statement s) {
        IMethod m = s.getNode().getMethod();
        if (m instanceof AstMethod) {
            switch (s.getKind()) {
                case NORMAL:
                    return ((AstMethod) m).getSourcePosition(((NormalStatement) s).getInstructionIndex());
                case PARAM_CALLER:
                    return ((AstMethod) m).getSourcePosition(((ParamCaller) s).getInstructionIndex());
            }
        }

        return null;
    }

}
