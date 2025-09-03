package org.example.pa;

import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
import org.example.datastructure.ConfItem;
import org.example.util.CSVKeyExtractor;
import org.example.util.ConfReader;
import org.example.util.XMLKeyExtractor;
import org.example.util.YamlKeyExtractor;
import org.objectweb.asm.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class AsmAnalysis {

    // wala
    // private static WalaAnalysis walaAnalyzer = new WalaAnalysis();
    public HashMap<String, List<String>> getConfObjsWithGettersByConfigDocs(String yamlFilePath, String jarFilePath)
            throws IOException, ClassHierarchyException, CallGraphBuilderCancelException, InvalidClassFileException,
            ParserConfigurationException, SAXException {

        HashMap<String, Object> staticFieldsWithValues = new HashMap<>(); //
        HashSet<String> confObjs = new HashSet<>();
        HashMap<String, String> confObjMap = new HashMap<>();
        // HashMap<String, S onfObjsWithConfFiled = new HashMap();
        //
        HashMap<String, List<String>> getterFunctions = new HashMap<>(); // key: confObj class Name value: list of the
        // signature of the getter functions
        HashMap<String, List<String>> publicVisitConfFiled = new HashMap<>(); // key: confObj class Name value: the name
        // of the filed

        // Map<String, Object> yamlMap = YamlKeyExtractor.getYamlMap(yamlFilePath);
        // Map<String, String> yamlMap =
        // CSVKeyExtractor.getKeyValuesFromCSV(yamlFilePath);

        Map<String, String> yamlMap = XMLKeyExtractor.extractKeyValuesFromXML(yamlFilePath);
        HashSet<String> withoutPrefixNames = new HashSet<>();
        String prefiex = "yarn.";
        for (String name : yamlMap.keySet()) {
            String removePrefix = name.replaceFirst(prefiex, "");
            withoutPrefixNames.add(removePrefix.trim());
        }

        HashMap<String, HashSet<String>> recordedMethods = new HashMap<String, HashSet<String>>();

        try (JarFile jarFile = new JarFile(new File(jarFilePath))) {
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    try (InputStream is = jarFile.getInputStream(entry)) {
                        ClassReader classReader = new ClassReader(is);
                        classReader.accept(new ClassVisitor(Opcodes.ASM9) {
                            Boolean getterFlag = false;
                            Boolean isFiledPublic = false;

                            @Override
                            public FieldVisitor visitField(int access, String name, String descriptor, String signature,
                                    Object value) {

                                if ((access & Opcodes.ACC_STATIC) != 0) {
                                    String className = classReader.getClassName();
                                    String type = Type.getType(descriptor).getClassName();
                                    String fieldInfo = name + " " + className + " " + descriptor;
                                    String fieldValue = (value == null) ? "null" : value.toString(); // value
                                    // System.out.println(
                                    // "Field: " + fieldInfo + ", Value: " + fieldValue + ",Type: " + type);
                                    if (!fieldValue.equals("null") && (yamlMap.containsKey(fieldValue)
                                            || withoutPrefixNames.contains(fieldValue))) {

                                        staticFieldsWithValues.put(fieldInfo, value);
                                        confObjs.add(className);
                                        // if (confObjMap.containsKey(className)) {
                                        // String t = confObjMap.get(className);
                                        // if (t.contains("Key-Holder")) {
                                        // ;
                                        // } else if (t.contains("Dict-Holder")) {
                                        // confObjMap.put(className, "Both");
                                        // }
                                        // } else {
                                        // confObjMap.put(className, "Key-Holder");
                                        // }
                                        if ((access & Opcodes.ACC_PUBLIC) != 0) {
                                            if (!isFiledPublic)
                                                isFiledPublic = true;
                                            if (publicVisitConfFiled.containsKey(className)) {
                                                List<String> oldFileds = publicVisitConfFiled.get(className);
                                                oldFileds.add(name);
                                                publicVisitConfFiled.put(className, oldFileds);
                                            } else {
                                                publicVisitConfFiled.put(className,
                                                        new ArrayList<>(Collections.singleton(name)));
                                            }
                                        }
                                        if ((access & Opcodes.ACC_PROTECTED) != 0
                                                || (access & Opcodes.ACC_PRIVATE) != 0) {
                                            System.out.println("protected | private:\t" + "Field: " + fieldInfo
                                                    + ", Value: " + fieldValue + ", Type: " + type);
                                        }

                                    }
                                }
                                return super.visitField(access, name, descriptor, signature, value);
                            }

                            @Override
                            public void visitEnd() {
                                if ((!getterFlag && !isFiledPublic) && confObjs.contains(classReader.getClassName())) {
                                    confObjs.remove(classReader.getClassName());
                                }

                                super.visitEnd();
                            }

                            @Override
                            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                    String signature, String[] exceptions) {

                                String mName = name;
                                String mDes = descriptor;
                                String mSig = signature;
                                String className = classReader.getClassName();
                                boolean getMethodSlice = false;
                                String mdSig = String.format("%s.%s%s", className.replace('/', '.'), mName, mDes);
                                // System.out.println("Class:"+className + " Method Signature: " + name +
                                // descriptor);
                                if (name.contains("get") && confObjs.contains(className)) {
                                    getterFlag = true;
                                    if (getterFunctions.containsKey(className)) {
                                        List<String> oldGetters = getterFunctions.get(className);
                                        oldGetters.add(name + " " + descriptor);
                                        getterFunctions.put(className, oldGetters);

                                    } else {
                                        getterFunctions.put(className,
                                                new ArrayList<>(Collections.singleton(name + " " + descriptor)));
                                    }
                                }
                                // if(directCallGeters.contains(mdSig))
                                // {
                                // System.out.println("Method Refined Signature:"+mdSig+"\t
                                // getSignature:"+mSig);
                                // }
                                return new MethodVisitor(Opcodes.ASM9) {

                                    @Override
                                    public void visitLdcInsn(Object value) {
                                        // System.out.println(" visitLdcInsn: " + value);
                                        if (yamlMap.containsKey(value.toString().toLowerCase())) {
                                            // if(declaredMap.contains(value.toString().toLowerCase()))
                                            {
                                                // System.out.println(" -------Config--------\n visitLdcInsn: " +
                                                // value);
                                                String className = classReader.getClassName();
                                                HashSet<String> tmp = new HashSet<>();
                                                if (recordedMethods.containsKey(className)) {
                                                    tmp = recordedMethods.get(className);
                                                    tmp.add((mName == null ? "empty" : mName) + "\t"
                                                            + (mDes == null ? "empty" : mDes) + "\t"
                                                            + (mSig == null ? "empty" : mSig));
                                                    recordedMethods.replace(className, tmp);
                                                } else {
                                                    tmp.add((mName == null ? "empty" : mName) + "\t"
                                                            + (mDes == null ? "empty" : mDes) + "\t"
                                                            + (mSig == null ? "empty" : mSig));
                                                    recordedMethods.put(classReader.getClassName(), tmp);
                                                }
                                            }

                                        }
                                        super.visitLdcInsn(value);
                                    }

                                    public void visitLocalVariable(String name, String descriptor, String signature,
                                            Label start, Label end, int index) {
                                        // if (name.toLowerCase().contains("conf")) {
                                        // System.out.println("Local Variable: " +
                                        // "name=" + name + ", " +
                                        // "descriptor=" + descriptor + ", " +
                                        // "signature=" + signature + ", " +
                                        // "start=" + start + ", " +
                                        // "end=" + end + ", " +
                                        // "index=" + index);
                                        // }
                                        super.visitLocalVariable(name, descriptor, signature, start, end, index);
                                    }

                                    @Override
                                    public void visitParameter(String name, int access) {
                                        // System.out.println(" visitParameter: " + name);
                                        super.visitParameter(name, access);
                                    }

                                    public void visitTypeInsn(int opcode, String type) {
                                        // System.out.println("Type Instruction: " + opcodeToString(opcode) + ", type: "
                                        // + type);
                                        super.visitTypeInsn(opcode, type);
                                    }

                                    @Override
                                    public void visitInsn(int opcode) {
                                        // System.out.println(" visitInsn: " + opcodeToString(opcode));
                                        super.visitInsn(opcode);
                                    }

                                    @Override
                                    public void visitVarInsn(int opcode, int var) {
                                        // System.out.println(" visitVarInsn: " + opcodeToString(opcode) + ", " + var);
                                        super.visitVarInsn(opcode, var);
                                    }

                                    @Override
                                    public void visitIntInsn(int opcode, int operand) {
                                        // System.out.println(" visitIntInsn: " + opcodeToString(opcode) + ", " +
                                        // operand);
                                        super.visitIntInsn(opcode, operand);
                                    }

                                    @Override
                                    public void visitFieldInsn(int opcode, String owner, String name,
                                            String descriptor) {
                                        // System.out.println(" visitFieldInsn: " + opcodeToString(opcode) + ", " +
                                        // owner + "." + name + " " + descriptor);
                                        if (opcodeToString(opcode).equals("GETSTATIC") && owner.contains("Config")) {
                                            String className = classReader.getClassName();
                                            HashSet<String> tmp = new HashSet<>();
                                            if (recordedMethods.containsKey(className)) {
                                                tmp = recordedMethods.get(className);
                                                tmp.add((mName == null ? "empty" : mName) + "\t"
                                                        + (mDes == null ? "empty" : mDes) + "\t"
                                                        + (mSig == null ? "empty" : mSig));
                                                recordedMethods.replace(className, tmp);
                                            } else {
                                                tmp.add((mName == null ? "empty" : mName) + "\t"
                                                        + (mDes == null ? "empty" : mDes) + "\t"
                                                        + (mSig == null ? "empty" : mSig));
                                                recordedMethods.put(classReader.getClassName(), tmp);
                                            }
                                        }
                                        super.visitFieldInsn(opcode, owner, name, descriptor);
                                    }

                                    @Override
                                    public void visitMethodInsn(int opcode, String owner, String name,
                                            String descriptor, boolean isInterface) {
                                        // System.out.println(" visitMethodInsn: " + opcodeToString(opcode) + ", " +
                                        // owner + "." + name + descriptor + ", isInterface: " + isInterface);
                                        if (opcodeToString(opcode).toLowerCase().contains("invoke")) {
                                            String className = classReader.getClassName();
                                            HashSet<String> tmp = new HashSet<>();
                                            if (recordedMethods.containsKey(className)) {
                                                tmp = recordedMethods.get(className);
                                                tmp.add((mName == null ? "empty" : mName) + "\t"
                                                        + (mDes == null ? "empty" : mDes) + "\t"
                                                        + (mSig == null ? "empty" : mSig));
                                                recordedMethods.replace(className, tmp);
                                            } else {
                                                tmp.add((mName == null ? "empty" : mName) + "\t"
                                                        + (mDes == null ? "empty" : mDes) + "\t"
                                                        + (mSig == null ? "empty" : mSig));
                                                recordedMethods.put(classReader.getClassName(), tmp);
                                            }
                                        }
                                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                                    }

                                    @Override
                                    public void visitEnd() {
                                        // System.out.println(" Method end.");
                                        super.visitEnd();
                                    }
                                };
                            }
                        }, 0);
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw e;
                    }
                }
            }
        }

        // System.out.println("Length of
        // StaticFiledWithValues:"+staticFieldsWithValues.size());
        // for(String key:staticFieldsWithValues.keySet())
        // {
        // System.out.println("______static_______\tkey:"+key+"\tvalue:"+staticFieldsWithValues.get(key));
        // }
        //
        // System.out.println("Configuration Objs length:"+confObjs.size());
        // for(String ele:confObjs){
        // System.out.println("_________confObjs_______:\t"+ele);
        // for(String getters: getterFunctions.get(ele))
        // {
        // System.out.println("_________________getterfunctions_____________\t"+getters);
        // }
        // }

        // if(recordedMethods.isEmpty()){
        // System.out.println("no method captrued");
        // return;
        // }
        // for (String str : recordedMethods.keySet()) {
        // HashSet<String> methods = recordedMethods.get(str);
        // System.out.println("--------------------recordedMethod:"+str);
        // for(String value:methods){
        // System.out.print("\t"+value+"\n");
        // }
        // System.out.println("");
        // }

        // for(String klass:publicVisitConfFiled.keySet())
        // {
        // System.out.println("-----------Public visit Filed-------------\t"+klass);
        // for(String filed:publicVisitConfFiled.get(klass)) {
        // System.out.println("filed:\t"+filed);
        // }
        // }
        return publicVisitConfFiled;
    }

    private static String opcodeToString(int opcode) {
        switch (opcode) {
            case Opcodes.NOP:
                return "NOP";
            case Opcodes.ACONST_NULL:
                return "ACONST_NULL";
            case Opcodes.ICONST_M1:
                return "ICONST_M1";
            case Opcodes.ICONST_0:
                return "ICONST_0";
            case Opcodes.ICONST_1:
                return "ICONST_1";
            case Opcodes.ICONST_2:
                return "ICONST_2";
            case Opcodes.ICONST_3:
                return "ICONST_3";
            case Opcodes.ICONST_4:
                return "ICONST_4";
            case Opcodes.ICONST_5:
                return "ICONST_5";
            case Opcodes.LCONST_0:
                return "LCONST_0";
            case Opcodes.LCONST_1:
                return "LCONST_1";
            case Opcodes.FCONST_0:
                return "FCONST_0";
            case Opcodes.FCONST_1:
                return "FCONST_1";
            case Opcodes.FCONST_2:
                return "FCONST_2";
            case Opcodes.DCONST_0:
                return "DCONST_0";
            case Opcodes.DCONST_1:
                return "DCONST_1";
            case Opcodes.BIPUSH:
                return "BIPUSH";
            case Opcodes.SIPUSH:
                return "SIPUSH";
            case Opcodes.LDC:
                return "LDC";
            case Opcodes.ILOAD:
                return "ILOAD";
            case Opcodes.LLOAD:
                return "LLOAD";
            case Opcodes.FLOAD:
                return "FLOAD";
            case Opcodes.DLOAD:
                return "DLOAD";
            case Opcodes.ALOAD:
                return "ALOAD";
            case Opcodes.IALOAD:
                return "IALOAD";
            case Opcodes.LALOAD:
                return "LALOAD";
            case Opcodes.FALOAD:
                return "FALOAD";
            case Opcodes.DALOAD:
                return "DALOAD";
            case Opcodes.AALOAD:
                return "AALOAD";
            case Opcodes.BALOAD:
                return "BALOAD";
            case Opcodes.CALOAD:
                return "CALOAD";
            case Opcodes.SALOAD:
                return "SALOAD";
            case Opcodes.ISTORE:
                return "ISTORE";
            case Opcodes.LSTORE:
                return "LSTORE";
            case Opcodes.FSTORE:
                return "FSTORE";
            case Opcodes.DSTORE:
                return "DSTORE";
            case Opcodes.ASTORE:
                return "ASTORE";
            case Opcodes.IASTORE:
                return "IASTORE";
            case Opcodes.LASTORE:
                return "LASTORE";
            case Opcodes.FASTORE:
                return "FASTORE";
            case Opcodes.DASTORE:
                return "DASTORE";
            case Opcodes.AASTORE:
                return "AASTORE";
            case Opcodes.BASTORE:
                return "BASTORE";
            case Opcodes.CASTORE:
                return "CASTORE";
            case Opcodes.SASTORE:
                return "SASTORE";
            case Opcodes.POP:
                return "POP";
            case Opcodes.POP2:
                return "POP2";
            case Opcodes.DUP:
                return "DUP";
            case Opcodes.DUP_X1:
                return "DUP_X1";
            case Opcodes.DUP_X2:
                return "DUP_X2";
            case Opcodes.DUP2:
                return "DUP2";
            case Opcodes.DUP2_X1:
                return "DUP2_X1";
            case Opcodes.DUP2_X2:
                return "DUP2_X2";
            case Opcodes.SWAP:
                return "SWAP";
            case Opcodes.IADD:
                return "IADD";
            case Opcodes.LADD:
                return "LADD";
            case Opcodes.FADD:
                return "FADD";
            case Opcodes.DADD:
                return "DADD";
            case Opcodes.ISUB:
                return "ISUB";
            case Opcodes.LSUB:
                return "LSUB";
            case Opcodes.FSUB:
                return "FSUB";
            case Opcodes.DSUB:
                return "DSUB";
            case Opcodes.IMUL:
                return "IMUL";
            case Opcodes.LMUL:
                return "LMUL";
            case Opcodes.FMUL:
                return "FMUL";
            case Opcodes.DMUL:
                return "DMUL";
            case Opcodes.IDIV:
                return "IDIV";
            case Opcodes.LDIV:
                return "LDIV";
            case Opcodes.FDIV:
                return "FDIV";
            case Opcodes.DDIV:
                return "DDIV";
            case Opcodes.IREM:
                return "IREM";
            case Opcodes.LREM:
                return "LREM";
            case Opcodes.FREM:
                return "FREM";
            case Opcodes.DREM:
                return "DREM";
            case Opcodes.INEG:
                return "INEG";
            case Opcodes.LNEG:
                return "LNEG";
            case Opcodes.FNEG:
                return "FNEG";
            case Opcodes.DNEG:
                return "DNEG";
            case Opcodes.ISHL:
                return "ISHL";
            case Opcodes.LSHL:
                return "LSHL";
            case Opcodes.ISHR:
                return "ISHR";
            case Opcodes.LSHR:
                return "LSHR";
            case Opcodes.IUSHR:
                return "IUSHR";
            case Opcodes.LUSHR:
                return "LUSHR";
            case Opcodes.IAND:
                return "IAND";
            case Opcodes.LAND:
                return "LAND";
            case Opcodes.IOR:
                return "IOR";
            case Opcodes.LOR:
                return "LOR";
            case Opcodes.IXOR:
                return "IXOR";
            case Opcodes.LXOR:
                return "LXOR";
            case Opcodes.IINC:
                return "IINC";
            case Opcodes.I2L:
                return "I2L";
            case Opcodes.I2F:
                return "I2F";
            case Opcodes.I2D:
                return "I2D";
            case Opcodes.L2I:
                return "L2I";
            case Opcodes.L2F:
                return "L2F";
            case Opcodes.L2D:
                return "L2D";
            case Opcodes.F2I:
                return "F2I";
            case Opcodes.F2L:
                return "F2L";
            case Opcodes.F2D:
                return "F2D";
            case Opcodes.D2I:
                return "D2I";
            case Opcodes.D2L:
                return "D2L";
            case Opcodes.D2F:
                return "D2F";
            case Opcodes.I2B:
                return "I2B";
            case Opcodes.I2C:
                return "I2C";
            case Opcodes.I2S:
                return "I2S";
            case Opcodes.LCMP:
                return "LCMP";
            case Opcodes.FCMPL:
                return "FCMPL";
            case Opcodes.FCMPG:
                return "FCMPG";
            case Opcodes.DCMPL:
                return "DCMPL";
            case Opcodes.DCMPG:
                return "DCMPG";
            case Opcodes.IFEQ:
                return "IFEQ";
            case Opcodes.IFNE:
                return "IFNE";
            case Opcodes.IFLT:
                return "IFLT";
            case Opcodes.IFGE:
                return "IFGE";
            case Opcodes.IFGT:
                return "IFGT";
            case Opcodes.IFLE:
                return "IFLE";
            case Opcodes.IF_ICMPEQ:
                return "IF_ICMPEQ";
            case Opcodes.IF_ICMPNE:
                return "IF_ICMPNE";
            case Opcodes.IF_ICMPLT:
                return "IF_ICMPLT";
            case Opcodes.IF_ICMPGE:
                return "IF_ICMPGE";
            case Opcodes.IF_ICMPGT:
                return "IF_ICMPGT";
            case Opcodes.IF_ICMPLE:
                return "IF_ICMPLE";
            case Opcodes.IF_ACMPEQ:
                return "IF_ACMPEQ";
            case Opcodes.IF_ACMPNE:
                return "IF_ACMPNE";
            case Opcodes.GOTO:
                return "GOTO";
            case Opcodes.JSR:
                return "JSR";
            case Opcodes.RET:
                return "RET";
            case Opcodes.TABLESWITCH:
                return "TABLESWITCH";
            case Opcodes.LOOKUPSWITCH:
                return "LOOKUPSWITCH";
            case Opcodes.IRETURN:
                return "IRETURN";
            case Opcodes.LRETURN:
                return "LRETURN";
            case Opcodes.FRETURN:
                return "FRETURN";
            case Opcodes.DRETURN:
                return "DRETURN";
            case Opcodes.ARETURN:
                return "ARETURN";
            case Opcodes.RETURN:
                return "RETURN";
            case Opcodes.GETSTATIC:
                return "GETSTATIC";
            case Opcodes.PUTSTATIC:
                return "PUTSTATIC";
            case Opcodes.GETFIELD:
                return "GETFIELD";
            case Opcodes.PUTFIELD:
                return "PUTFIELD";
            case Opcodes.INVOKEVIRTUAL:
                return "INVOKEVIRTUAL";
            case Opcodes.INVOKESPECIAL:
                return "INVOKESPECIAL";
            case Opcodes.INVOKESTATIC:
                return "INVOKESTATIC";
            case Opcodes.INVOKEINTERFACE:
                return "INVOKEINTERFACE";
            case Opcodes.INVOKEDYNAMIC:
                return "INVOKEDYNAMIC";
            case Opcodes.NEW:
                return "NEW";
            case Opcodes.NEWARRAY:
                return "NEWARRAY";
            case Opcodes.ANEWARRAY:
                return "ANEWARRAY";
            case Opcodes.ARRAYLENGTH:
                return "ARRAYLENGTH";
            case Opcodes.ATHROW:
                return "ATHROW";
            case Opcodes.CHECKCAST:
                return "CHECKCAST";
            case Opcodes.INSTANCEOF:
                return "INSTANCEOF";
            case Opcodes.MONITORENTER:
                return "MONITORENTER";
            case Opcodes.MONITOREXIT:
                return "MONITOREXIT";
            case Opcodes.MULTIANEWARRAY:
                return "MULTIANEWARRAY";
            case Opcodes.IFNULL:
                return "IFNULL";
            case Opcodes.IFNONNULL:
                return "IFNONNULL";
            default:
                return "UNKNOWN OPCODE";
        }
    }
}