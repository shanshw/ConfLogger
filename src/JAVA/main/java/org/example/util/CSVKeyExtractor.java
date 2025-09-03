package org.example.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CSVKeyExtractor {

    public static Map<String, String> getKeyValuesFromCSV(String filePath) {
        Map<String, String> keyValueMap = new HashMap<>();
        String line;
        String[] headers = null;

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            if ((line = br.readLine()) != null) {
                headers = line.split(",");
            }

            if (headers != null) {
                int cnameIndex = -1;
                int cvalueIndex = -1;

                for (int i = 0; i < headers.length; i++) {
                    if (headers[i].trim().equalsIgnoreCase("cname")) {
                        cnameIndex = i;
                    } else if (headers[i].trim().equalsIgnoreCase("cvalue")) {
                        cvalueIndex = i;
                    }
                }

                if (cnameIndex != -1 && cvalueIndex != -1) {
                    while ((line = br.readLine()) != null) {
                        String[] values = line.split(",");
                        if (values.length > Math.max(cnameIndex, cvalueIndex)) {
                            String cname = values[cnameIndex].trim();
                            String cvalue = values[cvalueIndex].trim();
                            keyValueMap.put(cname, cvalue);
                        }
                    }
                } else {
                    System.err.println("CSV does not contain required columns 'cname' and 'cvalue'.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return keyValueMap;
    }

}
