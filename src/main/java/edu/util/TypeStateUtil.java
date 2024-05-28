package edu.util;

import edu.model.TypeStateCallback;
import edu.model.TypeStateComponent;
import edu.model.TypeStateMethod;
import edu.model.sourcefile.TypeStateAPIStmt;
import edu.model.sourcefile.TypeStateRule;
import edu.src.GlobalRef;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import soot.*;
import soot.dexpler.DalvikThrowAnalysis;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.ExceptionalUnitGraphFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TypeStateUtil {

    public static void typeStateAnalysis() {
        /**
         * step1. construct Block Graph for each method that contains Type State Sensitive APIs
         */
        //List<String> typeStateSensitiveAPIs = FileUtil.getAPIsFromFile("/Users/xsun0035/Desktop/TypeStateRules.txt");
        GlobalRef.typeStateRules = FileUtil.getRulesFromSourceFile(GlobalRef.typeStateRulesPath);
        GlobalRef.resourceLeakRules = FileUtil.getResourceLeakRulesFromSourceFile(GlobalRef.resourceLeakRulesPath);
        GlobalRef.allAPIList = FileUtil.getAllAPIListFromSourceFile(GlobalRef.allAPIListPath);
        //GlobalRef.resourceLeakRules = FileUtil.getRulesFromSourceFile(GlobalRef.typeStateRulesPath);

        JimpleBasedInterproceduralCFG baseICFG = new JimpleBasedInterproceduralCFG(true, true) {
            protected DirectedGraph<Unit> makeGraph(Body body) {
                return enableExceptions ? ExceptionalUnitGraphFactory.createExceptionalUnitGraph(body, DalvikThrowAnalysis.interproc(), true)
                        : new BriefUnitGraph(body);
            }
        };

        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            if (ApplicationClassFilter.isClassInSystemPackage(sootClass.getName()) || CollectionUtils.isEmpty(sootClass.getMethods())) {
                continue;
            }

            for(int k = 0; k < sootClass.getMethods().size(); k++){
                SootMethod targetMethod = sootClass.getMethods().get(k);
                if (targetMethod.getSignature().contains("dummyMainClass") || !targetMethod.isConcrete()) {
                    continue;
                }

                DirectedGraph<Unit> ug = baseICFG.getOrCreateUnitGraph(targetMethod.retrieveActiveBody());
                Iterator<Unit> uit = ug.iterator();
                List<Unit> units = new ArrayList<>();
                uit.forEachRemaining(units::add);

                /**
                 * =============[Construct stmtList Start]=============
                 * stmtList indicates the stmt list inside a method from root stmt to leaf stmt forwardly.
                 */
                Unit[] stmtList = new Unit[1000];
                boolean containsTypeStateStmt = TypeStateUtil.containsTypeStateStmtFromAllAPI(targetMethod, GlobalRef.allAPIList);
                if (containsTypeStateStmt) {
                    TypeStateMethod typeStateMethod = new TypeStateMethod();
                    typeStateMethod.setSootMethod(targetMethod);
                    typeStateMethod.setDeclaringClass(sootClass);
                    GlobalRef.typeStateMethodList.add(typeStateMethod);

                    CFGUtil.getPathsFromRoot2Leaf(ug.getHeads().get(0), stmtList, 0, ug, targetMethod, new ArrayList<>());
                }
            }
        }

        /**
         * =============[Construct methodPathList(For each component) Start]=============
         * pathList indicates the method list from entry point to target method.
         */
        SootMethod[] methodPathList = new SootMethod[1000];
        SootClass dummyMainClass = Scene.v().getSootClassUnsafe("dummyMainClass");
        for(SootMethod entryPointMethod : dummyMainClass.getMethods()){
            TypeStateComponent typeStateComponent = new TypeStateComponent();
            typeStateComponent.entryPointClass = dummyMainClass;
            typeStateComponent.entryPointMethod = entryPointMethod;
            GlobalRef.typeStateComponentList.add(typeStateComponent);

            CFGUtil.getMethodPathsFromRoot2Leaf(entryPointMethod, methodPathList, 0, entryPointMethod, new ArrayList<>());
        }

        /**
         * =============[Construct methodPathList(For callback methods) Start]=============
         * pathList indicates the method list for callback methods[only consider component callbacks here]
         */
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            if (ApplicationClassFilter.isClassInSystemPackage(sootClass.getName()) || CollectionUtils.isEmpty(sootClass.getMethods()) || !ApplicationClassFilter.is4ComponentClass(sootClass)) {
                continue;
            }
            TypeStateCallback typeStateCallback = new TypeStateCallback();
            typeStateCallback.declaringClass = sootClass;
            GlobalRef.typeStateCallbackList.add(typeStateCallback);

            extractCallbackMethods4Rules(sootClass, typeStateCallback, ApplicationClassFilter.callbackRule1());
            extractCallbackMethods4Rules(sootClass, typeStateCallback, ApplicationClassFilter.callbackRule2());
            extractCallbackMethods4Rules(sootClass, typeStateCallback, ApplicationClassFilter.callbackRule3());
            extractCallbackMethods4Rules(sootClass, typeStateCallback, ApplicationClassFilter.callbackRule4());
            extractCallbackMethods4Rules(sootClass, typeStateCallback, ApplicationClassFilter.callbackRule5());
            extractCallbackMethods4Rules(sootClass, typeStateCallback, ApplicationClassFilter.callbackRule6());
            extractCallbackMethods4Rules(sootClass, typeStateCallback, ApplicationClassFilter.callbackRule7());
        }

    }

    private static void extractCallbackMethods4Rules(SootClass sootClass, TypeStateCallback typeStateCallback, List<String> callbackRules) {
        //for callbackRule1
        List<SootMethod> callbackList = new ArrayList<>();
        for(String callbackMethodName : callbackRules){
            for(SootMethod sm : sootClass.getMethods()){
                if(sm.getName().equals(callbackMethodName)){
                    callbackList.add(sm);
                }
            }
        }
        if(CollectionUtils.isNotEmpty(callbackList)){
            typeStateCallback.methodChain.add(callbackList);
        }
    }

    public static boolean containsTypeStateStmt(SootMethod sootMethod, List<TypeStateRule> typeStateRules) {
        if (sootMethod == null || !sootMethod.hasActiveBody() || CollectionUtils.isEmpty(sootMethod.getActiveBody().getUnits())) {
            return false;
        }
        for (Unit unit : sootMethod.getActiveBody().getUnits()) {
            String currentStmt = Regex.getSubUtilSimple(unit.toString(), "(<.*>)").replace("$", ".");
            if(StringUtils.isBlank(currentStmt)){
                continue;
            }
            for(TypeStateRule typeStateRule : typeStateRules){
                if(typeStateRule.afterAPI != null && typeStateRule.afterAPI.equals(currentStmt)){
                    return true;
                }
                if(CollectionUtils.isNotEmpty(typeStateRule.beforeAPIs)){
                    for(TypeStateAPIStmt beforeStmt : typeStateRule.beforeAPIs){
                        if(beforeStmt != null && beforeStmt.equals(currentStmt)){
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean containsTypeStateStmtFromAllAPI(SootMethod sootMethod, List<String> allAPIList) {
        if (sootMethod == null || !sootMethod.hasActiveBody() || CollectionUtils.isEmpty(sootMethod.getActiveBody().getUnits())) {
            return false;
        }
        for (Unit unit : sootMethod.getActiveBody().getUnits()) {
            String currentStmt = Regex.getSubUtilSimple(unit.toString(), "(<.*>)").replace("$", ".");
            if(StringUtils.isBlank(currentStmt)){
                continue;
            }
            for(String typestateAPI : allAPIList){
                String thisClassName = Regex.getSubUtilSimple(currentStmt, "(<.*?:)").replace("<","").replace(":", "");
                String thisMethodName = Regex.getSubUtilSimple(currentStmt, "([a-zA-Z<>]+\\()").replace("(","");

                String thatClassName = Regex.getSubUtilSimple(typestateAPI, "(<.*?:)").replace("<","").replace(":", "");
                String thatMethodName= Regex.getSubUtilSimple(typestateAPI, "([a-zA-Z<>]+\\()").replace("(","");

                if(StringUtils.equals(thisClassName, thatClassName) && StringUtils.equals(thisMethodName, thatMethodName)){
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * check if the given unit lies in AllAPI.txt
     * [Considering class Hierarchy here]
     * if yes, return the currentStmt
     * @param unit
     * @param allAPIList
     * @return
     */
    public static String containsTypeStateStmt(Unit unit, List<String> allAPIList) {
        if (unit == null || StringUtils.isBlank(Regex.getSubUtilSimple(unit.toString(), "(<.*>)"))) {
            return "";
        }

        String currentStmt = "";
        if(ApplicationClassFilter.hasAndroidSuperClass(unit)){
            currentStmt = ApplicationClassFilter.transform2Superclass(unit);
            System.out.println("====SuperClass===origin==="+unit.toString());
            System.out.println("====SuperClass===after==="+currentStmt.toString());
        }else {
            currentStmt = Regex.getSubUtilSimple(unit.toString(), "(<.*>)").replace("$", ".");
        }

        if(StringUtils.isBlank(currentStmt)){
            return "";
        }

        for(String typestateAPI : allAPIList){
            String thisClassName = Regex.getSubUtilSimple(currentStmt, "(<.*?:)").replace("<","").replace(":", "");
            String thisMethodName = Regex.getSubUtilSimple(currentStmt, "([a-zA-Z<>]+\\()").replace("(","");

            String thatClassName = Regex.getSubUtilSimple(typestateAPI, "(<.*?:)").replace("<","").replace(":", "");
            String thatMethodName= Regex.getSubUtilSimple(typestateAPI, "([a-zA-Z<>]+\\()").replace("(","");

            if(StringUtils.equals(thisClassName, thatClassName) && StringUtils.equals(thisMethodName, thatMethodName)){
                return currentStmt;
            }
        }
//        for(TypeStateRule typeStateRule : typeStateRules){
//            if(typeStateRule.afterAPI != null && typeStateRule.afterAPI.equals(currentStmt)){
//                return currentStmt;
//            }
//            if(CollectionUtils.isNotEmpty(typeStateRule.beforeAPIs)){
//                for(TypeStateAPIStmt beforeStmt : typeStateRule.beforeAPIs){
//                    if(beforeStmt != null && beforeStmt.equals(currentStmt)){
//                        return currentStmt;
//                    }
//                }
//            }
//        }
        return "";
    }


    public static TypeStateMethod getFromList(List<TypeStateMethod> typeStateMethodList, SootMethod sootMethod) {
        for (TypeStateMethod typeStateMethod : typeStateMethodList) {
            if (typeStateMethod.getSootMethod().getSignature().equals(sootMethod.getSignature())) {
                return typeStateMethod;
            }
        }
        return null;
    }

    public static TypeStateComponent getElementFromList(List<TypeStateComponent> typeStateComponentList, SootMethod sootMethod) {
        for (TypeStateComponent typeStateComponent : typeStateComponentList) {
            if (typeStateComponent.entryPointMethod.getSignature().equals(sootMethod.getSignature())) {
                return typeStateComponent;
            }
        }
        return null;
    }
}
