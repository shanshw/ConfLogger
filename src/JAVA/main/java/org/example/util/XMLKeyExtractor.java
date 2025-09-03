package org.example.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.HashMap;
import java.util.Map;

public class XMLKeyExtractor {

    public static Map<String, String> extractKeyValuesFromXML(String filePath) {
        Map<String, String> keyValueMap = new HashMap<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(filePath);

            NodeList propertyNodes = document.getElementsByTagName("property");

            for (int i = 0; i < propertyNodes.getLength(); i++) {
                Node propertyNode = propertyNodes.item(i);

                if (propertyNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element propertyElement = (Element) propertyNode;
                    String value = "";
                    String name = propertyElement.getElementsByTagName("name").item(0).getTextContent();
                    if (propertyElement.getElementsByTagName("value").item(0) != null) {
                        value = propertyElement.getElementsByTagName("value").item(0).getTextContent();
                    }

                    keyValueMap.put(name, value);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return keyValueMap;
    }

}
