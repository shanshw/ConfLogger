package org.example.util;

import org.objectweb.asm.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class StaticFieldExtractor {

    public static HashSet<String> getDeclared(String jarPath, List<String> configEngineClassName) throws IOException {

        HashMap<String, Object> staticFieldsWithValues = new HashMap<>();
        HashSet<String> map = new HashSet<>();
        for (String targetClassName : configEngineClassName) {
            try (JarFile jarFile = new JarFile(jarPath)) {

                JarEntry entry = jarFile.getJarEntry(targetClassName + ".class");

                if (entry == null) {
                    System.err.println("Class " + targetClassName + " not found in JAR.");
                    System.exit(1);
                }

                try (var inputStream = jarFile.getInputStream(entry)) {
                    ClassReader classReader = new ClassReader(inputStream);
                    classReader.accept(new ClassVisitor(Opcodes.ASM9) {
                        @Override
                        public FieldVisitor visitField(int access, String name, String descriptor, String signature,
                                Object value) {
                            if ((access & Opcodes.ACC_STATIC) != 0) {
                                String type = Type.getType(descriptor).getClassName();
                                String fieldInfo = name + " " + descriptor;
                                String fieldValue = (value == null) ? "null" : value.toString();
                                System.out
                                        .println("Field: " + fieldInfo + ", Value: " + fieldValue + ", Type: " + type);
                                staticFieldsWithValues.put(fieldInfo, value);
                            }
                            return super.visitField(access, name, descriptor, signature, value);
                        }
                    }, 0);
                }
            }
        }

        for (String field : staticFieldsWithValues.keySet()) {
            Object value = staticFieldsWithValues.get(field);
            // System.out.println(field+"\t"+staticFieldsWithValues.get(field));
            if (value instanceof String)
                map.add((String) value);
            // if(value isntanceof )
        }
        return map;
    }
}
