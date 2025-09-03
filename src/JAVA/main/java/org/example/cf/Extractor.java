package org.example.cf;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class Extractor {

    // public static void main(String[] args) {
    // Path filePath = Path.of(""); //

    // try {
    // ExtractionResult result = extractData(filePath);
    // System.out.println("Extracted Method Code:");
    // System.out.println(result.methodCode);
    // System.out.println("Method Start Line: " + result.methodStartLine);
    // System.out.println("Block Lines:");
    // result.blockLines.forEach((startLine) -> System.out.println("Start Line: " +
    // startLine));
    // } catch (IOException e) {
    // e.printStackTrace();
    // }
    // }

    public static ExtractionResult extractData(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        StringBuilder methodCode = new StringBuilder();
        List<Integer> blockLines = new ArrayList<>();
        List<String> params = new ArrayList<>();
        Integer methodStartLine = null;
        // String param = "";

        boolean inMethodSection = false;
        boolean inTaintedSinkInfo = false;

        Pattern methodLinesPattern = Pattern.compile("methodLines:\\s*(\\d+):");

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();

            if (line.equals("====================MethodSrc:=======================")) {
                inMethodSection = true;

                continue;
            }

            if (inMethodSection && line.equals("====================ctx:=======================")) {
                inMethodSection = false;
                continue;
            }

            if (inMethodSection) {
                // if (line.isEmpty()) {
                // continue;
                // }
                methodCode.append(line).append("\n");
                continue;
            }

            if (line.equals("-------------TaintedSinkInfo----------")) {
                inTaintedSinkInfo = true;
                continue;
            }

            if (inTaintedSinkInfo) {
                if (line.startsWith("paras:")) {
                    params.add(line.split(":")[1]);
                } else if (line.startsWith("blockLines:")) {

                    String[] parts = line.split(":");
                    if (parts.length >= 2) {
                        String blockLinesStr = parts[1].trim();
                        blockLines.add(Integer.parseInt(blockLinesStr));
                    }
                } else if (line.contains("methodLines:")) {

                    Matcher matcher = methodLinesPattern.matcher(line);
                    if (matcher.find() && methodStartLine == null) {
                        String methodLinesStr = matcher.group(1).trim();
                        methodStartLine = Integer.parseInt(methodLinesStr);
                    }
                }
            }
        }

        return new ExtractionResult(methodCode.toString(), methodStartLine, blockLines, params);
    }

    // Result class to hold the extracted data
    public static class ExtractionResult {
        public String methodCode;
        public Integer methodStartLine;
        public List<Integer> blockLines;
        public List<String> params;

        public ExtractionResult(String methodCode, Integer methodStartLine, List<Integer> blockLines,
                List<String> params) {
            this.methodCode = methodCode;
            this.methodStartLine = methodStartLine;
            this.blockLines = blockLines;
            this.params = params;
        }

    }
}