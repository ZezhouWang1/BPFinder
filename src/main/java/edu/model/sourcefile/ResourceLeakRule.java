package edu.model.sourcefile;

import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

public class ResourceLeakRule {

    public ResourceLeakAPIStmt beforeAPI;

    public ResourceLeakAPIStmt afterAPI;

    public static boolean containsResourceLeakAPIStmt(List<String> apiCallOrderList, ResourceLeakAPIStmt resourceLeakAPIStmt) {
        if (CollectionUtils.isEmpty(apiCallOrderList)) {
            return false;
        }
        for (String api : apiCallOrderList) {
            if (resourceLeakAPIStmt.equals(api)) {
                return true;
            }
        }
        return false;
    }
}
