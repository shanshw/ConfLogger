package org.example.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import org.example.datastructure.CtxCodeBlock;
import org.example.datastructure.ExtendedBlock;
import org.example.datastructure.TaintedSinkInfo;

import java.util.*;

public class FileUtil {

    private static final String FILE_PREFIX = "Method-Wise-CCH_";
    private static final String FILE_EXTENSION = ".out";
    private static final ReentrantLock lock = new ReentrantLock();

    public static String readFileContent(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }

    public static List<String> readFileLines(String filePath) throws IOException {
        return Files.readAllLines(Paths.get(filePath));
    }

    public static <T> void writeToFile(TaintedSinkInfo ts) {
        lock.lock();
        try {
            String fileName = generateFileName();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
                writer.write(ts.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            lock.unlock();
        }
    }

    public static <T> void writeToFile(List<T> content) {
        lock.lock();
        try {
            String fileName = generateFileName();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
                for (T item : content) {
                    writer.write(item.toString());
                    writer.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            lock.unlock();
        }
    }

    public static void writeToFile(HashSet<TaintedSinkInfo> sks, List<ExtendedBlock> extendedBlocks,
            ArrayList<String> methodSrc,
            HashSet<CtxCodeBlock> ctxes) {
        lock.lock();
        try {
            String fileName = generateFileName();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
                writer.write("====================TaintedSinkInfo:=======================\n");
                for (TaintedSinkInfo sk : sks) {
                    writer.write(sk.toString());
                    writer.newLine();
                }
                writer.write("\n\n====================ExtendedBlocks:=======================\n");
                for (ExtendedBlock item : extendedBlocks) {
                    writer.write(item.toString());
                    writer.newLine();
                }
                writer.write("\n\n====================MethodSrc:=======================\n");
                for (String item : methodSrc) {
                    writer.write(item.toString());
                    writer.newLine();
                }
                writer.write("\n\n====================ctx:=======================\n");
                for (CtxCodeBlock item : ctxes) {
                    writer.write(item.toString());
                    writer.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            lock.unlock();
        }
    }

    public static <T> void writeToFile(List<T> content, ArrayList<String> srcCotent, ArrayList<String> confCheckBlocks,
            TaintedSinkInfo ts) {
        lock.lock();
        try {
            String fileName = generateFileName();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
                writer.write(("Tainted Path: \n"));
                for (T item : content) {
                    writer.write(item.toString());
                    writer.newLine();
                }
                writer.write("\nconfCheckBlocks: \n");
                for (String s : confCheckBlocks) {
                    writer.write(s);
                    writer.newLine();
                }
                writer.write("\n srcClassFileContent:\n");
                for (String src : srcCotent) {
                    writer.write(src);
                    writer.newLine();
                }

                writer.write("\nts:\n");
                writer.write(ts.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            lock.unlock();
        }
    }

    public static <T> void writeToFile(List<T> content, ArrayList<String> confCheckBlocks, TaintedSinkInfo ts) {
        lock.lock();
        try {
            String fileName = generateFileName();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
                for (T item : content) {
                    writer.write(item.toString());
                    writer.newLine();
                }
                writer.write("\n");
                for (String s : confCheckBlocks) {
                    writer.write(s);
                    writer.newLine();
                }
                writer.write("\n");
                writer.write(ts.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            lock.unlock();
        }
    }

    private static String generateFileName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS");
        String timestamp = sdf.format(new Date());
        int randomNum = new Random().nextInt(1000000);
        String directoryPath = "paResults";

        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileName = FILE_PREFIX + timestamp + "_" + randomNum + FILE_EXTENSION;
        return directoryPath + "/" + fileName;
    }

}
