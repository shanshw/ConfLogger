package org.example.cf;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.example.cf.Extractor.ExtractionResult;

import com.ibm.wala.util.io.FileUtil;

public class OutFileProcessor {

    public static void processOutFiles(String rootDir) throws IOException {

        File root = new File(rootDir);
        if (!root.isDirectory()) {
            throw new IllegalArgumentException("The provided path is not a directory.");
        }

        Files.walk(Paths.get(rootDir))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".out"))
                .forEach(path -> {
                    try {
                        processFile(path, rootDir);
                    } catch (IOException e) {
                        System.err.println("Failed to process file: " + path);
                        e.printStackTrace();
                    }
                });
    }

    private static void processFile(Path path, String root) throws IOException {
        System.out.println("================================");
        System.out.println("Processing file: " + path);
        Boolean ifProcessed = false;
        try {
            ExtractionResult result = Extractor.extractData(path);
            int methodStartLine = result.methodStartLine;
            List<Integer> blockLines = result.blockLines;
            String methodCode = result.methodCode;
            String user_prompt = "<code-whole>\n" + methodCode.strip() + "\n</code-whole>";
            List<String> params = result.params;
            List<String> mergedBlocks = new ArrayList<>();
            for (int blockStartLine : blockLines) {
                List<String> extractedBlock = CodeBlockExtractor.extractIfElseBlock(methodCode, methodStartLine,
                        blockStartLine);
                String singleMergedBlocks = "";
                if (extractedBlock == null || extractedBlock.isEmpty()) {

                    break;
                } else {
                    // System.out.println("Another block:" + blockStartLine);
                    // System.out.println("===========================");
                    for (String line : extractedBlock) {
                        singleMergedBlocks += line + "\n";
                    }
                    mergedBlocks.add(singleMergedBlocks);
                    ifProcessed = true;
                }
            }
            String specified_blocks = "";
            if (ifProcessed == false) {
                System.out.println("Not Processed:" + path);
            } else {
                // System.out.println(user_prompt);
                int i = 0;
                for (String singleMergedBlocks : mergedBlocks) {
                    specified_blocks += "\n<code-specified>\n" + singleMergedBlocks.strip()
                            + "\n</code-specified>";
                    String param = params.get(i);
                    i++;
                    specified_blocks += "\n<param>\n" + param.strip() + "\n</param>";
                }
                user_prompt += "\n" + specified_blocks;
                System.out.println(user_prompt);
                System.out.println("-----------------");
                String outputdir = Paths.get(root, path.getFileName().toString().split(".out")[0].strip()).toString();
                // System.out.println(outputdir);
                ConfLogger.interactWithLLM(user_prompt, outputdir);
            }
        } catch (Exception e) {
            System.out.println("Wrong in extractData for path:" + path);
            return;
        }

    }

}
