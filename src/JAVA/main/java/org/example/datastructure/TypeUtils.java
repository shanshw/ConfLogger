package org.example.datastructure;

public class TypeUtils {

    public static ConfType getTypeFromDefaultValue(String defaultValue) {
        if (defaultValue == null || defaultValue.isEmpty()) {
            return ConfType.STRING;
        }

        if (defaultValue.equalsIgnoreCase("true") || defaultValue.equalsIgnoreCase("false")) {
            return ConfType.BOOLEAN;
        }

        try {
            Integer.parseInt(defaultValue);
            return ConfType.CONSTANT_NUMERIC;
        } catch (NumberFormatException e1) {
            try {
                Double.parseDouble(defaultValue);
                return ConfType.CONSTANT_NUMERIC;
            } catch (NumberFormatException e2) {
                // Not a numeric value
            }
        }

        if (defaultValue.matches("\\d{1,3}(\\.\\d{1,3}){3}:\\d{4}")) {
            ConfType confType = ConfType.SYMBOLIC_NUMERIC;
            confType.setSymbolicNumericType(ConfType.SymbolicNumericType.IP);
            return confType;
        }

        if (defaultValue.matches("\\d+(ms|s|m|h|d)")) {
            ConfType confType = ConfType.SYMBOLIC_NUMERIC;
            confType.setSymbolicNumericType(ConfType.SymbolicNumericType.DURATION);
            return confType;
        }

        if (defaultValue.matches("^(/[^/ ]*)+/?$")) {
            ConfType confType = ConfType.STRING;
            confType.setStringType(ConfType.StringType.PATH);
            return confType;
        }

        if (defaultValue.contains(".")) {
            ConfType confType = ConfType.STRING;
            confType.setStringType(ConfType.StringType.CLASSPATH);
            return confType;
        }

        return ConfType.UNKOWN;
    }

    public static ConfType getTypeFromDesc(String description) {
        if (description == null || description.isEmpty()) {
            return ConfType.UNKOWN;
        }

        String lowerDesc = description.toLowerCase();

        if (lowerDesc.contains("true") || lowerDesc.contains("false")) {
            return ConfType.BOOLEAN;
        }

        if (lowerDesc.contains("ip address") || lowerDesc.contains("ip")) {
            ConfType confType = ConfType.SYMBOLIC_NUMERIC;
            confType.setSymbolicNumericType(ConfType.SymbolicNumericType.IP);
            return confType;
        }

        if (lowerDesc.contains("duration") || lowerDesc.contains("time") || lowerDesc.contains("milliseconds")
                || lowerDesc.contains("ms") || lowerDesc.contains("timing")) {
            ConfType confType = ConfType.SYMBOLIC_NUMERIC;
            confType.setSymbolicNumericType(ConfType.SymbolicNumericType.DURATION);
            return confType;
        }

        if (lowerDesc.contains("path") || lowerDesc.contains("directory")) {
            ConfType confType = ConfType.STRING;
            confType.setStringType(ConfType.StringType.PATH);
            return confType;
        }

        if (lowerDesc.contains("classpath") || lowerDesc.contains("package") || lowerDesc.contains("class")) {
            ConfType confType = ConfType.STRING;
            confType.setStringType(ConfType.StringType.CLASSPATH);
            return confType;
        }

        try {
            Integer.parseInt(lowerDesc);
            return ConfType.CONSTANT_NUMERIC;
        } catch (NumberFormatException e1) {
            try {
                Double.parseDouble(lowerDesc);
                return ConfType.CONSTANT_NUMERIC;
            } catch (NumberFormatException e2) {
                // Not a numeric value
            }
        }

        return ConfType.UNKOWN;
    }
}
