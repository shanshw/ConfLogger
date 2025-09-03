package org.example.datastructure;

import java.util.ArrayList;
import java.util.List;

public class CatchBlockInfo {

    private int tryBlockLineNumber = -1;
    private String logLevel = "";
    private String logConstantContent = "";
    private List<String> exceptionTypes = new ArrayList<>();

    public CatchBlockInfo(int tryBlockLineNumber, String logLevel, String logConstantContent,
            List<String> exceptionTypes) {
        this.tryBlockLineNumber = tryBlockLineNumber;
        this.logLevel = logLevel;
        this.logConstantContent = logConstantContent;
        this.exceptionTypes = exceptionTypes;
    }

    public int getTryBlockLineNumber() {
        return tryBlockLineNumber;
    }

    public void setTryBlockLineNumber(int tryBlockLineNumber) {
        this.tryBlockLineNumber = tryBlockLineNumber;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public String getLogConstantContent() {
        return logConstantContent;
    }

    public void setLogConstantContent(String logConstantContent) {
        this.logConstantContent = logConstantContent;
    }

    public List<String> getExceptionTypes() {
        return exceptionTypes;
    }

    public void setExceptionTypes(List<String> exceptionTypes) {
        this.exceptionTypes = exceptionTypes;
    }

    @Override
    public String toString() {
        return "CatchBlockInfo{" +
                "tryBlockLineNumber=" + tryBlockLineNumber +
                ", logLevel='" + logLevel + '\'' +
                ", logConstantContent='" + logConstantContent + '\'' +
                ", exceptionTypes=" + exceptionTypes +
                '}';
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + tryBlockLineNumber;
        result = 31 * result + (logLevel != null ? logLevel.hashCode() : 0);
        result = 31 * result + (logConstantContent != null ? logConstantContent.hashCode() : 0);

        for (String exceptionType : exceptionTypes) {
            result = 31 * result + (exceptionType != null ? exceptionType.hashCode() : 0);
        }

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        else if (obj instanceof CatchBlockInfo) {
            CatchBlockInfo anotherTs = (CatchBlockInfo) obj;
            if (this.tryBlockLineNumber == anotherTs.getTryBlockLineNumber()
                    && this.logLevel.equals(anotherTs.getLogLevel())
                    && this.logConstantContent.equals(anotherTs.getLogConstantContent())) {
                List<String> otherEt = anotherTs.getExceptionTypes();
                if (this.exceptionTypes.equals(otherEt))
                    return true;
                return false;
            }
        }
        return false;
    }
}
