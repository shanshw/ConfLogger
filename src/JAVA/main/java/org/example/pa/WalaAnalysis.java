package org.example.pa;

import java.time.Instant;
import com.ibm.wala.cast.ir.ssa.AssignInstruction;
import com.ibm.wala.cast.java.client.impl.ZeroOneContainerCFABuilderFactory;
import com.ibm.wala.cast.java.ipa.modref.AstJavaModRef;
import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.*;
// import com.ibm.wala.classLoader.CompoundModule.Reader;
import com.ibm.wala.core.util.config.AnalysisScopeReader;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.callgraph.impl.DefaultContextSelector;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.CPAContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallerSiteContext;
import com.ibm.wala.ipa.callgraph.propagation.cfa.nCFAContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.rta.CallSite;
import com.ibm.wala.ipa.callgraph.pruned.ApplicationLoaderPolicy;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.slicer.*;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.intset.IntSet;
import org.example.datastructure.ExtendedBlock;
import org.example.datastructure.TaintedSinkInfo;
import org.example.util.FileUtil;
//import com.ibm.wala.ipa.modref.ModRef;
//import com.ibm.wala.ipa.slicer.SDG;
//import com.ibm.wala.ipa.slicer.Slicer;
//import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.shrike.shrikeBT.IInstruction;
import com.ibm.wala.shrike.shrikeBT.InvokeInstruction;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.*;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.collections.Pair;
// import com.ibm.wala.util.io.FileUtil;

import org.example.cf.ConfLogger;
import org.example.datastructure.ConfItem;
import org.example.datastructure.CtxCodeBlock;
import org.example.util.Print;
import org.jf.dexlib2.util.InstructionUtil;

import soot.dava.internal.AST.ASTTryNode.container;
import soot.dexpler.instructions.InvokeStaticInstruction;
import soot.dexpler.instructions.InvokeVirtualInstruction;
import soot.jimple.parser.node.AMethodSignature;
//import org.example.datastructure.ConfItem;

import javax.annotation.Tainted;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static com.ibm.wala.ipa.callgraph.propagation.PointsToSetVariable.instanceKeys;
import static org.example.pa.WalaTaintAnalysis.*;

public class WalaAnalysis {
    // private static Logger logger = LoggerFactory.getLogger(WalaAnalysis.class);

    // private static asmAnalysis asmModule = new asmAnalysis();

    // key: name of the bound field
    // value: the qualified name of the method.
    static public HashMap<IClass, String> confObjMap = new HashMap<>();
    static public HashMap<String, HashSet<Statement>> nodeToSink;
    static private AnalysisScope analysisScope;
    static private ClassHierarchy cha;
    static private IAnalysisCacheView cache;
    static private Iterable<Entrypoint> eps;
    static private AnalysisOptions options;
    static private CallGraphBuilder<InstanceKey> builder;
    static private NullProgressMonitor monitor = new NullProgressMonitor();
    static private CallGraph cg;
    static private Iterator<IClass> classes;

    static private HashSet<IClass> confObjs;

    private HashMap<String, IMethod> var_dics = new HashMap<>();

    private static HashSet<String> confFiled = new HashSet<>();

    private static HashSet<String> confPara = new HashSet<>();

    // static WalaTaintAnalysis wtAnalysis = new WalaTaintAnalysis();

    public WalaAnalysis(String scope, String exclusion_file, ClassLoader jarClassLoader)
            throws IOException, ClassHierarchyException, CallGraphBuilderCancelException {

        analysisScope = AnalysisScopeReader.instance.readJavaScope(scope, new File(exclusion_file), jarClassLoader); // ClassLoader.getSystemClassLoader()
        cha = ClassHierarchyFactory.make(analysisScope); // Get the ClassHierarchy Relation in the class files: WithRoot
        // originalnal:makeWithRoot
        cache = new AnalysisCacheImpl();
        eps = new AllApplicationEntrypoints(analysisScope, cha); // Get the entrypoints of the classes.
        options = new AnalysisOptions(analysisScope, eps);

        builder = new ZeroOneContainerCFABuilderFactory().make(options, cache, cha); // ZeroOneContainerCFABuilderFactory().make(options,
                                                                                     // cache, cha);
        cg = builder.makeCallGraph(options, monitor); // Create the call graph builder using ZeroCFA
        // (context-insensitive)
        // classes = cha.iterator();
    }

