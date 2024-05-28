package edu.model.sourcefile;

import edu.util.Regex;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TypeStateAPIStmt {

    public String fullPathClassName;

    public String returnType;

    public String methodName;

    public List<String> paramNames = new ArrayList<>();

    public TypeStateAPIStmt(String s){
        this.fullPathClassName = Regex.getSubUtilSimple(s, "(<.*?:)").replace("<","").replace(":", "");
        this.returnType = Regex.getSubUtilSimple(s, "( .*? )").replace(" ","");
        this.methodName = Regex.getSubUtilSimple(s, "([a-zA-Z<>]+\\()").replace("(","");
        String paramStr = Regex.getSubUtilSimple(s, "(\\(.*?\\))").replace("(", "").replace(")", "");
        if(!paramStr.contains(",")){
            this.paramNames.add(paramStr);
        }else{
            this.paramNames.addAll(Arrays.asList(paramStr.split(",")));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if(o.getClass() != String.class){
            return false;
        }
        String that = (String) o;
        String thatClassName = Regex.getSubUtilSimple(that, "(<.*?:)").replace("<","").replace(":", "");
        String thatMethodName= Regex.getSubUtilSimple(that, "([a-zA-Z<>]+\\()").replace("(","");

        return StringUtils.equals(this.fullPathClassName, thatClassName) && StringUtils.equals(this.methodName, thatMethodName);
    }

    public String getAPIString() {
        return "<" + fullPathClassName +
                " " + returnType + ":" +
                " " + methodName +
                "(...)>";
    }
}
