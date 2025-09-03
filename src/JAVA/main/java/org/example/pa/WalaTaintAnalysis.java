package org.example.pa;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.*;
import com.ibm.wala.ipa.slicer.Statement.Kind;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.traverse.BFSPathFinder;

import org.example.cf.ConfLogger;
import org.example.datastructure.TaintedSinkInfo;
import org.example.util.FileUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

public class WalaTaintAnalysis {
    private static HashMap<String, HashSet<Statement>> nodeToSinkStatement = new HashMap<>();

    private static HashMap<Statement, String> srcParaBook = new HashMap<Statement, String>();

    private static HashSet<IClass> confObjs;

    private static HashSet<String> targetClassNames = new HashSet<>(); // name of the confObjs

    private static HashSet<String> getterBelongs = new HashSet<>();

    private static HashSet<String> bothNames = new HashSet<>();

    private static HashSet<String> dictHolderNames = new HashSet<>();

    private static HashSet<String> confItemNames;

    private static HashSet<Statement> srcStatements = new HashSet<>();

    private static HashSet<Statement> sinkStatements = new HashSet<>();

    private static HashSet<IClass> dictHolders = new HashSet<>();

    private static HashSet<IClass> keyHolders = new HashSet<>();

    private static HashSet<IClass> bothHolders = new HashSet<>();

    @FunctionalInterface
    interface EndpointFinder<T> {
        boolean endpoint(T s) throws InvalidClassFileException;
    }

    public static <T> void init(HashSet<IClass> keys, HashSet<IClass> dicts, HashSet<IClass> boths,
            HashSet<String> cfItemNames) {
        keyHolders = keys;
        dictHolders = dicts;
        bothHolders = boths;
        for (IClass klass : keyHolders) {
            targetClassNames.add(String.valueOf(klass.getName()));
        }
        for (IClass klass : boths) {
            // targetClassNames.add(String.valueOf(klass.getName()));
            getterBelongs.add(String.valueOf(klass.getName()));
            bothNames.add(String.valueOf(klass.getName()));
        }
        for (IClass klass : dicts) {
            getterBelongs.add(String.valueOf(klass.getName()));
            dictHolderNames.add(String.valueOf(klass.getName()));
        }
        getterBelongs.add("Lorg/apache/hadoop/conf/Configuration");

        // graph = G;
        confItemNames = cfItemNames;
    }

    public static EndpointFinder<Statement> callGetterSource = (s) -> {
        if (s.getKind() == Kind.NORMAL_RET_CALLER) { // NORMAL_RET_CALLER
            NormalReturnCaller statement = (NormalReturnCaller) s;
            MethodReference callee = statement.getInstruction().getCallSite().getDeclaredTarget();
            IMethod caller = statement.getNode().getMethod();
            if (callee.getName().toString().toLowerCase().contains("get")
                    && caller.getDeclaringClass().getClassLoader().toString().contains("Application")) // sources:
                                                                                                       // Configuration
                                                                                                       // Engine.getXXX(Config
            {
                // System.out.println("----------------check if source-----------");
                // System.out.println("caller:" + caller + "\t callee:" + callee);

                String cName = callee.getDeclaringClass().getName().toString();
                // int dd = 0;
                // if (cName.contains("Configuration")) {
                // dd++;
                // System.out.println("ddd:" + dd);
                // }

                SymbolTable irSym = statement.getNode().getIR().getSymbolTable();
                int numOfUse = statement.getInstruction().getNumberOfUses();
                Boolean atLeastOneCallStaticFiled = false;
                Boolean getterGotConfigParm = false;
                for (int i = 0; i < numOfUse; i++) {
                    int useNum = statement.getInstruction().getUse(i);
                    if (irSym.isConstant(useNum)) {
                        String constValue = irSym.getValueString(useNum)
                                .split("#")[irSym.getValueString(useNum).split("#").length - 1].strip();
                        // System.out.println("it's a constValues:" + constValue);
                        if (confItemNames.contains(constValue)) {
                            atLeastOneCallStaticFiled = true;
                            // System.out.println("passed param is confItemNames:" + constValue);
                            srcParaBook.put(s, constValue);
                            break;
                        }
                    }
                }

                if (!atLeastOneCallStaticFiled && !(getterBelongs.contains(cName) ||
                        cName.contains("Ljava/util/Map")
                        || cName.contains("Ljava/util/HashMap"))) {
                    return false;
                }

                for (String targetClassName : targetClassNames) {
                    if (callee.getDescriptor().toString().split("\\)")[0].contains(targetClassName)) {

                        System.out.println("identify sources: " + callee);
                        getterGotConfigParm = true;
                        break;
                    }
                }
                for (String targetClassName : bothNames) {
                    if (callee.getDescriptor().toString().split("\\)")[0].contains(targetClassName)) {

                        System.out.println("identify sources: " + callee);
                        getterGotConfigParm = true;
                        break;
                    }
                }
                if (bothNames.contains(cName)) {
                    return true;
                }
                if (atLeastOneCallStaticFiled || getterGotConfigParm) {
                    return true;
                }

            }

        }
        return false;
    };