    public static void TaintAnalysis(HashSet<IClass> keyHolders, HashSet<IClass> dictHolders,
            HashSet<IClass> boths) // cocurrency method
            throws IOException, ExecutionException, InterruptedException, InvalidClassFileException,
            IllegalArgumentException, CancelException {
        PointerAnalysis<InstanceKey> ptr = builder.getPointerAnalysis(); // get ptr
        Slicer.DataDependenceOptions data = Slicer.DataDependenceOptions.NO_BASE_NO_HEAP_NO_EXCEPTIONS;
        Slicer.ControlDependenceOptions control = Slicer.ControlDependenceOptions.NO_EXCEPTIONAL_EDGES; // do not
        // track
        // NO_EXCEPTIONAL_EDGES
        SDG<InstanceKey> sdg = new SDG<InstanceKey>(cg, ptr, new AstJavaModRef<InstanceKey>(), data, control);

        // do analysis
        System.out.println("initizilation of Taint Done, begin to do analysis.");
        // // CallGraph callGrph = sdg.getCallGraph();
        WalaTaintAnalysis.getPaths(confPara, keyHolders, dictHolders, boths, sdg, callGetterSource,
                branchSink); // branchSink
        System.out.println("get Paths Done - get sources and get sinks");
        nodeToSink = WalaTaintAnalysis.getNodeToSink();
        System.out.println("get nodeToSink done");
        HashSet<Statement> srcStats = WalaTaintAnalysis.getSrcStatements();
        // HashSet<Statement> sinks = WalaTaintAnalysis.getSinkStatements();
        FilteredBFSPathFinder<Statement> paths = new FilteredBFSPathFinder<>(sdg,
                new NonNullSingletonIterator<>(srcStats),
                new Predicate<Statement>() {
                    @Override
                    public boolean test(Statement t) {
                        // return t.equals(dst);
                        return false;
                    }
                });
        int i = 0;
        int size = nodeToSink.size();
        int poolSize = Runtime.getRuntime().availableProcessors();
        System.out.println("poolsize:" + poolSize);
        ExecutorService executorService = Executors.newFixedThreadPool(poolSize);
        List<Future<?>> futures = new ArrayList<>();
        TaintedBlockMerger tbm = new TaintedBlockMerger<>();

        int id = 0;
        int medsize = nodeToSink.size();
        for (String med : nodeToSink.keySet()) {
            Future<?> future = executorService.submit(() -> {
                try {
                    HashSet<Statement> sinksInOneNode = nodeToSink.get(med);
                    if (sinksInOneNode == null || sinksInOneNode.isEmpty())
                    // return;
                    {
                        ;
                    }
                    Map<Statement, List<Statement>> found = paths.findPathsToMultipleDsts(srcStats, sinksInOneNode);
                    if (found == null || found.isEmpty()) {
                    } else {
                        Set<Statement> taintedSinks = found.keySet();
                        CGNode node = taintedSinks.iterator().next().getNode();
                        Set<Set<Statement>> mergedBlocks = tbm.mergeBlocks(taintedSinks, node.getIR(),
                                node.getIR().getControlFlowGraph());

                        forNodeToHandleTaintedSink(taintedSinks, found);

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            // System.out.println("Done: " + i + " " + size);
            // System.out.println("\n\n");
            // i++;
            futures.add(future);
        }

        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executorService;

        Instant beginTime = Instant.now();
        try {
            futures.parallelStream().forEach(future -> {
                try {
                    future.get(60, TimeUnit.MINUTES);
                } catch (TimeoutException e) {
                    System.err.println("A task timed out. Cancelling the task...");
                    future.cancel(true);
                } catch (InterruptedException | ExecutionException e) {
                    System.err.println("A task failed with exception: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } finally {

            try {
                if (!executorService.awaitTermination(70, TimeUnit.MINUTES)) {
                    System.err.println("Forcing shutdown as tasks are still running...");
                    List<Runnable> notExecutedTasks = executorService.shutdownNow(); //

                    System.err.println("Number of tasks not executed: " +
                            notExecutedTasks.size());

                    long completedTaskCount = threadPoolExecutor.getCompletedTaskCount();
                    System.out.println("Number of completed tasks: " + completedTaskCount);

                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }

            Instant endTime = Instant.now();

            long elapsedMillis = java.time.Duration.between(beginTime,
                    endTime).toMillis();
            System.out.println("Elapsed time in milliseconds: " + elapsedMillis);
        }

    }

    public static void forNodeToHandleTaintedSink(Set<Statement> taintedSinks, Map<Statement, List<Statement>> found)
            throws InvalidClassFileException, IOException {

        String mdName = "";

        if (taintedSinks.isEmpty()) {
            System.out.println("tainted Path is empty.");
            return;
        }
        HashSet<TaintedSinkInfo> tses = new HashSet<>();

        for (Statement taintedStatement : taintedSinks) {
            TaintedSinkInfo tsinfo = getTaintedSinkInfo(found.get(taintedStatement));
            mdName = tsinfo.getMethodDescriptor();
            tses.add(tsinfo);
        }

        ArrayList<String> methodSrc = new ArrayList<String>();

        HashSet<CtxCodeBlock> ctxes = new HashSet<>();
        for (TaintedSinkInfo s : tses) {
            ConfLogger.colorBasicBlocks(s.getStats());
        }
        // if (!taintedSinksForNode.isEmpty()) {

        System.out.println("Attemp to get ExtendedBlocks Begin");
        List<ExtendedBlock> extendedBlocks = getExtenededBlocks(tses, methodSrc,
                ctxes);
        if (extendedBlocks == null) {
            System.out.println("Cannot get the source code of ExtendedBlocks for node:" + mdName);
            return;
        }
        System.out.println("Get files of extendedBlocks with ctxes.");
        System.out.println("methodSrc Length:" + methodSrc.size());
        System.out.println("method ctxes length:" + ctxes.size());
        // FileUtil.writeToFile(tsinfo);
        FileUtil.writeToFile(tses,
                extendedBlocks, methodSrc, ctxes);
        System.out.println("ExtendedBLocks-size:" + extendedBlocks.size());
        // }

    }

    public static TaintedSinkInfo getTaintedSinkInfo(List<Statement> path) throws InvalidClassFileException {
        Statement src = path.get(0);
        Statement dst = path.get(path.size() - 1);

        String para = WalaTaintAnalysis.searchForPara(src);
        TaintedSinkInfo ts = ConfLogger.propocessTaintedPath(dst);
        ts.setParaSet(para);
        ts.setStats((Statement) dst);
        ts.setSrc((Statement) src);
        ts.setTaintedPath((List<Statement>) path);
        return ts;

    }

    public static List<ExtendedBlock> getExtenededBlocks(HashSet<TaintedSinkInfo> taintedSinksForNode,
            ArrayList<String> methodSrcCode, HashSet<CtxCodeBlock> ctx)
            throws InvalidClassFileException, IOException {

        CGNode node = null;
        for (TaintedSinkInfo s : taintedSinksForNode) {
            node = s.getNode();
            break;
        }

        int taintedSinkFirstLineNum = 10000;

        List<ExtendedBlock> result = new ArrayList<>();
        int extendBlocksNum = ConfLogger.getColredNumber(node);
        SSACFG cfg = node.getIR().getControlFlowGraph();

        ShrikeCTMethod method = (ShrikeCTMethod) node.getMethod();
        Iterator<CGNode> calltersToTheMethod = cg.getPredNodes(node);
        System.out.println("color the basicblocks of nodes");

        int lengthofInss = node.getIR().getInstructions().length;
        int lst = lengthofInss - 1;
        int methodFirstLineNumber = method.getLineNumber(method.getBytecodeIndex(0)) - 1; // method first
        int methodLastLineNumber = method.getLineNumber(method.getBytecodeIndex(lst));// method last line

        ArrayList<String> classCodeContent = new ArrayList<>();
        java.io.Reader stream = method.getDeclaringClass().getSource();
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

        } else {
            System.out.println("Stream is null, no source code available.");
            return null;
        }
        methodSrcCode.addAll(ConfLogger.getSrcMethodCode(classCodeContent,
                methodFirstLineNumber,
                methodLastLineNumber + 1));
        System.out.println("methodSrcCode Length:" + methodSrcCode.size());

        for (TaintedSinkInfo ts : taintedSinksForNode) {

            Statement s = ts.getStats();
            int tempInst = ((NormalStatement) s).getInstructionIndex();
            int tempFirstLine = method.getLineNumber(method.getBytecodeIndex(tempInst));
            if (tempFirstLine > methodFirstLineNumber && tempFirstLine < taintedSinkFirstLineNum) {
                taintedSinkFirstLineNum = tempFirstLine;
            }

        }

        HashSet<CtxCodeBlock> context = processOneHopCaller(calltersToTheMethod, node);
        ctx.addAll(context);
        System.out.println("processOneHopCaller done,size of the context:" + context.size());

        for (int i = 0; i < extendBlocksNum; i++) {
            List<ISSABasicBlock> basicBlocks = ConfLogger.getBlocksForNodeAndColor(node, i);
            ExtendedBlock eb = new ExtendedBlock(node, basicBlocks, i);

            int extendFirstLine = 10000;
            int extendLastLine = -1;

            boolean isBasicFirstLineValid = true;
            boolean isBasicLastLineValid = true;

            eb.setMethodFirstLine(methodFirstLineNumber);
            eb.setMethodLastLine(methodLastLineNumber);

            for (ISSABasicBlock bb : basicBlocks) {
                if (cfg.isCatchBlock(bb.getNumber())) {
                    System.out.println("passed catch block");
                }

                // eb.addBlock(bb);
                int basicFirstLine = getFirstLineNumForABasicBlock(bb, method);
                if (basicFirstLine == -1) {
                    isBasicFirstLineValid = false;
                }

                int basicLastLine = getLastLineNumForABasicBlock(bb, method, methodLastLineNumber);
                if (basicLastLine == -1) {
                    isBasicLastLineValid = false;
                }

                if (isBasicFirstLineValid) {
                    if (basicFirstLine > 0 && methodFirstLineNumber <= basicFirstLine
                            && basicFirstLine < extendFirstLine) {
                        extendFirstLine = basicFirstLine;
                    }
                }

                if (isBasicLastLineValid) {
                    if (basicLastLine != -1 && basicLastLine <= methodLastLineNumber
                            && basicLastLine > extendLastLine) {
                        extendLastLine = basicLastLine;
                    }
                }

            }

            if (extendFirstLine != 10000) {
                eb.setFirstLineNumber(extendFirstLine);
            } else {
                eb.setFirstLineNumber(taintedSinkFirstLineNum);

            }
            if (extendLastLine != -1) {
                eb.setLastLineNumber(extendLastLine);
            } else {
                eb.setLastLineNumber(methodLastLineNumber);
            }

            ArrayList<String> ccb = ConfLogger.extractCodeBlock(classCodeContent, eb.getFirstLineNumber());
            eb.setCCB(ccb);

            result.add(eb);

        }

        return result;
    }

    private static HashSet<CtxCodeBlock> processOneHopCaller(Iterator<CGNode> calltersToTheMethod,
            CGNode calleeNode) throws InvalidClassFileException, IOException {
        HashSet<CtxCodeBlock> result = new HashSet<>();
        Iterator<CGNode> nodeIter = calltersToTheMethod;
        MethodReference calleeMethodRef = calleeNode.getMethod().getReference();

        while (nodeIter.hasNext()) {
            CGNode callerNode = nodeIter.next();
            if (callerNode.getMethod().isWalaSynthetic() || callerNode.getMethod().isSynchronized()
                    || callerNode.getMethod().isSynthetic()) {
                System.out.println("caller is not a application node.");
                continue;

            }

            IR callerIR = callerNode.getIR();
            ControlFlowGraph cfg = callerIR.getControlFlowGraph();
            ShrikeCTMethod method = (ShrikeCTMethod) callerNode.getMethod();

            ArrayList<String> classCodeContent = new ArrayList<>();
            java.io.Reader stream = method.getDeclaringClass().getSource();
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
                // System.out.println(content);
            } else {
                System.out.println("Stream is null, no source code available.");
            }

            int lengthofInss = callerIR.getInstructions().length;
            int lst = lengthofInss - 1;
            int methodFirstLineNumber = method.getLineNumber(method.getBytecodeIndex(0)) - 1; // method first
            int methodLastLineNumber = method.getLineNumber(method.getBytecodeIndex(lst));// method last line
            ArrayList<String> methodSrcCode = ConfLogger.getSrcMethodCode(classCodeContent,
                    methodFirstLineNumber,
                    methodLastLineNumber + 1);
            if (methodSrcCode.isEmpty()) {
                continue;
            }

            CtxCodeBlock ctxBlock = new CtxCodeBlock(callerNode);
            ctxBlock.setMethodSrcCode(methodSrcCode);
            ctxBlock.setMethodFirstLine(methodFirstLineNumber);
            ctxBlock.setMethodLastLine(methodLastLineNumber);

            Iterator<CallSiteReference> callSiteIter = callerNode.iterateCallSites();
            while (callSiteIter.hasNext()) {
                CallSiteReference callSiteRef = callSiteIter.next();
                if (callSiteRef.getDeclaredTarget().equals(calleeMethodRef)) {

                    ISSABasicBlock[] currentBlocks = callerIR.getBasicBlocksForCall(callSiteRef);
                    int extendFirstLine = 10000;
                    int extendLastLine = -1;
                    for (ISSABasicBlock currentBlock : currentBlocks) {
                        if (currentBlock.isCatchBlock()) {
                            System.out.println("when processing the one-hop-caller, skip the catch block");
                            continue;
                        }

                        ctxBlock.setNewBlocks(currentBlock);

                        Iterator<ISSABasicBlock> it = cfg.getPredNodes(currentBlock);
                        while (it.hasNext()) {
                            boolean isFirstValid = true;

                            ISSABasicBlock predBlock = it.next();
                            if (predBlock.isCatchBlock())
                                continue;
                            ctxBlock.setNewBlocks(predBlock);
                            int basicFirstLine = getFirstLineNumForABasicBlock(predBlock, method);
                            if (basicFirstLine == -1) {
                                isFirstValid = false;
                            }
                            if (isFirstValid) {
                                if (basicFirstLine > 0 && methodFirstLineNumber <= basicFirstLine
                                        && basicFirstLine < extendFirstLine) {
                                    extendFirstLine = basicFirstLine;
                                }
                            }
                        }

                        it = cfg.getSuccNodes(currentBlock);

                        while (it.hasNext()) {
                            boolean isLastValid = true;
                            ISSABasicBlock predBlock = it.next();
                            if (predBlock.isCatchBlock())
                                continue;
                            ctxBlock.setNewBlocks(predBlock);
                            int basicLastLine = getLastLineNumForABasicBlock(predBlock, method, methodLastLineNumber);
                            if (basicLastLine == -1) {
                                isLastValid = false;
                            }
                            if (isLastValid) {
                                if (basicLastLine != -1 && basicLastLine <= methodLastLineNumber
                                        && basicLastLine > extendLastLine) {
                                    extendLastLine = basicLastLine;
                                }
                            }

                        }

                        if (extendFirstLine != 10000) {
                            ctxBlock.setFirstLineNumber(extendFirstLine);
                        } else {
                            ctxBlock.setFirstLineNumber(methodFirstLineNumber);
                            ctxBlock.setFirstInvalid();
                        }
                        if (extendLastLine != -1) {
                            ctxBlock.setLastLineNumber(extendLastLine);
                        } else {
                            ctxBlock.setLastLineNumber(methodLastLineNumber);
                        }
                    }

                }
                result.add(ctxBlock);
            }
        }
        // for (CtxCodeBlock ctx : result) {
        // System.out.println("ctx in firsthopcaller:" + ctx.size());
        // }
        return result;

    }

    public static int getFirstLineNumForABasicBlock(ISSABasicBlock bb, ShrikeCTMethod method)
            throws InvalidClassFileException {
        int firstInsIndex = bb.getFirstInstructionIndex();
        if (firstInsIndex == -1)
            return -1;
        // if (firstInsIndex == -1)
        // return methodFirstLine;
        return method.getLineNumber(method.getBytecodeIndex(firstInsIndex));
    }

    public static int getLastLineNumForABasicBlock(ISSABasicBlock bb, ShrikeCTMethod method, int lastMethodNum)
            throws InvalidClassFileException {
        int lastInsIndex = bb.getLastInstructionIndex();
        if (lastInsIndex > lastMethodNum || lastInsIndex < 0)
            return -1;
        return method.getLineNumber(method.getBytecodeIndex(lastInsIndex));
    }

    public HashMap<IMethod, List<Integer>> getCallstoConfObjs(
            HashMap<String, List<String>> withStaticFiled, HashSet<String> configPara) {
        HashMap<IClass, HashSet<IMethod>> getters = new HashMap<>();// key: iclass-config obj
        HashSet<MethodReference> getterMethodRef = new HashSet<>();
        HashMap<IMethod, CallSiteReference> callsiteStats = new HashMap<>();// key: the caller to the getter functions;
                                                                            // callsite: the callsite struct
        confPara = configPara;
        confFiled = new HashSet<>();
        HashMap<IMethod, List<Integer>> callers = new HashMap<>(); // key:method value:a list of instruction indexes.

        for (String key : withStaticFiled.keySet()) {
            for (String f : withStaticFiled.get(key))
                confFiled.add(f);
        }

        IClass systemClass = cha.lookupClass(TypeReference.JavaLangSystem);
        confObjs.add(systemClass);

        return callers;

    }

    public HashMap<IClass, String> getRefindConfObjs(HashSet<String> targetClassNames) throws ClassHierarchyException {
        HashSet<String> getterSignatures = new HashSet<>();
        confObjs = new HashSet<>(); // HashSet<IClass>
        confObjMap = new HashMap<>();
        HashSet<String> CallertoGettersSignatures = new HashSet<>();

        for (String targetClassName : targetClassNames) { // Seed configuration objs
            // System.out.println("targetClassName:"+targetClassName); //one goes
            String outerClassName = "";
            if (targetClassName.contains("$")) {
                outerClassName = targetClassName.substring(0, targetClassName.lastIndexOf('$'));
            }
            // System.out.println("targetClassName:"+targetClassName+"\t
            // outer:"+outerClassName);
            ClassHierarchy newcha = ClassHierarchyFactory.makeWithRoot(analysisScope); // added root
            Iterator<IClass> it = newcha.iterator();
            while (it.hasNext()) {
                IClass klass = it.next();
                if (!klass.getClassLoader().toString().contains("Application")) // filter out
                    continue;
                if (klass.getName().toString().contains(targetClassName.replace(".", "/"))
                        || (!outerClassName.equals("")
                                && klass.getName().toString().contains(outerClassName.replace(".", "/"))
                                && !klass.getName().toString().contains("$"))) {
                    // if (klass.getName().toString().contains("PropertyKey")) {
                    // System.out.println("Propertykeys");
                    // }
                    Collection<IClass> subklasses = new ArrayList<>();
                    if (klass.getName().toString().contains(targetClassName.replace(".", "/"))) {
                        subklasses = newcha.getImmediateSubclasses(klass);
                    }
                    subklasses.add(klass);
                    for (IClass subklass : subklasses) // klass是specified configuration engine
                    {
                        Boolean ifdic = false;
                        if (!"Application".equals(subklass.getClassLoader().toString())) // ||
                                                                                         // subklass.getName().toString().contains("$")
                            continue;
                        // System.out.println("subclass:"+subklass);
                        // if(!subklass.getClass().getName().toString().contains("$"))
                        // {

                        // }
                        if (!subklass.isPublic()) { // subklass.isInterface() || subklass.isAbstract() ||
                            continue;
                        }
                        Collection<? extends IMethod> methods = subklass.getAllMethods();
                        for (IMethod method : methods) {
                            if (method.getDeclaringClass().getClassLoader().toString().contains("Application")
                                    && method.getName().toString().contains("get")) {
                                // System.out.println(method);
                                ifdic = true;
                                break;
                            }
                        }
                        confObjs.add(subklass);
                        if (ifdic) {
                            confObjMap.put(subklass, "Both");
                        } else {
                            confObjMap.put(subklass, "Key-Holder");
                        }

                        // System.out.println("ASm - confObjs:"+subklass);
                        // TypeReference interfaceRef = subklass.getReference();
                        // IClass targetInterface = cha.lookupClass(interfaceRef);
                        // Collection<? extends IMethod> methods = targetInterface.getAllMethods();
                    }
                    // break;
                }
            }

            Iterator<IClass> again = newcha.iterator();
            while (again.hasNext()) {
                IClass klass2 = again.next();
                if (confObjs.contains(klass2) || !"Application".equals(klass2.getClassLoader().toString())
                        || klass2.getName().toString().contains("$"))
                    continue;
                Collection<? extends IMethod> methods = klass2.getAllMethods();
                for (IMethod method : methods) {
                    if (method.getNumberOfParameters() == 0)
                        continue;
                    String med = method.getName().toString().toLowerCase();
                    String meDescriptor = method.getDescriptor().toString().split("\\)")[0];
                    Iterator<IClass> iterator = confObjs.iterator();
                    while (iterator.hasNext()) {
                        // System.out.println(meDescriptor);
                        // System.out.println(method.getDescriptor());
                        // System.out.println(seeobj.getName().toString());
                        IClass seeobj = iterator.next();
                        if (med.contains("get") && meDescriptor.contains(seeobj.getName().toString())) { // ||
                            if (!klass2.isPublic()) // klass2.isInterface() || klass2.isAbstract() ||
                                continue;
                            // confObjs.add(klass2);
                            confObjMap.put(klass2, "Dict-Holder");
                        }
                    }

                }

            }

            // }
            // }

        }
        return confObjMap;
    }

    public CallGraph getCG() {
        return cg;
    }

}
