package edu.model;

import org.apache.commons.collections4.CollectionUtils;
import soot.SootMethod;

import java.util.ArrayList;
import java.util.List;

public class TypeStateICC {
    public IC3 ic3;
    public List<List<SootMethod>> methodChain = new ArrayList<>();

    public String unitChain2Str(){
        StringBuilder stringBuilder = new StringBuilder();
        if(CollectionUtils.isEmpty(this.methodChain)){
            return stringBuilder.toString();
        }

        for(List<SootMethod> sootMethods : methodChain){
            for(SootMethod sootMethod : sootMethods){
                if(sootMethod != null){
                    stringBuilder.append(sootMethod.getSignature().toString() + "===");
                }
            }
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }

    @Override
    public String toString() {
        return "TypeStateICC{" +
                "ic3=" + ic3 +
                ", methodChain=" + unitChain2Str() +
                '}';
    }
}
