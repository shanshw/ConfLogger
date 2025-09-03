package org.example;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
import org.apache.commons.cli.*;
import org.example.cf.ConfLogger;
import org.example.cf.OutFileProcessor;
import org.example.datastructure.ConfItem;
import org.example.pa.AsmAnalysis;
import org.example.pa.WalaAnalysis;
import org.example.pa.WalaTaintAnalysis;
import org.example.util.ConfReader;
import org.xml.sax.SAXException;
//import org.apache.commons.*;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;

import org.example.pa.WalaAnalysis;

public class Main {
    static WalaAnalysis walaAnalyzer;
    static AsmAnalysis asmAnalyzer = new AsmAnalysis();

    static ConfLogger confLogger = new ConfLogger();

    private static void initWalaAnalyzer(String scopePath, String exclusionPath, ClassLoader jarClassLoader) {
        try {
            walaAnalyzer = new WalaAnalysis(scopePath, exclusionPath, jarClassLoader);
        } catch (IOException e) {
            System.out.println("initiWalaAnalyzer");
            throw new RuntimeException(e);
        } catch (ClassHierarchyException e) {
            System.out.println("initiWalaAnalyzer");
            throw new RuntimeException(e);
        } catch (CallGraphBuilderCancelException e) {
            System.out.println("initiWalaAnalyzer");
            throw new RuntimeException(e);
        } catch (Exception e) {
            System.out.println("initiWalaAnalyzer");
            e.printStackTrace();
        }
    }

    public static void genLog(String rootDir) {
        try {
            OutFileProcessor.processOutFiles(rootDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException,
            ClassHierarchyException, CallGraphBuilderCancelException, InvalidClassFileException, ExecutionException,
            InterruptedException {

        Option optionJar = Option.builder("j")
                .required(true)
                .desc("jar file path")
                .longOpt("jar")
                .hasArg()
                .build();

        Option optionConfDoc = Option.builder("cd")
                .required(true)
                .desc("configuration file path")
                .longOpt("cdoc")
                .hasArg()
                .build();

        Option optionScope = Option.builder("sc")
                .required(true)
                .desc("scope file path, txt.")
                .longOpt("scope")
                .hasArg()
                .build();

        Option optionExclusion = Option.builder("ex")
                .required(true)
                .desc("exclusion file path, txt.")
                .longOpt("exclusion")
                .hasArg()
                .build();

        Options options = new Options();
        options.addOption(optionJar);
        options.addOption(optionConfDoc);
        options.addOption(optionScope);
        options.addOption(optionExclusion);

        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmd = parser.parse(options, args);
            String source_path = cmd.getOptionValue("jar");
            String conf_doc = cmd.getOptionValue("cd");
            String scope_path = cmd.getOptionValue("scope");
            String exclusion_path = cmd.getOptionValue("exclusion");

            File jarFile = new File(source_path);
            URL jarURL = jarFile.toURI().toURL();
            URLClassLoader jarClassLoader = new URLClassLoader(new URL[] { jarURL });

            System.out.println("source_path:" + source_path);
            System.out.println("conf_doc:" + conf_doc);
            System.out.println("scope_path:" + scope_path);
            System.out.println("exclusion_path:" + exclusion_path);
            // process the configuration docs
            // option
            initWalaAnalyzer(scope_path, exclusion_path, jarClassLoader);
            HashSet<String> specified_config_engines = new HashSet<>(); //

            ConfReader confReader = new ConfReader(Path.of(conf_doc), "xml");
            Collection<ConfItem> listConfItems = confReader.write_and_store();
            HashSet<String> configParaName = new HashSet<>();

            for (ConfItem cf : listConfItems) {
                configParaName.add(cf.getCname());
                // System.out.println(cf.getCname());
            }

            System.out.println("configItem search done"); // 加载configuration documentations
            // program identification
            /* confOBj Identification */
            // specifying the configuration engines

            // HashSet<String> confObjs =
            // asmAnalyzer.getConfObjsWithGettersByConfigDocs(conf_doc,source_path);
            HashMap<String, List<String>> confObjsWithFiled = asmAnalyzer.getConfObjsWithGettersByConfigDocs(conf_doc,
                    source_path);

            System.out.println("ASM analyzer to find confObj done");
            for (String ConfigObjs : confObjsWithFiled.keySet()) {
                specified_config_engines.add(ConfigObjs);
                System.out.println("ASM-found configObjs:" + ConfigObjs);
            }

            HashMap<IClass, String> refinedConfObjsMap = walaAnalyzer.getRefindConfObjs(specified_config_engines);

            Set<IClass> refinedConfObjs = refinedConfObjsMap.keySet();

            HashSet<IClass> keyHolders = new HashSet<>();
            HashSet<IClass> dictHolders = new HashSet<>();
            HashSet<IClass> boths = new HashSet<>();

            System.out.println("RefinedConfOBjs by walaAnalyzer done");
            if (refinedConfObjs.isEmpty()) {
                System.out.println("refinedConfObjs is empty");
                return;
            }
            for (IClass klass : refinedConfObjs) {
                System.out.println(klass.getName() + "\t type:" + refinedConfObjsMap.get(klass));
                if (refinedConfObjsMap.get(klass).contains("Key")) {
                    keyHolders.add(klass);
                } else if (refinedConfObjsMap.get(klass).contains("Dict")) {
                    dictHolders.add(klass);
                } else {
                    boths.add(klass);
                }
            }

            /* confVar Identification */
            // 1. call to getters of confObjs if getter exist
            // 2. call to public filed
            HashMap<IMethod, List<Integer>> callerswithIndexes = walaAnalyzer.getCallstoConfObjs(
                    confObjsWithFiled, configParaName);

            System.out.println("callersWithIndexes by walaAnalyzer done");

            // Set<List<Statement>> taintedPaths = walaAnalyzer.TaintAnalysis();
            walaAnalyzer.TaintAnalysis(keyHolders, dictHolders, boths);
            // HashMap<Statement, String> srcParaBook =
            // WalaTaintAnalysis.getSrcPropertyBook();
            // // for(Statement key:srcParaBook.keySet()){
            // // System.out.println(key);
            // // System.out.println(srcParaBook.get(key));
            // // }

            // System.out.println("TaintAnalysis by walaAnalyzer done");

            // if (taintedPaths.isEmpty()) {
            // System.out.println("No tatined Paths Found.");
            // return;
            // }

            /* Configuration Checking Blocks Identification */
            // String projectRoot =
            // "/usr/local/Lcg-src/src/main/resources/jars/alluxio-core-common-313/";
            // confLogger.extractExistLogs(projectRoot, taintedPaths, srcParaBook);

            /* confLog Point Identification */

            // confLog Generation
        } catch (Exception e) {
            // System.out.println(e);
            e.printStackTrace();
        }

    }
}