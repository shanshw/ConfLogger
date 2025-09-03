package org.example.cf;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.*;

import org.example.cf.Extractor.ExtractionResult;

public class CodeBlockExtractor {

    public static void main(String[] args) throws IOException {
        Path methodCodeFilePath = Path
                .of("");
        ExtractionResult result = Extractor.extractData(methodCodeFilePath);
        int methodStartLine = result.methodStartLine;
        List<Integer> blockLines = result.blockLines;
        String methodCode = result.methodCode;
        for (int blockStartLine : blockLines) {
            List<String> extractedBlock = extractIfElseBlock(methodCode, methodStartLine, blockStartLine);
            extractedBlock.forEach(System.out::println);
            if (extractedBlock.isEmpty())
                System.out.println("Empty");
            // System.out.println("----------XXXXXXXXXXXX-------------");
        }

    }

    public static List<String> extractIfElseBlock(String methodCode, int methodStartLine, int blockStartLine) {
        List<String> codeLines = new ArrayList<>(Arrays.asList(methodCode.split("\n")));
        // System.out.println(codeLines.size());
        List<String> extractedBlock = new ArrayList<>();
        boolean inBlock = false;
        int openBraces = 0;

        int relativeBlockStart = blockStartLine - methodStartLine - 1;

        for (int offset = 0; offset <= 5; offset++) {
            int adjustedBlockStart = relativeBlockStart + offset;
            if (adjustedBlockStart >= 0 && adjustedBlockStart < codeLines.size()) {
                String lineContent = codeLines.get(adjustedBlockStart).trim();

                if (lineContent.startsWith("if") || lineContent.startsWith("else")) {
                    inBlock = true;
                    openBraces = 0;
                    extractedBlock.clear();
                    for (int i = adjustedBlockStart; i < codeLines.size(); i++) {
                        String line = codeLines.get(i).trim();
                        extractedBlock.add(line);
                        for (char c : line.toCharArray()) {
                            if (c == '{')
                                openBraces++;
                            if (c == '}')
                                openBraces--;
                        }
                        if (openBraces == 0) {
                            return extractedBlock;
                        }
                    }
                }
            }
        }

        int ifLine = -1;
        for (int i = relativeBlockStart - 1; i >= 0; i--) {
            String line = codeLines.get(i).trim();
            if (line.startsWith("else")) {
                for (int j = i - 1; j >= 0; j--) {
                    String lineBefore = codeLines.get(j).trim();
                    if (lineBefore.startsWith("if")) {
                        ifLine = j;
                        break;
                    }
                }
                break;
            }
        }

        if (ifLine >= 0) {
            openBraces = 0;
            inBlock = true;
            extractedBlock.clear();
            for (int i = ifLine; i < codeLines.size(); i++) {
                extractedBlock.add(codeLines.get(i).trim());
                for (char c : codeLines.get(i).toCharArray()) {
                    if (c == '{')
                        openBraces++;
                    if (c == '}')
                        openBraces--;
                }
                if (openBraces == 0) {
                    return extractedBlock;
                }
            }
        }

        return extractedBlock;
    }
}
