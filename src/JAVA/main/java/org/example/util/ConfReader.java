package org.example.util;

import org.example.datastructure.ConfItem;
import org.example.datastructure.ConfType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

import org.xml.sax.SAXException;
import org.yaml.snakeyaml.Yaml;

import javax.xml.parsers.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.example.datastructure.TypeUtils.getTypeFromDefaultValue;

/**
 * The class is used to manage the given configuration files.
 * The default format is .xml.
 * Supported: 1. xml to csv file 2. yaml to csv file
 * plus, there should at least three dimensions of the element:
 * 1. property_name
 * 2. property_value (can be empty but should exist the dimension)
 * 3. description (can be empty but should exist the dimension)
 */
public class ConfReader {
    private static final Logger logger = LoggerFactory.getLogger(ConfReader.class);
    private Path confPath;
    private String format;

    public ConfReader(Path confPath, String format) {
        this.confPath = confPath;
        if (format.equals("xml") || format.equals("yaml") || format.equals("md") || format.equals("csv")) {
            this.format = format;
        } else {
            logger.error("format not supported. The supported formats are: xml, yaml,MD and csv");
        }
    }

    /**
     * The method is to turn the given file into a list of ConfItems.
     */
    public Collection<ConfItem> write_and_store() throws ParserConfigurationException, IOException, SAXException {
        switch (format) {
            case "xml":
                return xml_write_and_store();
            case "yaml":
                return yaml_write_and_store();
            case "md":
                return md_write_and_store();
            case "csv":
                return csv_write_and_store();
            default:
                logger.error("format not supported. The supported formats are: xml, yaml and MD.");
                return null;
        }
    }

    /**
     *
     * @return
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    private Collection<ConfItem> xml_write_and_store() throws ParserConfigurationException, IOException, SAXException {
        Path xmlFilePath = this.confPath;
        Collection<ConfItem> confItems = new ArrayList<>();

        // Create a DocumentBuilderFactory and configure it
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        // Parse the XML file
        Document document = builder.parse(new File(String.valueOf(xmlFilePath)));

        // Normalize the XML structure
        document.getDocumentElement().normalize();

        // Get all <property> elements
        NodeList nodeList = document.getElementsByTagName("property");

        // Iterate through all <property> elements
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;

                // Get the text content of each child element
                String description = "";
                String name = element.getElementsByTagName("name").item(0).getTextContent();
                String value = "null";
                // String value =
                // element.getElementsByTagName("value").item(0).getTextContent();

                if (element.getElementsByTagName("value").getLength() > 0) {
                    value = element.getElementsByTagName("value").item(0).getTextContent();
                }
                if (element.getElementsByTagName("description").getLength() > 0) {
                    description = element.getElementsByTagName("description").item(0).getTextContent();
                }

                // Here you can add logic to determine the Type and Deprecated status
                ConfType type = ConfType.CONSTANT_NUMERIC; // Assuming all are strings for this example
                boolean deprecated = false; // Assuming false for this example

                // Create a new confItem object and add it to the collection
                ConfItem item = new ConfItem(name, value, type, description, deprecated);
                confItems.add(item);
            }
        }
        // String fileName = changeFileExtension(String.valueOf(xmlFilePath));
        // writeConfItemsToCsv(confItems,fileName);
        return confItems;
    }

    public static String changeFileExtension(String filePath) {
        String newExtension = ".csv";
        Path path = Paths.get(filePath);
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1) {
            return filePath + newExtension;
        } else {
            return filePath.substring(0, filePath.length() - (fileName.length() - dotIndex)) + newExtension;
        }
    }

    /**
     * todo
     * 
     * @return
     */
    private Collection<ConfItem> yaml_write_and_store() throws IOException {
        String filePath = String.valueOf(this.confPath);
        Yaml yaml = new Yaml();
        Collection<ConfItem> confItems = new ArrayList<>();

        try {
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            Map<String, Object> map = yaml.load(content);

            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String propertyName = entry.getKey();
                String propertyValue = entry.getValue() != null ? entry.getValue().toString() : "";

                // Create ConfItem object
                ConfItem confItem = new ConfItem(propertyName, propertyValue, ConfType.STRING, "", false);
                confItems.add(confItem);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        String fileName = changeFileExtension(filePath);
        writeConfItemsToCsv(confItems, fileName);
        return confItems;
    }

    private Collection<ConfItem> md_write_and_store() throws IOException {
        String filePath = String.valueOf(this.confPath);
        Collection<ConfItem> confItems = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        String propertyName = parts[0].trim();
                        String description = parts[1].trim();

                        // Check if description starts with "(Java system property: xxx)"
                        if (description.startsWith("(Java system property:")
                                || description.startsWith("(Java system property only:")) {
                            String startMarker;
                            if (description.startsWith("(Java system property:")) {
                                startMarker = "(Java system property:";
                            } else {
                                startMarker = "(Java system property only:";
                            }

                            int startIndex = description.indexOf(startMarker) + startMarker.length();
                            int endIndex = description.indexOf(")", startIndex);
                            if (endIndex > startIndex) {
                                propertyName = description.substring(startIndex, endIndex).trim();
                                description = description.substring(endIndex + 1).trim();
                            }
                        }
                        String defaultValue = "";
                        ConfType type = ConfType.CONSTANT_NUMERIC;
                        confItems.add(new ConfItem(propertyName, defaultValue, type, description, false));
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String fileName = changeFileExtension(filePath);
        writeConfItemsToCsv(confItems, fileName);
        return confItems;
    }

    private Collection<ConfItem> csv_write_and_store() {
        String csvPath = String.valueOf(this.confPath);
        Collection<ConfItem> confItems = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(csvPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length >= 2) {
                    String propertyName = values[0].trim();
                    String defaultValue = values[1].trim();
                    ConfType type = getTypeFromDefaultValue(defaultValue);
                    ConfItem confItem = new ConfItem(propertyName, defaultValue, type, "", false);
                    confItems.add(confItem);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return confItems;
    }

    private void writeConfItemsToCsv(Collection<ConfItem> confItems, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.append("cname,cvalue,ctype,cdesc,Deprecated\n");
            for (ConfItem item : confItems) {
                writer.append(item.getCname())
                        .append(',')
                        .append(escapeSpecialCharacters(item.getCvalue()))
                        .append(',')
                        .append(item.getCtype().name())
                        .append(',')
                        .append(escapeSpecialCharacters(item.getCdesc()))
                        .append(',')
                        .append(Boolean.toString(item.isDeprecated()))
                        .append('\n');
            }
        }
    }

    private static String escapeSpecialCharacters(String data) {
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }

}