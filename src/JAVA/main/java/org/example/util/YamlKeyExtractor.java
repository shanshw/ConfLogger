package org.example.util;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class YamlKeyExtractor {
    // private static Map<String, Object> yamlMap;
    public static Map<String, Object> getYamlMap(String yamlFilePath) {

        Map<String, Object> yamlMap = null;

        try {
            Yaml yaml = new Yaml();

            try (FileInputStream fileInputStream = new FileInputStream(yamlFilePath)) {
                yamlMap = yaml.load(fileInputStream);

                extractKeys(yamlMap);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return yamlMap;
    }

    private static void extractKeys(Map<String, Object> map) {
        for (String key : map.keySet()) {
            Object value = map.get(key);
            if (value instanceof Map) {

                extractKeys((Map<String, Object>) value);
            } else {
                // System.out.println(key);
                ;
            }
        }
    }
}
