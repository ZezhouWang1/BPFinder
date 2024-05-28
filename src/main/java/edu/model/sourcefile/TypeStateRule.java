package edu.model.sourcefile;

import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class TypeStateRule {

    //indicates if this rule needs to be followed or against
    public boolean isCorrect = true;

    public List<TypeStateAPIStmt> beforeAPIs = new ArrayList<>();

    public TypeStateAPIStmt afterAPI;

    public String minSdkVersion;

    public String maxSdkVersion;

    public static boolean containsTypeStateAPIStmt(List<String> apiCallOrderList, List<TypeStateAPIStmt> typeStateAPIStmtList){
        if(CollectionUtils.isEmpty(apiCallOrderList)){
            return false;
        }
        if(CollectionUtils.isEmpty(typeStateAPIStmtList)){
            return false;
        }
        for(String apiStr : apiCallOrderList){
            for(TypeStateAPIStmt typeStateAPIStmt : typeStateAPIStmtList){
                if(typeStateAPIStmt.equals(apiStr)){
                    return true;
                }
            }
        }
        return false;
    }
}
