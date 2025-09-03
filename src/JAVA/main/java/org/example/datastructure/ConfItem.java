package org.example.datastructure;

//package org.example.datastructure;

//import org.objectweb.asm.Type;
import org.slf4j.LoggerFactory;

import org.slf4j.Logger;

/**
 * to store the configuration information.
 */
public class ConfItem {
    private static Logger logger = LoggerFactory.getLogger(ConfItem.class);

    private static TypeUtils typeUtils = new TypeUtils();
    /**
     * the name of the property
     */
    private String cname;

    /**
     * the default value in the format of String.
     * "none" for not presenting the default value.
     */
    private String cvalue;

    /**
     * based on ASM Types, represent the configuration type.
     * Mainly in the range:
     * Boolean
     * Integer
     * Double
     * Float
     * Char
     * Object (String)
     */
    private ConfType ctype; // default Type is INT_TYPE

    /**
     * the description of the configuraiton property
     */
    private String cdesc;

    /**
     * mark the given property, default is false.
     */
    private boolean Deprecated = false;

    public ConfItem(String canme, String cvalue, ConfType ctype, String cdesc, boolean deprecated) {
        // if(canme.equals("dfs.web.ugi"))
        // System.out.println(cdesc);
        this.cname = canme.strip();
        this.cvalue = cvalue.strip();
        this.cdesc = cdesc.strip();
        setCType(ctype); // type inference,the default is INT TYPE
        checkIfDeprecated(cdesc);
    }

    private void checkIfDeprecated(String cdesc) {
        this.Deprecated = cdesc.toLowerCase().contains("deprecated");
    }

    public void setCType(ConfType t) {
        assert this.cdesc != null;
        if (this.cdesc.isEmpty() && !this.isDefaultValueSet()) {
            this.ctype = t;
            return;
        } else {
            if (this.isDefaultValueSet()) {
                this.ctype = TypeUtils.getTypeFromDefaultValue(this.cvalue);
                return;
            } else if (!this.isDefaultValueSet()) {
                this.ctype = TypeUtils.getTypeFromDesc(this.cdesc);
                return;
            }
        }
        logger.error("No description nor default value set and the given type doesn't work.");
    }

    public String getCdesc() {
        return this.cdesc;
    }

    public String getCname() {
        return this.cname;
    }

    public String getCvalue() {
        return this.cvalue;
    }

    public ConfType getCtype() {
        return this.ctype;
    }

    public boolean isDefaultValueSet() {
        return !this.cvalue.isEmpty();
    }

    public boolean isDeprecated() {
        return this.Deprecated;
    }

    @Override
    public String toString() {
        // return String.format("=====configuration
        // property======\nname:\t%s\nvalue:\t%s\ntype:\t%s\ndesc:\t%s\ndeprecated:\t%b\n",
        // getCname(), getCvalue(), getCtype(), getCdesc(), isDeprecated());
        StringBuilder builder = new StringBuilder("=====configuration property=====\n");
        builder.append("name:\t").append(getCname()).append("\n");
        builder.append("value:\t").append(getCvalue()).append("\n");
        builder.append("type:\t").append(getCtype()).append("\n");

        ConfType type = getCtype();
        if (type == ConfType.STRING) {
            builder.append("stringType:\t").append(type.getStringType()).append("\n");
        } else if (type == ConfType.SYMBOLIC_NUMERIC) {
            builder.append("symbolicNumericType:\t").append(type.getSymbolicNumericType()).append("\n");
        }

        builder.append("desc:\t").append(getCdesc()).append("\n");
        builder.append("deprecated:\t").append(isDeprecated()).append("\n");

        return builder.toString();
    }

}
