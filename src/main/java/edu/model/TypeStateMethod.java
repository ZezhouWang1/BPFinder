package edu.model;

import org.apache.commons.collections4.CollectionUtils;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;

import java.util.ArrayList;
import java.util.List;

public class TypeStateMethod {

    public SootClass declaringClass;
    public SootMethod sootMethod;
    public List<List<Unit>> unitChain = new ArrayList<>();

    public String unitChain2Str(){
        StringBuilder stringBuilder = new StringBuilder();
        if(CollectionUtils.isEmpty(this.unitChain)){
            return stringBuilder.toString();
        }

        for(List<Unit> units : unitChain){
            for(Unit unit : units){
                if(unit != null){
                    stringBuilder.append(unit.toString() + "===");
                }
            }
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }

    @Override
    public String toString() {
        return "TypeStateMethod{" +
                "declaringClass=" + declaringClass +
                ", sootMethod=" + sootMethod +
                ", unitChain=" + unitChain2Str() +
                '}';
    }

    public SootClass getDeclaringClass() {
        return declaringClass;
    }

    public void setDeclaringClass(SootClass declaringClass) {
        this.declaringClass = declaringClass;
    }

    public SootMethod getSootMethod() {
        return sootMethod;
    }

    public void setSootMethod(SootMethod sootMethod) {
        this.sootMethod = sootMethod;
    }

}
