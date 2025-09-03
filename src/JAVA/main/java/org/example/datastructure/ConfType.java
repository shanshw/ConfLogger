package org.example.datastructure;

//package org.example.datastructure;

public enum ConfType {
    CONSTANT_NUMERIC,
    STRING, // path, classpath
    BOOLEAN,
    SYMBOLIC_NUMERIC, // ip, port ,duration
    UNKOWN;

    public enum SymbolicNumericType {
        IP,
        DURATION
    }

    public enum StringType {
        PATH,
        CLASSPATH
    }

    private SymbolicNumericType symbolicNumericType;
    private StringType stringType;

    public void setSymbolicNumericType(SymbolicNumericType symbolicNumericType) {
        this.symbolicNumericType = symbolicNumericType;
    }

    public SymbolicNumericType getSymbolicNumericType() {
        return symbolicNumericType;
    }

    public void setStringType(StringType stringType) {
        this.stringType = stringType;
    }

    public StringType getStringType() {
        return stringType;
    }
}