    public static EndpointFinder<Statement> LogSink = (s) -> {
        if (s.getKind() == Kind.NORMAL) { // NORMAL_RET_CALLER
            NormalStatement statement = (NormalStatement) s;
            SSAInstruction inst = statement.getInstruction();
            if (inst instanceof SSAInvokeInstruction) {
                SSAInvokeInstruction instruction = (SSAInvokeInstruction) inst;
                MethodReference callee = instruction.getCallSite().getDeclaredTarget();
                if (callee.getDeclaringClass().getName().toString().contains("org/slf4j/Logger")
                        && (callee.toString().contains("info") || callee.toString().contains("error")
                                || callee.toString().contains("trace") || callee.toString().contains("debug")
                                || callee.toString().contains("warn"))) {
                    System.out.println(callee);
                    return true;
                }

            }

        }
        return false;
    };

    public static EndpointFinder<Statement> branchSink = (s) -> {
        if (s.getKind() == Kind.NORMAL) {

            NormalStatement ns = (NormalStatement) s;
            SSAInstruction inst = ns.getInstruction();

            if (inst instanceof SSAConditionalBranchInstruction) {
                IMethod method = s.getNode().getMethod();
                if (method.getName().toString().toLowerCase().contains("equals")) {
                    return false;
                }
                String cName = method.getDeclaringClass().getName().toString();
                if (cName.contains("$"))
                    cName = cName.substring(0, cName.lastIndexOf('$'));
                if (method.getDeclaringClass().getClassLoader().toString().contains("Application")) {
                    if (targetClassNames.contains(cName)
                            || getterBelongs.contains(cName)) {
                        return false;
                    }

                    return true;
                    // }

                }

            }

        }

        return false;
    };

    public static HashMap<Statement, String> getSrcPropertyBook() {
        return srcParaBook;
    }

    public static HashSet<Statement> getSinkStatements() {
        return sinkStatements;
    }

    public static HashSet<Statement> getSrcStatements() {
        return srcStatements;
    }

    public static <T> HashSet<TaintedSinkInfo> realGetPathsGetTaintedSink(HashSet<T> srcStatements,
            HashSet<T> sinkStatements,
            Graph<T> G,
            boolean muiltiThreas) throws InvalidClassFileException {
        // HashSet<T> taintedSinks = new HashSet<>();
        HashSet<TaintedSinkInfo> taintedSinkInfos = new HashSet<>();
        // Set<List<T>> result = HashSetFactory.make();
        // for (final T src : srcStatements) { //
        // for (final T dst : sinkStatements) {

        FilteredBFSPathFinder<T> paths = new FilteredBFSPathFinder<>(G, new NonNullSingletonIterator<>(srcStatements),
                new Predicate<T>() {
                    @Override
                    public boolean test(T t) {
                        // return t.equals(dst);
                        return false;
                    }
                });
        paths.findPath(srcStatements, sinkStatements);

        return taintedSinkInfos;
    }

    public static String readSourceFileContent(CGNode node) {
        ArrayList<String> classCodeContent = new ArrayList<>();
        Reader stream = node.getMethod().getDeclaringClass().getSource();
        if (stream != null) {

            StringWriter writer = new StringWriter();
            try (BufferedReader bufferedReader = new BufferedReader(stream)) {
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    classCodeContent.add(line);
                    writer.write(line);
                    writer.write(System.lineSeparator());
                }
            } catch (IOException e) {

                e.printStackTrace();
            }

            String content = writer.toString();
            return content;
            // System.out.println(content);
        } else {
            System.out.println("Stream is null, no source code available.");
            return null;
        }

    }

    public static <T> String searchForPara(T t) {
        return srcParaBook.get(t);
    }

    public static HashMap<String, HashSet<Statement>> getNodeToSink() {
        return nodeToSinkStatement;
    }

    public static <Statment> void getPaths(HashSet<String> cItemNames, HashSet<IClass> keyHolders,
            HashSet<IClass> dictHolders, HashSet<IClass> both, Graph<Statement> G,
            EndpointFinder<Statement> sources, EndpointFinder<Statement> sinks)
            throws ExecutionException, InterruptedException, InvalidClassFileException {
        init(keyHolders, dictHolders, both, cItemNames);
        System.out.println("initization init function done");

        for (Statement src : G) {
            if (sources.endpoint(src)) {
                srcStatements.add((Statement) src);
            } else if (sinks.endpoint(src)) {
                sinkStatements.add((Statement) src);
                Statement s = (Statement) src;
                CGNode node = s.getNode();
                if (!node.getMethod().getDeclaringClass().getClassLoader().toString().contains("Application"))
                    continue;
                String methodSig = node.getMethod().getSignature();
                // System.out.println("sink-methodSig:" + methodSig);
                if (!nodeToSinkStatement.containsKey(methodSig)) {
                    HashSet<Statement> hs = new HashSet<>();
                    hs.add(s);
                    nodeToSinkStatement.put(methodSig, hs);
                } else {
                    nodeToSinkStatement.get(methodSig).add(s);
                }
            }

        }
        System.out.println("length of source:" + srcStatements.size());
        System.out.println("length of sink:" + sinkStatements.size());

        System.out.println("src and sinks get done");
        // return realGetPaths(srcStatements,sinkStatments,G);
        // int processedSink = 0;
        // int processedSource = 0;

    }

}
